import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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


    private boolean isWheelViewer = false;

    public void setWheelViewer(boolean b) {
        isWheelViewer = b;
    }

    public Rad3DViewer() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1024, 768));

        medium = new Medium();

        drawingPanel = new DrawingPanel();
        drawingPanel.setOpaque(false);
        add(drawingPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        JButton btnOriginal = new JButton("Original");
        btnOriginal.addActionListener(e -> {
            colorScheme = 0;
            repaint();
        });
        JButton btnSkin1 = new JButton("Color Scheme 1");
        btnSkin1.addActionListener(e -> {
            colorScheme = 1;
            repaint();
        });
        JButton btnSkin2 = new JButton("Color Scheme 2");
        btnSkin2.addActionListener(e -> {
            colorScheme = 2;
            repaint();
        });
        buttonPanel.add(btnOriginal);
        buttonPanel.add(btnSkin1);
        buttonPanel.add(btnSkin2);
        add(buttonPanel, BorderLayout.SOUTH);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        setFocusable(true);
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

            // Position the wheel in front of the camera box
            wheelModel.x = 110;
            wheelModel.y = 100;
            wheelModel.z = 400;     // closer than the car
            wheelModel.zy = 0;
            wheelModel.xz = 0;

            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        repaint();
    }

        private class DrawingPanel extends JPanel {

        public DrawingPanel() {
            setPreferredSize(new Dimension(1024, 768));
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
            Medium.cy = ph / 2;
            Medium.cz = ph / 2;

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

                carModel.d(g2d);

                for (int i = 0; i < nPlanes; i++) {
                    Plane p = carModel.p[i];
                    System.arraycopy(backupOx[i], 0, p.ox, 0, p.ox.length);
                    System.arraycopy(backupOz[i], 0, p.oz, 0, p.oz.length);
                }
            }

            // ============ WHEEL MODEL ============
            if (wheelModel != null) {
                int wn = wheelModel.npl;

                int[][] wBackupOx = new int[wn][];
                int[][] wBackupOz = new int[wn][];

                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    wBackupOx[i] = p.ox.clone();
                    wBackupOz[i] = p.oz.clone();
                }

                int wTotalX = 0, wTotalZ = 0, wCount = 0;
                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    for (int v = 0; v < p.n; v++) {
                        wTotalX += p.ox[v];
                        wTotalZ += p.oz[v];
                        wCount++;
                    }
                }

                int wCenterX = wCount > 0 ? wTotalX / wCount : 0;
                int wCenterZ = wCount > 0 ? wTotalZ / wCount : 0;

                float wAng = (float) Math.toRadians(wheelAngle);
                float wCos = (float) Math.cos(wAng);
                float wSin = (float) Math.sin(wAng);

                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    for (int v = 0; v < p.n; v++) {
                        int ox = p.ox[v];
                        int oz = p.oz[v];

                        int rx = ox - wCenterX;
                        int rz = oz - wCenterZ;

                        p.ox[v] = wCenterX + (int) (rx * wCos - rz * wSin);
                        p.oz[v] = wCenterZ + (int) (rx * wSin + rz * wCos);
                    }
                }

                wheelModel.d(g2d);

                for (int i = 0; i < wn; i++) {
                    Plane p = wheelModel.p[i];
                    System.arraycopy(wBackupOx[i], 0, p.ox, 0, p.ox.length);
                    System.arraycopy(wBackupOz[i], 0, p.oz, 0, p.oz.length);
                }
            }
        }
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

     // --- MouseListener ---
    @Override
    public void mousePressed(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // --- MouseMotionListener ---
    @Override
    public void mouseDragged(MouseEvent e) {
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
        repaint();
    }

    @Override public void mouseMoved(MouseEvent e) {}

    // --- MouseWheelListener ---
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        ContO m = getActiveModel();
        if (m == null) return;

        int notches = e.getWheelRotation();
        m.z += notches * 100;   // zoom whichever is active
        repaint();
    }

    // --- KeyListener ---
    @Override
    public void keyPressed(KeyEvent e) {
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
            case KeyEvent.VK_C:
                colorScheme = (colorScheme + 1) % 3;
                System.out.println("Color scheme set to " + colorScheme);
                repaint();
                break;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

}
