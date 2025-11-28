import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.*;

public class Rad3DViewer extends JPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private ContO model;
    private Medium medium;
    

    // This angle (in degrees) controls the rotation of the model around its Y-axis.
    private double modelAngle = 0;

    // For mouse dragging:
    private int lastMouseX, lastMouseY;

    // Color scheme: 0 = original (c()), 1 = skin1 (c1()), 2 = skin2 (c2())
    private int colorScheme = 0;

    // The drawing panel that does the custom painting.
    private DrawingPanel drawingPanel;

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
     * Loads the given .rad file, builds the ContO model using our Medium/Trackers,
     * and then positions the model.
     */
    public void loadRadFile(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            model = new ContO(fileData, medium);
            model.x = medium.cx;
            model.y = 250 - model.grat;
            model.z = 650;
            model.zy = 0;
            model.xz = 0;
            medium.crs = true;
            medium.ground = 650;
            medium.fogd = 8;
            medium.fadeFrom(2000);
            medium.y = -300;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        repaint();
    }

    private class DrawingPanel extends JPanel {

        public DrawingPanel() { setPreferredSize(new Dimension(1024, 768)); }

                @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (model == null) {
                g.drawString("No model loaded.", getWidth() / 2 - 50, getHeight() / 2);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pw = getWidth();
            int ph = getHeight();

            // Tell medium to match this panel
            medium.setViewport(0, 0, pw, ph);

            // Keep the car horizontally centered on the screen
            model.x = medium.cx;

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, pw, ph);

            medium.w = getWidth();
            medium.h = getHeight();
            medium.cx = medium.w / 2;
            medium.cy = medium.h / 2;
            medium.cz = medium.h / 2;
            medium.d(g2d);

            int nPlanes = model.npl;

            // Store original vertex positions so rotation doesn't accumulate
            int[][] backupOx = new int[nPlanes][];
            int[][] backupOz = new int[nPlanes][];

            for (int i = 0; i < nPlanes; i++) {
                Plane p = model.p[i];
                backupOx[i] = p.ox.clone();
                backupOz[i] = p.oz.clone();
            }

            // BEFORE rotation and rendering
            model.applySkin(colorScheme);

            // =============================================
            // Center + rotation
            // =============================================
            int totalX = 0, totalZ = 0, vcount = 0;
            for (int i = 0; i < nPlanes; i++) {
                Plane p = model.p[i];
                for (int v = 0; v < p.n; v++) {
                    totalX += p.ox[v];
                    totalZ += p.oz[v];
                    vcount++;
                }
            }

            int centerX = vcount > 0 ? totalX / vcount : 0;
            int centerZ = vcount > 0 ? totalZ / vcount : 0;

            float ang = (float) Math.toRadians(modelAngle);
            float cosA = (float) Math.cos(ang);
            float sinA = (float) Math.sin(ang);

            for (int i = 0; i < nPlanes; i++) {
                Plane p = model.p[i];
                for (int v = 0; v < p.n; v++) {
                    int ox = p.ox[v];
                    int oz = p.oz[v];

                    int rx = ox - centerX;
                    int rz = oz - centerZ;

                    p.ox[v] = centerX + (int) (rx * cosA - rz * sinA);
                    p.oz[v] = centerZ + (int) (rx * sinA + rz * cosA);
                }
            }

            model.d(g2d);

            // Restore original vertices
            for (int i = 0; i < nPlanes; i++) {
                Plane p = model.p[i];
                System.arraycopy(backupOx[i], 0, p.ox, 0, p.ox.length);
                System.arraycopy(backupOz[i], 0, p.oz, 0, p.oz.length);
            }
        }
    }

    // --- Listeners ---
    @Override
    public void mousePressed(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - lastMouseX;
        modelAngle += dx;
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        medium.z += notches * 100;
        repaint();
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_RIGHT) {
            modelAngle += 5;
            repaint();
        } else if (key == KeyEvent.VK_LEFT) {
            modelAngle -= 5;
            repaint();
        } else if (key == KeyEvent.VK_UP) {
            if (model != null) {
                model.y += 5;
                repaint();
            }
        } else if (key == KeyEvent.VK_DOWN) {
            if (model != null) {
                model.y -= 5;
                repaint();
            }
        } else if (key == KeyEvent.VK_C) {
            colorScheme = (colorScheme + 1) % 3;
            System.out.println("Color scheme set to " + colorScheme);
            repaint();
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
