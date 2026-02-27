import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * AppLauncher — start screen that lets the user choose Car Maker or Stage Maker.
 * Change RadMergerGUI.main() to:  SwingUtilities.invokeLater(AppLauncher::new);
 */
public class AppLauncher extends JFrame {

    private static final Color BG     = new Color(22, 22, 35);
    private static final Color CARD   = new Color(32, 32, 50);
    private static final Color GREEN  = new Color(60, 200, 110);
    private static final Color ORANGE = new Color(255, 160, 60);
    private static final Color TEXT   = new Color(220, 220, 240);
    private static final Color DIM    = new Color(140, 140, 170);

    public AppLauncher() {
        setTitle("NFM Tools");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);

        // Title
        JLabel title = new JLabel("NFM Modding Tools", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 30, 0));
        root.add(title, BorderLayout.NORTH);

        // Cards
        JPanel cards = new JPanel(new GridLayout(1, 2, 30, 0));
        cards.setBackground(BG);
        cards.setBorder(BorderFactory.createEmptyBorder(0, 60, 50, 60));
        cards.add(buildCard("Car Maker",   "Build and edit\ncar .rad files", GREEN,  e -> openCarMaker()));
        cards.add(buildCard("Stage Maker", "Design and save\nstage .txt files", ORANGE, e -> openStageMaker()));
        root.add(cards, BorderLayout.CENTER);

        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildCard(String title, String desc, Color accent, ActionListener action) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(accent.darker().darker());
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 24));
        card.setPreferredSize(new Dimension(240, 200));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 20));
        titleLbl.setForeground(accent);
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(12));

        for (String line : desc.split("\n")) {
            JLabel dl = new JLabel(line);
            dl.setFont(new Font("Arial", Font.PLAIN, 13));
            dl.setForeground(DIM);
            dl.setAlignmentX(CENTER_ALIGNMENT);
            card.add(dl);
        }
        card.add(Box.createVerticalStrut(24));

        JButton btn = new JButton("Open");
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(accent.darker());
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 32, 8, 32));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.addActionListener(action);
        card.add(btn);
        return card;
    }

    private void openCarMaker() {
        dispose();
        SwingUtilities.invokeLater(RadMergerGUI::new);
    }

    private void openStageMaker() {
        dispose();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("Stage Maker");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setContentPane(new StageMakerPanel());
                f.setExtendedState(JFrame.MAXIMIZED_BOTH);
                f.setSize(1280, 768);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppLauncher::new);
    }
}
