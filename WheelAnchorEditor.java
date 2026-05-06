import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WheelAnchorEditor extends JPanel {
    private ContO carModel;
    private Rad3DViewer viewer;
    private JPanel controlPanel;
    private List<AxleRow> axleRows = new ArrayList<>();
    
    public WheelAnchorEditor(Rad3DViewer viewer) {
        this.viewer = viewer;
        
        setLayout(new GridBagLayout()); // Use GridBagLayout to center
        setOpaque(false); // Transparent background
        
        // Create the control panel with rounded corners
        controlPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded rectangle background
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Draw border
                g2d.setColor(new Color(100, 100, 100));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                
                g2d.dispose();
            }
        };
        
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false); // Make transparent so custom painting shows
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Wheel Anchor Editor");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(titleLabel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Center the panel at the top
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH; // Align to top
        gbc.insets = new Insets(-275, 0, 0, 0); // 10px padding from top
        
        add(controlPanel, gbc);
        
        setVisible(false); // Hidden by default
    }
    
   public void loadCarModel(ContO car, String fileContent) {
    this.carModel = car;
    axleRows.clear();
    controlPanel.removeAll();
    
    JLabel titleLabel = new JLabel("Wheel Anchor Editor");
    titleLabel.setForeground(Color.WHITE);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    controlPanel.add(titleLabel);
    controlPanel.add(Box.createVerticalStrut(10));
    
    if (car == null || fileContent == null) {
        JLabel noWheels = new JLabel("No wheels found in this car");
        noWheels.setForeground(Color.LIGHT_GRAY);
        controlPanel.add(noWheels);
        return;
    }
    
    // Parse wheel definitions from file content
    List<WheelData> wheels = parseWheelsFromFile(fileContent);
    
    if (wheels.isEmpty()) {
        JLabel noWheels = new JLabel("No wheels found in this car");
        noWheels.setForeground(Color.LIGHT_GRAY);
        controlPanel.add(noWheels);
        return;
    }
    
    // Group wheels into axles (pairs of left/right)
    List<WheelData> leftWheels = new ArrayList<>();
    List<WheelData> rightWheels = new ArrayList<>();
    
    for (WheelData wheel : wheels) {
        if (wheel.x < 0) {
            leftWheels.add(wheel);
        } else {
            rightWheels.add(wheel);
        }
    }
    
    // Add header row
    JPanel headerRow = createHeaderRow();
    controlPanel.add(headerRow);
    controlPanel.add(Box.createVerticalStrut(5));
    
    // Create rows for each axle
    int axleCount = Math.min(leftWheels.size(), rightWheels.size());
    for (int i = 0; i < axleCount; i++) {
        String axleName = i == 0 ? "Front" : i == 1 ? "Back" : "Axle " + i;
        AxleRow row = new AxleRow(axleName, leftWheels.get(i), rightWheels.get(i));
        axleRows.add(row);
        controlPanel.add(row);
        controlPanel.add(Box.createVerticalStrut(3));
    }
    
    // Add Apply and Close buttons
    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.setOpaque(false);
    
    JButton applyBtn = new JButton("Apply Changes");
    applyBtn.addActionListener(e -> applyChanges());
    
    JButton closeBtn = new JButton("Close");
    closeBtn.addActionListener(e -> setVisible(false));
    
    buttonPanel.add(applyBtn);
    buttonPanel.add(closeBtn);
    controlPanel.add(Box.createVerticalStrut(10));
    controlPanel.add(buttonPanel);
    
    controlPanel.revalidate();
    controlPanel.repaint();
}

private List<WheelData> parseWheelsFromFile(String content) {
    List<WheelData> wheels = new ArrayList<>();
    String[] lines = content.split("\n");
    int currentGwgr = 0;
    
    for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.startsWith("gwgr(")) {
            try {
                currentGwgr = Integer.parseInt(trimmed.substring(5, trimmed.indexOf(')')).trim());
            } catch (Exception e) { /* skip */ }
        }
        if (trimmed.startsWith("w(")) {
            try {
                int start = trimmed.indexOf('(') + 1;
                int end = trimmed.indexOf(')');
                String params = trimmed.substring(start, end);
                String[] values = params.split(",");
                
                WheelData wheel = new WheelData();
                wheel.x = Integer.parseInt(values[0].trim());
                wheel.y = Integer.parseInt(values[1].trim());
                wheel.z = Integer.parseInt(values[2].trim());
                wheel.rotation = Integer.parseInt(values[3].trim());
                wheel.width = Integer.parseInt(values[4].trim());
                wheel.height = Integer.parseInt(values[5].trim());
                if (values.length > 6) {
                    wheel.modelId = Integer.parseInt(values[6].trim());
                }
                wheel.gwgr = currentGwgr;
                
                wheels.add(wheel);
            } catch (Exception e) { /* skip */ }
        }
    }
    
    return wheels;
}

// Data class to hold wheel parameters
public class WheelData {
    public int x, y, z, rotation, width, height, modelId, gwgr;
}
    
    private JPanel createHeaderRow() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        header.setOpaque(false);
        
        String[] labels = {"Axle", "±X", "Y", "Z", "Width", "Height", "Model", "gwgr"};
        int[] widths = {50, 50, 50, 50, 60, 60, 60, 50};
        
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setForeground(Color.YELLOW);
            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setPreferredSize(new Dimension(widths[i], 20));
            header.add(label);
        }
        
        return header;
    }
    
    private void applyChanges() {
        if (carModel == null) return;
        
        for (AxleRow row : axleRows) {
            row.applyToAnchors();
        }
        
        viewer.repaint();
        
        // Get the parent RadMergerGUI and call the update method
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            ((RadMergerGUI) parent).updateWheelAnchorsInFile();
            JOptionPane.showMessageDialog(this, 
                "Wheel anchors updated and saved!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public WheelData getWheelData(int index) {
        // Get wheel data by index (0=left front, 1=right front, 2=left back, 3=right back)
        int axleIndex = index / 2;
        boolean isLeft = (index % 2 == 0);
        
        if (axleIndex >= axleRows.size()) return null;
        
        AxleRow axle = axleRows.get(axleIndex);
        return isLeft ? axle.leftWheel : axle.rightWheel;
    }
    
    // Inner class for each axle (pair of wheels)
        class AxleRow extends JPanel {
        private String name;
        private WheelData leftWheel;
        private WheelData rightWheel;
        private JTextField xField, yField, zField, widthField, heightField, modelField, gwgrField;

        public AxleRow(String name, WheelData leftWheel, WheelData rightWheel) {
            this.name = name;
            this.leftWheel = leftWheel;
            this.rightWheel = rightWheel;
            
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
            setOpaque(false);
            
            // Axle name label
            JLabel nameLabel = new JLabel(name);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setPreferredSize(new Dimension(50, 25));
            add(nameLabel);
            
            // Use values from left wheel
            xField = createField(Math.abs(leftWheel.x), 50);
            yField = createField(leftWheel.y, 50);
            zField = createField(leftWheel.z, 50);
            widthField = createField(leftWheel.width, 60);
            heightField = createField(leftWheel.height, 60);
            modelField = createField(leftWheel.modelId, 60);
            gwgrField = createField(leftWheel.gwgr, 50);
    
            add(xField);
            add(yField);
            add(zField);
            add(widthField);
            add(heightField);
            add(modelField);
            add(gwgrField);
        }
        
        private JTextField createField(int value, int width) {
            JTextField field = new JTextField(String.valueOf(value));
            field.setPreferredSize(new Dimension(width, 25));
            field.setFont(new Font("Monospaced", Font.PLAIN, 11));
            return field;
        }
        
        public void applyToAnchors() {
            try {
                int x = Math.abs(Integer.parseInt(xField.getText()));
                int y = Integer.parseInt(yField.getText());
                int z = Integer.parseInt(zField.getText());
                int width = Integer.parseInt(widthField.getText());  // Remove Math.abs()
                int height = Integer.parseInt(heightField.getText());
                int modelId = Integer.parseInt(modelField.getText());
                int gwgr = Integer.parseInt(gwgrField.getText());
                
                // Update the wheel data
                leftWheel.x = -x;
                leftWheel.y = y;
                leftWheel.z = z;
                leftWheel.width = width;  // Keep as-is
                leftWheel.height = height;
                leftWheel.modelId = modelId;
                leftWheel.gwgr = gwgr;
                
                rightWheel.x = x;
                rightWheel.y = y;
                rightWheel.z = z;
                rightWheel.width = width;  // Keep as-is
                rightWheel.height = height;
                rightWheel.modelId = modelId;
                rightWheel.gwgr = gwgr;
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid number in " + name + " axle", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}