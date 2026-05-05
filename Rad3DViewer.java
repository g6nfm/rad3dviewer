import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;


public class Rad3DViewer extends JPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private ContO carModel;
    private ContO wheelModel; // used when this viewer is for a wheel
    private Medium medium;
    // This angle (in degrees) controls the rotation of the model around its Y-axis.
    private double modelAngle = 0;
    private double wheelAngle = 0;
    // For mouse dragging:
    private int lastMouseX, lastMouseY;
    // Color scheme: 0 = original (c()), 1 = skin1 (c1()), 2 = skin2 (c2())
    private int colorScheme = 0;
    // The drawing panel that does the custom painting.
    private DrawingPanel drawingPanel;

    private int mouseX = -1;
    private int mouseY = -1;
    private int hoveredPolyIndex = -1;

    private Set<Integer> selectedPolygons = new HashSet<>();
    private SelectionToolbar selectionToolbar;

    private int cameraDistance = 0; // Stores zoom offset


    private boolean isWheelViewer = false;

    public void setWheelViewer(boolean b) {
        isWheelViewer = b;
    }

    public Rad3DViewer() {
    setLayout(new BorderLayout());
    //setPreferredSize(new Dimension(1024, 768));

    medium = new Medium();

    // Create layered pane for overlay
    final JLayeredPane layeredPane = new JLayeredPane();
    layeredPane.setPreferredSize(new Dimension(1024, 768));

    drawingPanel = new DrawingPanel();
    drawingPanel.setOpaque(false);

    selectionToolbar = new SelectionToolbar();

    // Add component listener to THIS panel (Rad3DViewer) to resize children
    addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            int w = getWidth();
            int h = getHeight();
            
            // Resize layered pane to match parent
            layeredPane.setBounds(0, 0, w, h);
            
            // Resize drawing panel to fill entire space
            drawingPanel.setBounds(0, 0, w, h);
            
            // Center toolbar
            int toolbarWidth = 950;
            int centerX = (w - toolbarWidth) / 2;
            selectionToolbar.setBounds(centerX, 0, toolbarWidth, 50);
            
            // Trigger repaint to recenter car
            repaint();
        }
    });

    // Set initial bounds
    drawingPanel.setBounds(0, 0, 1024, 768);
    int toolbarWidth = 950;
    int centerX = (1024 - toolbarWidth) / 2;
    selectionToolbar.setBounds(centerX, 0, toolbarWidth, 50);

    // Add to layered pane
    layeredPane.add(drawingPanel, JLayeredPane.DEFAULT_LAYER);
    layeredPane.add(selectionToolbar, JLayeredPane.PALETTE_LAYER);

    add(layeredPane, BorderLayout.CENTER);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    /**
     * Loads the given .rad file, builds the ContO carmodel using our Medium/Trackers,
     * and then positions the carCarModel.
     */
    public void loadRadFile(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            carModel = new ContO(fileData, medium);

            carModel.x = Medium.cx;
            carModel.y = 250 - carModel.grat;
            carModel.z = 650;
            carModel.zy = 0;
            carModel.xz = 0;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        repaint();
    }

    /**
     * Loads a wheel .rad file as its own independent ContO model.
     * This does NOT touch the main car model, so it can be positioned separately.
     */
    public void loadWheelFile(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));

            // Build a NEW ContO specifically for wheels
            wheelModel = new ContO(fileData, medium);

            // Use ORIGINAL working position values
            wheelModel.x = 110;
            wheelModel.y = 120;
            wheelModel.z = 400; 
            wheelModel.zy = 0;
            wheelModel.xz = 0;

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        repaint();
    }

        private class DrawingPanel extends JPanel {

        public DrawingPanel() {
            //setPreferredSize(new Dimension(1024, 768));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (carModel == null && wheelModel == null) {
                g.drawString("No model loaded.", getWidth() / 2 - 50, getHeight() / 2);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pw = getWidth();
            int ph = getHeight();

            Medium.setViewport(0, 0, pw, ph);

            // Reset core Medium stuff every frame, so viewers are independent
            Medium.w  = pw;
            Medium.h  = ph;
            Medium.cx = pw / 2;
            Medium.cy = ph / 2;  // ADD THIS LINE
            Medium.cz = ph / 8;  // Main viewer with zoom
            // Different camera for wheel vs car viewer
            

            // --------------------------
            // ENVIRONMENT PER VIEWER
            // --------------------------
            if (isWheelViewer) {
                // Plain background for wheel panel
                //g2d.setColor(Color.WHITE);
                //g2d.fillRect(0, 0, pw, ph);

                Medium.y = 0;
                Medium.ground = 250;
                Medium.crs = false;
                Medium.fogd = 3;
                Medium.fadeFrom(3000);

                Medium.d(g2d);

            } else {
                // Car viewer environment
                Medium.y = -300;
                Medium.ground = 650;
                Medium.crs = true;
                Medium.fogd = 8;
                Medium.fadeFrom(2000);

                Medium.d(g2d); // draw sky/ground only in car viewer
            }

            // ============ CAR MODEL ============
            if (carModel != null && !isWheelViewer) {
                carModel.x = Medium.cx;

                // Find which polygon is hovered
                hoveredPolyIndex = findHoveredPoly(carModel);

                int nPlanes = carModel.npl;

                int[][] backupOx = new int[nPlanes][];
                int[][] backupOz = new int[nPlanes][];

                for (int i = 0; i < nPlanes; i++) {
                    Plane p = carModel.p[i];
                    backupOx[i] = p.ox.clone();
                    backupOz[i] = p.oz.clone();
                }

                carModel.applySkin(colorScheme);

                int totalX = 0, totalZ = 0, vcount = 0;
                for (int i = 0; i < nPlanes; i++) {
                    Plane p = carModel.p[i];
                    for (int v = 0; v < p.n; v++) {
                        totalX += p.ox[v];
                        totalZ += p.oz[v];
                        vcount++;
                    }
                }

                int centerX = (vcount > 0) ? totalX / vcount : 0;
                int centerZ = (vcount > 0) ? totalZ / vcount : 0;

                float ang = (float) Math.toRadians(modelAngle);
                float cosA = (float) Math.cos(ang);
                float sinA = (float) Math.sin(ang);

                for (int i = 0; i < nPlanes; i++) {
                    Plane p = carModel.p[i];
                    for (int v = 0; v < p.n; v++) {
                        int ox = p.ox[v];
                        int oz = p.oz[v];

                        int rx = ox - centerX;
                        int rz = oz - centerZ;

                        p.ox[v] = centerX + (int) (rx * cosA - rz * sinA);
                        p.oz[v] = centerZ + (int) (rx * sinA + rz * cosA);
                    }
                }

                carModel.d(g2d, hoveredPolyIndex, selectedPolygons);

                // After calling carModel.d(g2d, hoveredPolyIndex);
                // Highlight selected polygons
                for (int selectedPoly : selectedPolygons) {
                    if (selectedPoly >= 0 && selectedPoly < carModel.npl) {
                        // Draw selection indicator (you can customize this)
                    }
                }

                for (int i = 0; i < nPlanes; i++) {
                    Plane p = carModel.p[i];
                    System.arraycopy(backupOx[i], 0, p.ox, 0, p.ox.length);
                    System.arraycopy(backupOz[i], 0, p.oz, 0, p.oz.length);
                }
            }

            // ============ WHEEL MODEL ============
            if (wheelModel != null) {
                //wheelModel.x = Medium.cx;  // Re-center every frame
                int wn = wheelModel.npl;

                int[][] wBackupOx = new int[wn][];
                int[][] wBackupOz = new int[wn][];

                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    wBackupOx[i] = p.ox.clone();
                    wBackupOz[i] = p.oz.clone();
                }

                // Rotate wheel around origin (0,0,0) for proper centering
                float wAng = (float) Math.toRadians(wheelAngle);
                float wCos = (float) Math.cos(wAng);
                float wSin = (float) Math.sin(wAng);

                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    for (int v = 0; v < p.n; v++) {
                        int ox = p.ox[v];
                        int oz = p.oz[v];

                        // Rotate around origin (0,0,0)
                        p.ox[v] = (int) (ox * wCos - oz * wSin);
                        p.oz[v] = (int) (ox * wSin + oz * wCos);
                    }
                }

                wheelModel.d(g2d, -1, new HashSet<>());

                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    System.arraycopy(wBackupOx[i], 0, p.ox, 0, p.ox.length);
                    System.arraycopy(wBackupOz[i], 0, p.oz, 0, p.oz.length);
                }
            }
        }
    }

    public ContO getCarModel() {
        return carModel;
        }

    public ContO getWheelModel() {
            return wheelModel;
        }


    // Helper: whichever model is present (wheel first, else car)
   private ContO getActiveModel() {
        // in wheel viewer, always prefer wheel
        if (isWheelViewer && wheelModel != null) return wheelModel;
        if (!isWheelViewer && carModel != null) return carModel;
        // fallback
        if (wheelModel != null) return wheelModel;
        return carModel;
    }

    private int findHoveredPoly(ContO model) {
        if (model == null || mouseX == -1 || mouseY == -1) return -1;
        
        // Get the rotation angle for this model
        double angle = (model == carModel) ? modelAngle : wheelAngle;
        
        // Calculate center of model (same as in paintComponent)
        int totalX = 0, totalZ = 0, vcount = 0;
        for (int i = 0; i < model.npl; i++) {
            Plane p = model.p[i];
            for (int v = 0; v < p.n; v++) {
                totalX += p.ox[v];
                totalZ += p.oz[v];
                vcount++;
            }
        }
        int centerX = (vcount > 0) ? totalX / vcount : 0;
        int centerZ = (vcount > 0) ? totalZ / vcount : 0;
        
        float ang = (float) Math.toRadians(angle);
        float cosA = (float) Math.cos(ang);
        float sinA = (float) Math.sin(ang);
        
        int closestPoly = -1;
        int closestDepth = Integer.MAX_VALUE;
        
        // Check each polygon
        for (int i = 0; i < model.npl; i++) {
            Plane p = model.p[i];
            if (p == null) continue;
            
            int[] screenX = new int[p.n];
            int[] screenY = new int[p.n];
            int avgDepth = 0;
            
            for (int v = 0; v < p.n; v++) {
                // Apply model rotation around center
                int ox = p.ox[v];
                int oz = p.oz[v];
                
                int rx = ox - centerX;
                int rz = oz - centerZ;
                
                int rotX = centerX + (int)(rx * cosA - rz * sinA);
                int rotZ = centerZ + (int)(rx * sinA + rz * cosA);
                
                // Now transform to world space
                int wx = model.x + rotX - Medium.x;
                int wy = model.y + p.oy[v] - Medium.y;
                int wz = model.z + rotZ - Medium.z;
                
                // Apply camera rotation
                int camX = Medium.cx + (int)((wx - Medium.cx) * RadicalMath.cos(Medium.xz) 
                        - (wz - Medium.cz) * RadicalMath.sin(Medium.xz));
                int camZ = Medium.cz + (int)((wx - Medium.cx) * RadicalMath.sin(Medium.xz) 
                        + (wz - Medium.cz) * RadicalMath.cos(Medium.xz));
                
                // Accumulate depth for this polygon
                avgDepth += camZ;
                
                // Project to screen
                screenX[v] = Utility.xs(camX, camZ);
                screenY[v] = Utility.ys(wy, camZ, 0);
            }
            
            // Calculate average depth
            avgDepth /= p.n;
            
            // Check if mouse is inside this polygon AND it's closer than previous matches
            if (isPointInPolygon(mouseX, mouseY, screenX, screenY, p.n)) {
                if (avgDepth < closestDepth) {
                    closestDepth = avgDepth;
                    closestPoly = i;
                }
            }
        }
        
        return closestPoly;
    }

    private boolean isPointInPolygon(int px, int py, int[] polyX, int[] polyY, int n) {
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((polyY[i] > py) != (polyY[j] > py) &&
                (px < (polyX[j] - polyX[i]) * (py - polyY[i]) / (polyY[j] - polyY[i]) + polyX[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

   @Override
    public void mousePressed(MouseEvent e) {
        // Update mouse position first
        mouseX = e.getX();
        mouseY = e.getY();
        
        requestFocusInWindow();
        
        // Left click - paint mode takes priority
        if (SwingUtilities.isLeftMouseButton(e) && hoveredPolyIndex != -1) {
            
            // Check if in paint mode FIRST
            if (isInPaintMode()) {
                paintPolygon(hoveredPolyIndex);
                repaint();
                return;  // Don't process selection or toolbar
            }
            
            // Normal selection mode
            if (e.isControlDown()) {
                // Ctrl + Click = Add to selection
                if (selectedPolygons.contains(hoveredPolyIndex)) {
                    selectedPolygons.remove(hoveredPolyIndex);
                } else {
                    selectedPolygons.add(hoveredPolyIndex);
                }
            } else {
                // Normal click = Replace selection
                selectedPolygons.clear();
                selectedPolygons.add(hoveredPolyIndex);
            }
            
            // Update toolbar
            ContO model = getActiveModel();
            if (model != null) {
                selectionToolbar.setSelection(selectedPolygons, model.npl);
            }
            
            repaint();
            return;
        }
        
        // Otherwise - normal drag behavior
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    @Override 
    public void mouseClicked(MouseEvent e) {
        // Also handle left-click here as a backup
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseX = e.getX();
            mouseY = e.getY();
            
            // Recalculate hovered poly
            ContO model = getActiveModel();
            if (model != null) {
                hoveredPolyIndex = findHoveredPoly(model);
                if (hoveredPolyIndex != -1) {
                    //showPolygonContextMenu(e.getX(), e.getY());
                }
            }
        }
    }

    // --- MouseMotionListener ---
    @Override
    public void mouseDragged(MouseEvent e) {
        // Update mouse position during drag too
        mouseX = e.getX();
        mouseY = e.getY();
        
        // Only rotate when NOT clicking on a polygon
        if (hoveredPolyIndex == -1) {
            ContO m = getActiveModel();
            if (m == null) return;

            int dx = e.getX() - lastMouseX;

            // Rotate correct angle
            if (m == carModel) {
                modelAngle += dx;
            } else {
                wheelAngle += dx;
            }

            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
        
        repaint();
    }

    @Override 
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        ContO m = getActiveModel();
        if (m == null) {
            return;
        }
        int notches = e.getWheelRotation();
        m.z += notches * 20;  // Store zoom offset
        repaint();
    }

    // --- KeyListener ---
    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("Key pressed: " + e.getKeyCode()); // Debug line
        
        ContO m = getActiveModel();
        if (m == null) return;

        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_RIGHT:
                if (m == carModel) modelAngle += 5;
                else wheelAngle += 5;
                repaint();
                break;
            case KeyEvent.VK_LEFT:
                if (m == carModel) modelAngle -= 5;
                else wheelAngle -= 5;
                repaint();
                break;
            case KeyEvent.VK_UP:
                m.y += 5;
                repaint();
                break;
            case KeyEvent.VK_DOWN:
                m.y -= 5;
                repaint();
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:  // '+' key (with or without shift)
                m.y += 5;
                repaint();
                break;
            case KeyEvent.VK_MINUS:
                m.y -= 5;
                repaint();
                break;
            case KeyEvent.VK_C:
                colorScheme = (colorScheme + 1) % 3;
                System.out.println("Color scheme set to " + colorScheme);
                repaint();
                break;
        }
    }

    public void setColorScheme(int scheme) {
        this.colorScheme = scheme;
        repaint();
    }
    
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    
    
    private void changePolygonColor(int polyIndex) {
        ContO model = getActiveModel();
        if (model == null || polyIndex >= model.npl) return;
        
        Plane poly = model.p[polyIndex];
        
        // Get current color
        Color currentColor = new Color(poly.oc[0], poly.oc[1], poly.oc[2]);
        
        // Show compact color editor as overlay with callback
        ColorPaletteEditor.showCompactColorEditor(this, currentColor, newColor -> {
            // Update polygon color
            poly.c[0] = newColor.getRed();
            poly.c[1] = newColor.getGreen();
            poly.c[2] = newColor.getBlue();
            
            poly.oc[0] = newColor.getRed();
            poly.oc[1] = newColor.getGreen();
            poly.oc[2] = newColor.getBlue();
            
            // Update HSB
            float[] hsb = Color.RGBtoHSB(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), null);
            poly.hsb[0] = hsb[0];
            poly.hsb[1] = hsb[1];
            poly.hsb[2] = hsb[2];
            
            // Update the color in the model's skin lists
            int colorIndex = findColorIndex(model, polyIndex);
            if (colorIndex != -1) {
                SimpleColor sc = new SimpleColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
                model.original.set(colorIndex, sc);
                model.skin1.set(colorIndex, sc);
                model.skin2.set(colorIndex, sc);
            }
            
            // Notify parent to update the file
            updatePolygonColorInFile(polyIndex, newColor);
            
            repaint();
        });
    }

    private void translatePolygon(int polyIndex) {
        ContO model = getActiveModel();
        if (model == null || polyIndex >= model.npl) return;
        
        Plane poly = model.p[polyIndex];
        
        // Create dialog with input fields
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        JLabel xLabel = new JLabel("X offset:");
        JTextField xField = new JTextField("0", 10);
        
        JLabel yLabel = new JLabel("Y offset:");
        JTextField yField = new JTextField("0", 10);
        
        JLabel zLabel = new JLabel("Z offset:");
        JTextField zField = new JTextField("0", 10);
        
        panel.add(xLabel);
        panel.add(xField);
        panel.add(yLabel);
        panel.add(yField);
        panel.add(zLabel);
        panel.add(zField);
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Translate Polygon",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                int dx = Integer.parseInt(xField.getText().trim());
                int dy = Integer.parseInt(yField.getText().trim());
                int dz = Integer.parseInt(zField.getText().trim());
                
                // Apply translation to all vertices
                for (int v = 0; v < poly.n; v++) {
                    poly.ox[v] += dx;
                    poly.oy[v] += dy;
                    poly.oz[v] += dz;
                }
                
                // Notify parent to update the file
                updatePolygonTranslationInFile(polyIndex, dx, dy, dz);
                
                repaint();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid input. Please enter integer values.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void goToPolygonCode(int polyIndex) {
        // Get parent RadMergerGUI
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            RadMergerGUI gui = (RadMergerGUI) parent;
            gui.goToPolygonInEditor(polyIndex);
        }
    }

    private int findColorIndex(ContO model, int polyIndex) {
        // Count non-glass polygons before this one
        int colorIndex = 0;
        for (int i = 0; i < polyIndex; i++) {
            if (!model.p[i].glass) {
                colorIndex++;
            }
        }
        
        if (model.p[polyIndex].glass) {
            return -1;  // Glass polygons don't have colors in the list
        }
        
        return colorIndex;
    }

    private void updatePolygonColorInFile(int polyIndex, Color newColor) {
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            RadMergerGUI gui = (RadMergerGUI) parent;
            gui.updatePolygonColorInFile(polyIndex, newColor, colorScheme);
        }
    }

    private void updatePolygonTranslationInFile(int polyIndex, int dx, int dy, int dz) {
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            RadMergerGUI gui = (RadMergerGUI) parent;
            gui.updatePolygonTranslationInFile(polyIndex, dx, dy, dz);
        }
    }

    private void changeSelectedPolygonsColor() {
        if (selectedPolygons.isEmpty()) return;
        
        ContO model = getActiveModel();
        if (model == null) return;
        
        // Get color from first selected polygon
        int firstPoly = selectedPolygons.iterator().next();
        Plane poly = model.p[firstPoly];
        Color currentColor = new Color(poly.oc[0], poly.oc[1], poly.oc[2]);
        
        ColorPaletteEditor.showCompactColorEditor(this, currentColor, newColor -> {
            // Apply to all selected polygons
            for (int polyIndex : selectedPolygons) {
                changePolygonColorDirect(polyIndex, newColor);
            }
            
            // Clear selection after operation
            selectedPolygons.clear();
            selectionToolbar.clearSelection();
            repaint();
        });
    }

    private void changePolygonColorDirect(int polyIndex, Color newColor) {
        ContO model = getActiveModel();
        if (model == null || polyIndex >= model.npl) return;
        
        Plane poly = model.p[polyIndex];
        
        poly.c[0] = newColor.getRed();
        poly.c[1] = newColor.getGreen();
        poly.c[2] = newColor.getBlue();
        
        poly.oc[0] = newColor.getRed();
        poly.oc[1] = newColor.getGreen();
        poly.oc[2] = newColor.getBlue();
        
        float[] hsb = Color.RGBtoHSB(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), null);
        poly.hsb[0] = hsb[0];
        poly.hsb[1] = hsb[1];
        poly.hsb[2] = hsb[2];
        
        int colorIndex = findColorIndex(model, polyIndex);
        if (colorIndex != -1) {
            SimpleColor sc = new SimpleColor(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
            model.original.set(colorIndex, sc);
            model.skin1.set(colorIndex, sc);
            model.skin2.set(colorIndex, sc);
        }
        
        updatePolygonColorInFile(polyIndex, newColor);
    }

    private void translateSelectedPolygons() {
        if (selectedPolygons.isEmpty()) return;
        
        // Show translation dialog
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("X offset:"));
        JTextField xField = new JTextField("0");
        panel.add(xField);
        
        panel.add(new JLabel("Y offset:"));
        JTextField yField = new JTextField("0");
        panel.add(yField);
        
        panel.add(new JLabel("Z offset:"));
        JTextField zField = new JTextField("0");
        panel.add(zField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Translate Polygons", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                int dx = Integer.parseInt(xField.getText());
                int dy = Integer.parseInt(yField.getText());
                int dz = Integer.parseInt(zField.getText());
                
                // Find RadMergerGUI
                Component parent = this;
                while (parent != null && !(parent instanceof RadMergerGUI)) {
                    parent = parent.getParent();
                }
                
                if (parent instanceof RadMergerGUI) {
                    RadMergerGUI gui = (RadMergerGUI) parent;
                    
                    // Translate all selected polygons
                    for (int polyIndex : selectedPolygons) {
                        gui.updatePolygonTranslationInFile(polyIndex, dx, dy, dz);
                    }
                }
                
                // Clear selection after operation
                selectedPolygons.clear();
                selectionToolbar.clearSelection();
                repaint();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void goToSelectedPolygonCode() {
        if (selectedPolygons.isEmpty()) return;
        
        int firstPoly = selectedPolygons.iterator().next();
        goToPolygonCode(firstPoly);
        
        // Clear selection after operation
        selectedPolygons.clear();
        selectionToolbar.clearSelection();
        repaint();
    }

    private void removeSelectedPolygons() {
        if (selectedPolygons.isEmpty()) return;
        
        // Show confirmation
        int result = JOptionPane.showConfirmDialog(this,
            "Remove " + selectedPolygons.size() + " polygon(s)? This cannot be undone!",
            "Confirm Remove",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            // TODO: Implement polygon removal logic
            JOptionPane.showMessageDialog(this, "Remove polygons - Coming soon!");
            
            // Clear selection after operation
            selectedPolygons.clear();
            selectionToolbar.clearSelection();
            repaint();
        }
    }

    private boolean isInPaintMode() {
        // Find the color palette editor and check if paint mode is active
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            RadMergerGUI gui = (RadMergerGUI) parent;
            ColorPaletteEditor editor = gui.getColorPaletteEditor();
            return editor != null && editor.isVisible() && editor.isPaintModeActive();
        }
        return false;
    }

    private void paintPolygon(int polyIndex) {
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }

        if (parent instanceof RadMergerGUI) {
            RadMergerGUI gui = (RadMergerGUI) parent;
            ColorPaletteEditor editor = gui.getColorPaletteEditor();

            if (editor != null && editor.isPaintModeActive()) {
                Color paintColor = editor.getPaintColor();
                int paintScheme = editor.getPaintScheme();

                ContO model = getActiveModel();
                if (model == null || polyIndex >= model.npl) return;

                Plane poly = model.p[polyIndex];
                poly.c[0] = paintColor.getRed();
                poly.c[1] = paintColor.getGreen();
                poly.c[2] = paintColor.getBlue();
                poly.oc[0] = paintColor.getRed();
                poly.oc[1] = paintColor.getGreen();
                poly.oc[2] = paintColor.getBlue();

                float[] hsb = Color.RGBtoHSB(paintColor.getRed(), paintColor.getGreen(), paintColor.getBlue(), null);
                poly.hsb[0] = hsb[0];
                poly.hsb[1] = hsb[1];
                poly.hsb[2] = hsb[2];

                // Only update the skin list for the selected scheme
                int colorIndex = findColorIndex(model, polyIndex);
                if (colorIndex != -1) {
                    SimpleColor sc = new SimpleColor(paintColor.getRed(), paintColor.getGreen(), paintColor.getBlue());
                    switch (paintScheme) {
                        case 0: model.original.set(colorIndex, sc); break;
                        case 1: model.skin1.set(colorIndex, sc); break;
                        case 2: model.skin2.set(colorIndex, sc); break;
                    }
                }

                gui.updatePolygonColorInFile(polyIndex, paintColor, paintScheme);
                repaint();
            }
        }
    }

    public void hideToolbar() {
        selectedPolygons.clear();
        selectionToolbar.clearSelection();
        repaint();
    }

    public boolean isToolbarVisible() {
        return selectionToolbar != null && selectionToolbar.isVisible();
    }
}
