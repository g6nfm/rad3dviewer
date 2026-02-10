import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class SelectionToolbar extends JPanel {
    private JLabel selectionLabel;
    private JButton changeColorBtn;
    private JButton translateBtn;
    private JButton goToCodeBtn;
    private JButton removeBtn;
    private JButton closeBtn;
    
    private Set<Integer> selectedPolygons;
    private int totalPolygons;
    
    public SelectionToolbar() {
        selectedPolygons = new HashSet<>();
        
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(1000, 50));
        setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        
        // Left side - selection info
        selectionLabel = new JLabel("[ No selection ]");
        selectionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        selectionLabel.setForeground(Color.WHITE);
        selectionLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(selectionLabel, BorderLayout.WEST);
        
        // Right side - action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        buttonPanel.setOpaque(false);
        
        // Just create buttons normally - no custom styling
        changeColorBtn = new JButton("Change Color");
        translateBtn = new JButton("Translate Polygon...");
        goToCodeBtn = new JButton("Edit in code tab");
        removeBtn = new JButton("Remove from the model");
        closeBtn = new JButton("Close");
        
        buttonPanel.add(changeColorBtn);
        buttonPanel.add(translateBtn);
        buttonPanel.add(goToCodeBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(closeBtn);
        
        add(buttonPanel, BorderLayout.CENTER);
        
        setVisible(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw rounded rectangle background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        
        // Draw border
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
        
        g2d.dispose();
    }
    
    
    public void setSelection(Set<Integer> polygons, int total) {
        this.selectedPolygons = new HashSet<>(polygons);
        this.totalPolygons = total;
        updateLabel();
        setVisible(!polygons.isEmpty());
    }
    
    public void addToSelection(int polyIndex) {
        selectedPolygons.add(polyIndex);
        updateLabel();
        setVisible(true);
    }
    
    public void clearSelection() {
        selectedPolygons.clear();
        setVisible(false);
    }
    
    private void updateLabel() {
        if (selectedPolygons.isEmpty()) {
            selectionLabel.setText("[ No selection ]");
        } else if (selectedPolygons.size() == 1) {
            int poly = selectedPolygons.iterator().next();
            selectionLabel.setText(String.format("[ Piece %d of %d (%d) selected ]", 
                poly + 1, totalPolygons, totalPolygons));
        } else {
            selectionLabel.setText(String.format("[ %d pieces selected ]", selectedPolygons.size()));
        }
    }
    
    public Set<Integer> getSelectedPolygons() {
        return new HashSet<>(selectedPolygons);
    }
    
    public void setChangeColorAction(Runnable action) {
        ActionListener[] listeners = changeColorBtn.getActionListeners();
        for (ActionListener listener : listeners) {
            changeColorBtn.removeActionListener(listener);
        }
        changeColorBtn.addActionListener(e -> action.run());
    }
    
    public void setTranslateAction(Runnable action) {
        ActionListener[] listeners = translateBtn.getActionListeners();
        for (ActionListener listener : listeners) {
            translateBtn.removeActionListener(listener);
        }
        translateBtn.addActionListener(e -> action.run());
    }
    
    public void setGoToCodeAction(Runnable action) {
        ActionListener[] listeners = goToCodeBtn.getActionListeners();
        for (ActionListener listener : listeners) {
            goToCodeBtn.removeActionListener(listener);
        }
        goToCodeBtn.addActionListener(e -> action.run());
    }
    
    public void setRemoveAction(Runnable action) {
        ActionListener[] listeners = removeBtn.getActionListeners();
        for (ActionListener listener : listeners) {
            removeBtn.removeActionListener(listener);
        }
        removeBtn.addActionListener(e -> action.run());
    }
    
    public void setCloseAction(Runnable action) {
        ActionListener[] listeners = closeBtn.getActionListeners();
        for (ActionListener listener : listeners) {
            closeBtn.removeActionListener(listener);
        }
        closeBtn.addActionListener(e -> action.run());
    }
}