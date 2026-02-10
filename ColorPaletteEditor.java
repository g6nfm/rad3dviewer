import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ColorPaletteEditor extends JPanel {
    private Rad3DViewer viewer;
    private JPanel controlPanel;
    
    private int currentScheme = 0; // 0=original, 1=scheme1, 2=scheme2
    private Map<String, Color> uniqueColors = new LinkedHashMap<>();
    private Color selectedColor = null;
    
    private JPanel colorGridPanel;
    private JPanel editorPanel;
    
    private JSlider hueSlider, brightnessSlider, saturationSlider;
    private JTextField rgbField;
    private JPanel colorPreview;
    private JComboBox<String> schemeSelector;
    
    public ColorPaletteEditor(Rad3DViewer viewer) {
        this.viewer = viewer;
        
        setLayout(new GridBagLayout());
        setOpaque(false);
        
        controlPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                g2d.setColor(new Color(100, 100, 100));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                
                g2d.dispose();
            }
        };
        
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        buildUI();
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(-350, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        add(controlPanel, gbc);
        setVisible(false);
    }
    
    private void buildUI() {
        // Title
        JLabel titleLabel = new JLabel("Color Palette Editor");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(titleLabel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Scheme selector
        JPanel schemePanel = new JPanel(new FlowLayout());
        schemePanel.setOpaque(false);
        JLabel schemeLabel = new JLabel("Editing:");
        schemeLabel.setForeground(Color.WHITE);
        schemePanel.add(schemeLabel);
        
        schemeSelector = new JComboBox<>(new String[]{"Original", "Scheme 1", "Scheme 2"});
        schemeSelector.addActionListener(e -> {
            currentScheme = schemeSelector.getSelectedIndex();
            
            // Switch the viewer to this color scheme
            Component parent = this;
            while (parent != null && !(parent instanceof RadMergerGUI)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof RadMergerGUI) {
                Rad3DViewer viewer = ((RadMergerGUI) parent).getViewer();
                if (viewer != null) {
                    viewer.setColorScheme(currentScheme);
                }
            }
            
            refreshColorGrid();
        });
        schemePanel.add(schemeSelector);
        controlPanel.add(schemePanel);
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Color grid panel
        colorGridPanel = new JPanel();
        colorGridPanel.setOpaque(false);
        colorGridPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        colorGridPanel.setMaximumSize(new Dimension(750, 100));
        
        JLabel gridLabel = new JLabel("Click a color to edit:");
        gridLabel.setForeground(Color.WHITE);
        gridLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(gridLabel);
        controlPanel.add(Box.createVerticalStrut(3));
        controlPanel.add(colorGridPanel);
        controlPanel.add(Box.createVerticalStrut(8));
        
        // Editor panel (hidden until color selected)
        editorPanel = new JPanel();
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));
        editorPanel.setOpaque(false);
        editorPanel.setVisible(false);

        // Create a horizontal layout for sliders on left, preview on right
        JPanel editorContentPanel = new JPanel(new BorderLayout(10, 0));
        editorContentPanel.setOpaque(false);
        editorContentPanel.setMaximumSize(new Dimension(900, 150));

        // Left side: Sliders and controls
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        // Hue slider
        hueSlider = createSlider("Hue:", 0, 360);
        hueSlider.addChangeListener(e -> {
            updateColorFromSliders();
            if (saturationSlider != null) {
                saturationSlider.repaint(); // Update saturation gradient based on new hue
            }
        });
        leftPanel.add(createSliderPanel("Hue:", hueSlider));

        // Brightness slider
        brightnessSlider = createSlider("Brightness:", 0, 100);
        brightnessSlider.addChangeListener(e -> updateColorFromSliders());
        leftPanel.add(createSliderPanel("Brightness:", brightnessSlider));

        // Saturation slider
        saturationSlider = createSlider("Saturation:", 0, 100);
        saturationSlider.addChangeListener(e -> updateColorFromSliders());
        leftPanel.add(createSliderPanel("Saturation:", saturationSlider));

        leftPanel.add(Box.createVerticalStrut(8));

        // RGB input - CENTERED
        JPanel rgbPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rgbPanel.setOpaque(false);
        JLabel rgbLabel = new JLabel("RGB:");
        rgbLabel.setForeground(Color.WHITE);
        rgbPanel.add(rgbLabel);

        rgbField = new JTextField("(0,0,0)", 12);
        rgbField.addActionListener(e -> updateColorFromRGB());
        rgbPanel.add(rgbField);

        JButton applyRGBBtn = new JButton("Set");
        applyRGBBtn.addActionListener(e -> updateColorFromRGB());
        rgbPanel.add(applyRGBBtn);

        leftPanel.add(rgbPanel);
        leftPanel.add(Box.createVerticalStrut(8));

        // Buttons - CENTERED, SIDE BY SIDE
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);

        JButton saveBtn = new JButton("Save Color");
        saveBtn.addActionListener(e -> saveColor());
        buttonPanel.add(saveBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            editorPanel.setVisible(false);
            selectedColor = null;
            controlPanel.revalidate();
            controlPanel.repaint();
        });

        buttonPanel.add(cancelBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> setVisible(false));
        buttonPanel.add(closeBtn);

        /* 
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> setVisible(false));
        buttonPanel.add(closeBtn);
        */
        

        leftPanel.add(buttonPanel);

        // Right side: Color preview
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(120, 120));
        colorPreview.setMaximumSize(new Dimension(120, 120));
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        rightPanel.add(colorPreview);

        editorContentPanel.add(leftPanel, BorderLayout.CENTER);
        editorContentPanel.add(rightPanel, BorderLayout.EAST);

        editorPanel.add(editorContentPanel);

        controlPanel.add(editorPanel);
        controlPanel.add(Box.createVerticalStrut(5));  // LESS SPACE since close is now inside
    }
    
    private JSlider createSlider(String label, int min, int max) {
        JSlider slider = new JSlider(min, max, min);
        slider.setOpaque(false);
        
        // Add gradient paint to slider track
        if (label.equals("Hue:")) {
            slider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
                @Override
                public void paintTrack(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    int width = trackRect.width;
                    int height = trackRect.height;
                    
                    // Rainbow gradient for hue
                    float[] fractions = {0.0f, 0.16f, 0.33f, 0.5f, 0.66f, 0.83f, 1.0f};
                    Color[] colors = {
                        Color.RED, Color.YELLOW, Color.GREEN, 
                        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
                    };
                    
                    LinearGradientPaint gradient = new LinearGradientPaint(
                        trackRect.x, trackRect.y,
                        trackRect.x + width, trackRect.y,
                        fractions, colors
                    );
                    
                    g2d.setPaint(gradient);
                    g2d.fillRect(trackRect.x, trackRect.y + height/2 - 2, width, 4);
                }
            });
        } else if (label.equals("Brightness:")) {
            slider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
                @Override
                public void paintTrack(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    int width = trackRect.width;
                    int height = trackRect.height;
                    
                    // Black to white gradient
                    GradientPaint gradient = new GradientPaint(
                        trackRect.x, trackRect.y, Color.BLACK,
                        trackRect.x + width, trackRect.y, Color.WHITE
                    );
                    
                    g2d.setPaint(gradient);
                    g2d.fillRect(trackRect.x, trackRect.y + height/2 - 2, width, 4);
                }
            });
        } else if (label.equals("Saturation:")) {
            slider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
                @Override
                public void paintTrack(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    int width = trackRect.width;
                    int height = trackRect.height;
                    
                    // Get current hue from hue slider to show proper saturation gradient
                    float currentHue = hueSlider != null ? hueSlider.getValue() / 360f : 0;
                    Color fullSat = Color.getHSBColor(currentHue, 1.0f, 1.0f);
                    Color noSat = Color.getHSBColor(currentHue, 0.0f, 1.0f);
                    
                    GradientPaint gradient = new GradientPaint(
                        trackRect.x, trackRect.y, noSat,
                        trackRect.x + width, trackRect.y, fullSat
                    );
                    
                    g2d.setPaint(gradient);
                    g2d.fillRect(trackRect.x, trackRect.y + height/2 - 2, width, 4);
                }
            });
        }
        
        return slider;
    }
    
    private JPanel createSliderPanel(String label, JSlider slider) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(550, 25));
        
        JLabel lbl = new JLabel(label);
        lbl.setForeground(Color.WHITE);
        lbl.setPreferredSize(new Dimension(90, 20));
        panel.add(lbl);
        
        slider.setPreferredSize(new Dimension(400, 20));
        panel.add(slider);
        
        return panel;
    }
    
    public void loadCarColors(String fileContent) {
        refreshColorGrid();
    }

    private Color parseRimColor(String line) {
        try {
            int start = line.indexOf('(') + 1;
            int end = line.indexOf(')');
            String params = line.substring(start, end);
            String[] values = params.split(",");
            
            // rims() has 5 parameters, first 3 are RGB
            int r = Integer.parseInt(values[0].trim());
            int g = Integer.parseInt(values[1].trim());
            int b = Integer.parseInt(values[2].trim());
            
            return new Color(r, g, b);
        } catch (Exception e) {
            return null;
        }
    }

    private JPanel createColorBlock(String key, boolean isRim) {
        String[] rgb = key.split(",");
        Color color = new Color(
            Integer.parseInt(rgb[0]),
            Integer.parseInt(rgb[1]),
            Integer.parseInt(rgb[2])
        );
        
        JPanel colorBlock = new JPanel();
        colorBlock.setPreferredSize(new Dimension(40, 40));
        colorBlock.setBackground(color);
        colorBlock.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        colorBlock.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add tooltip showing RGB value
        colorBlock.setToolTipText(String.format("RGB: (%s)%s", key, isRim ? " - Rim Color" : ""));
        
        colorBlock.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isRim) {
                    selectRimColor(color);
                } else {
                    selectColor(color);
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                colorBlock.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                colorBlock.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            }
        });
        
        return colorBlock;
    }

    private void selectRimColor(Color color) {
        selectedColor = color;
        editorPanel.setVisible(true);
        
        // Load color into sliders
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hueSlider.setValue((int)(hsb[0] * 360));
        saturationSlider.setValue((int)(hsb[1] * 100));
        brightnessSlider.setValue((int)(hsb[2] * 100));
        
        colorPreview.setBackground(color);
        rgbField.setText(String.format("(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));
        
        controlPanel.revalidate();
        controlPanel.repaint();
    }
    
    private void refreshColorGrid() {
        uniqueColors.clear();
        colorGridPanel.removeAll();
        
        String fileContent = getFileContent();
        if (fileContent == null) return;
        
        // Parse colors based on current scheme
        String colorTag = "";
        String rimTag = "";
        switch (currentScheme) {
            case 0: 
                colorTag = "c("; 
                rimTag = "rims(";
                break;
            case 1: 
                colorTag = "c1("; 
                rimTag = "rims1(";
                break;
            case 2: 
                colorTag = "c2("; 
                rimTag = "rims2(";
                break;
        }
        
        // Count color occurrences for BODY, WHEELS, and RIMS separately
        Map<String, Integer> bodyColorCounts = new HashMap<>();
        Map<String, Integer> wheelColorCounts = new HashMap<>();
        List<Color> rimColors = new ArrayList<>();
        
        String[] lines = fileContent.split("\n");
        boolean inWheelModel = false;
        boolean inPoly = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Parse rim colors
            if (trimmed.startsWith(rimTag)) {
                Color rimColor = parseRimColor(trimmed);
                if (rimColor != null) {
                    rimColors.add(rimColor);
                }
            }
            
            // Track if we're inside a wheelModel block
            if (trimmed.startsWith("<wheelModel(")) {
                inWheelModel = true;
            } else if (trimmed.startsWith("</wheelModel>")) {
                inWheelModel = false;
            }
            
            // Track if we're inside a polygon
            if (trimmed.startsWith("<p>") || trimmed.startsWith("[p]")) {
                inPoly = true;
            } else if (trimmed.startsWith("</p>") || trimmed.startsWith("[/p]")) {
                inPoly = false;
            }
            
            // Count colors
            if (inPoly && trimmed.startsWith(colorTag)) {
                Color color = parseColor(trimmed);
                if (color != null) {
                    String key = colorToKey(color);
                    
                    boolean isWheelPoly = inWheelModel;
                    
                    if (isWheelPoly) {
                        wheelColorCounts.put(key, wheelColorCounts.getOrDefault(key, 0) + 1);
                    } else {
                        bodyColorCounts.put(key, bodyColorCounts.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }
        
        // Sort both by count
        List<Map.Entry<String, Integer>> sortedBodyColors = new ArrayList<>(bodyColorCounts.entrySet());
        sortedBodyColors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        List<Map.Entry<String, Integer>> sortedWheelColors = new ArrayList<>(wheelColorCounts.entrySet());
        sortedWheelColors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Add BODY colors first
        if (!sortedBodyColors.isEmpty()) {
            JLabel bodyLabel = new JLabel("Body:  ");
            bodyLabel.setForeground(Color.WHITE);
            bodyLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            colorGridPanel.add(bodyLabel);
        }
        
        for (Map.Entry<String, Integer> entry : sortedBodyColors) {
            colorGridPanel.add(createColorBlock(entry.getKey(), false));
        }
        
        // Add WHEEL colors
        if (!sortedWheelColors.isEmpty()) {
            colorGridPanel.add(Box.createHorizontalStrut(20));
            
            JLabel wheelLabel = new JLabel("Wheels:  ");
            wheelLabel.setForeground(Color.WHITE);
            wheelLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            colorGridPanel.add(wheelLabel);
        }
        
        for (Map.Entry<String, Integer> entry : sortedWheelColors) {
            colorGridPanel.add(createColorBlock(entry.getKey(), false));
        }
        
        // Add RIM colors
        if (!rimColors.isEmpty()) {
            colorGridPanel.add(Box.createHorizontalStrut(20));
            
            JLabel rimLabel = new JLabel("Rims:  ");
            rimLabel.setForeground(Color.WHITE);
            rimLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            colorGridPanel.add(rimLabel);
            
            for (Color rimColor : rimColors) {
                colorGridPanel.add(createColorBlock(colorToKey(rimColor), true));
            }
        }
        
        colorGridPanel.revalidate();
        colorGridPanel.repaint();
        editorPanel.setVisible(false);
        selectedColor = null;
    }

    private JPanel createColorBlock(String key) {
        String[] rgb = key.split(",");
        Color color = new Color(
            Integer.parseInt(rgb[0]),
            Integer.parseInt(rgb[1]),
            Integer.parseInt(rgb[2])
        );
        
        JPanel colorBlock = new JPanel();
        colorBlock.setPreferredSize(new Dimension(40, 40));
        colorBlock.setBackground(color);
        colorBlock.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        colorBlock.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        colorBlock.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectColor(color);
            }
        });
        
        return colorBlock;
    }
    
    private void selectColor(Color color) {
        selectedColor = color;
        editorPanel.setVisible(true);
        
        // Load color into sliders
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hueSlider.setValue((int)(hsb[0] * 360));
        saturationSlider.setValue((int)(hsb[1] * 100));
        brightnessSlider.setValue((int)(hsb[2] * 100));
        
        colorPreview.setBackground(color);
        rgbField.setText(String.format("(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));
        
        controlPanel.revalidate();
        controlPanel.repaint();
    }
    
    private void updateColorFromSliders() {
        float h = hueSlider.getValue() / 360f;
        float s = saturationSlider.getValue() / 100f;
        float b = brightnessSlider.getValue() / 100f;
        
        Color color = Color.getHSBColor(h, s, b);
        colorPreview.setBackground(color);
        rgbField.setText(String.format("(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));
    }
    
    private void updateColorFromRGB() {
        try {
            String text = rgbField.getText().trim().replaceAll("[()]", "");
            String[] values = text.split(",");
            
            int r = Math.max(0, Math.min(255, Integer.parseInt(values[0].trim())));
            int g = Math.max(0, Math.min(255, Integer.parseInt(values[1].trim())));
            int b = Math.max(0, Math.min(255, Integer.parseInt(values[2].trim())));
            
            Color color = new Color(r, g, b);
            
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            hueSlider.setValue((int)(hsb[0] * 360));
            saturationSlider.setValue((int)(hsb[1] * 100));
            brightnessSlider.setValue((int)(hsb[2] * 100));
            
            colorPreview.setBackground(color);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid RGB format", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveColor() {
        if (selectedColor == null) return;
        
        float h = hueSlider.getValue() / 360f;
        float s = saturationSlider.getValue() / 100f;
        float b = brightnessSlider.getValue() / 100f;
        Color newColor = Color.getHSBColor(h, s, b);
        
        // Get parent and save
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            // Check if this is a rim color
            if (isRimColor(selectedColor)) {
                ((RadMergerGUI) parent).replaceRimColorInFile(selectedColor, newColor, currentScheme);
            } else {
                ((RadMergerGUI) parent).replaceColorInFile(selectedColor, newColor, currentScheme);
            }
            selectedColor = newColor;
            refreshColorGrid();
        }
    }

    private boolean isRimColor(Color color) {
        String fileContent = getFileContent();
        if (fileContent == null) return false;
        
        String rimTag = "";
        switch (currentScheme) {
            case 0: rimTag = "rims("; break;
            case 1: rimTag = "rims1("; break;
            case 2: rimTag = "rims2("; break;
        }
        
        String[] lines = fileContent.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(rimTag)) {
                Color rimColor = parseRimColor(line);
                if (rimColor != null && colorsMatch(rimColor, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean colorsMatch(Color c1, Color c2) {
        return c1.getRed() == c2.getRed() && 
            c1.getGreen() == c2.getGreen() && 
            c1.getBlue() == c2.getBlue();
    }
    
    private String getFileContent() {
        Component parent = this;
        while (parent != null && !(parent instanceof RadMergerGUI)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof RadMergerGUI) {
            return ((RadMergerGUI) parent).getRadTextContent();
        }
        return null;
    }
    
    private Color parseColor(String line) {
        try {
            int start = line.indexOf('(') + 1;
            int end = line.indexOf(')');
            String params = line.substring(start, end);
            String[] values = params.split(",");
            
            int r = Integer.parseInt(values[0].trim());
            int g = Integer.parseInt(values[1].trim());
            int b = Integer.parseInt(values[2].trim());
            
            return new Color(r, g, b);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String colorToKey(Color c) {
        return String.format("%d,%d,%d", c.getRed(), c.getGreen(), c.getBlue());
    }
    
    public int getCurrentScheme() {
        return currentScheme;
    }
}