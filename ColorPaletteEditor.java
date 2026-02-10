import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ColorPaletteEditor extends JPanel {
    private Rad3DViewer viewer;
    private JPanel controlPanel;
    
    private int currentScheme = 0; // 0=original, 1=scheme1, 2=scheme2
    private Map<String, Color> uniqueColors = new LinkedHashMap<>();
    private Color selectedColor = null;

    private boolean paintMode = false;
    private Color paintColor = null;
    private JButton paintModeBtn;
    
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
    // Title and Paint Mode button in a horizontal panel
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    headerPanel.setMaximumSize(new Dimension(780, 30));

    JLabel titleLabel = new JLabel("Color Palette Editor");
    titleLabel.setForeground(Color.WHITE);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    headerPanel.add(titleLabel, BorderLayout.WEST);

    // Paint mode button on the right
    paintModeBtn = new JButton("Paint Mode: OFF");
    paintModeBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
    paintModeBtn.addActionListener(e -> togglePaintMode());
    headerPanel.add(paintModeBtn, BorderLayout.EAST);

    controlPanel.add(headerPanel);
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
        // REMOVE this line that limits the height:
        // colorGridPanel.setMaximumSize(new Dimension(750, 100));
        // REPLACE with:
        colorGridPanel.setPreferredSize(new Dimension(750, 80)); // Initial size
        colorGridPanel.setMaximumSize(new Dimension(750, 300)); // Allow expansion up to 300px

        JLabel gridLabel = new JLabel("Click a color to edit:");
        gridLabel.setForeground(Color.WHITE);
        gridLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(gridLabel);
        controlPanel.add(Box.createVerticalStrut(3));

        // Wrap colorGridPanel in a JScrollPane for very large color counts
        JScrollPane colorScroll = new JScrollPane(colorGridPanel);
        colorScroll.setOpaque(false);
        colorScroll.getViewport().setOpaque(false);
        colorScroll.setBorder(null);
        colorScroll.setMaximumSize(new Dimension(780, 150)); // Scrollable area
        controlPanel.add(colorScroll);
        
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

        leftPanel.add(Box.createVerticalStrut(2));

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
        leftPanel.add(Box.createVerticalStrut(2));

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
        controlPanel.add(Box.createVerticalStrut(0));  // LESS SPACE since close is now inside
    }

    private void togglePaintMode() {
        paintMode = !paintMode;
        
        if (paintMode) {
            paintModeBtn.setText("Paint Mode: ON");
            paintModeBtn.setBackground(new Color(100, 200, 100));  // Green when active
            
            // Hide toolbar when paint mode is on
            Component parent = this;
            while (parent != null && !(parent instanceof RadMergerGUI)) {
                parent = parent.getParent();
            }
            
            if (parent instanceof RadMergerGUI) {
                RadMergerGUI gui = (RadMergerGUI) parent;
                Rad3DViewer viewer = gui.getViewer();
                if (viewer != null) {
                    viewer.hideToolbar();
                }
            }
            
            if (paintColor == null) {
                JOptionPane.showMessageDialog(this, 
                    "Click a color to select your paint brush color!", 
                    "Paint Mode", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            paintModeBtn.setText("Paint Mode: OFF");
            paintModeBtn.setBackground(null);  // Default color
            paintColor = null;
        }
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
        
       // Add RIM colors - ONLY if car doesn't have custom wheels
        if (!rimColors.isEmpty() && !hasCustomWheels(fileContent)) {
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

    public boolean isPaintModeActive() {
        return paintMode && paintColor != null;
    }

    public Color getPaintColor() {
        return paintColor;
    }
    
    private void selectColor(Color color) {
        if (paintMode) {
            // In paint mode, clicking a color sets it as the paint color
            paintColor = color;
            paintModeBtn.setText("Paint Mode: ON (Selected)");
            JOptionPane.showMessageDialog(this, 
                "Paint color selected! Now click polygons on the car to paint them.", 
                "Paint Mode", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Normal color editing mode
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

    private boolean hasCustomWheels(String fileContent) {
        if (fileContent == null) return false;
        return fileContent.contains("<wheelModel(");
    }

    /**
     * Shows a compact color editor as an overlay panel (not a dialog)
     */
    public static Color showCompactColorEditor(Component parent, Color initialColor, Consumer<Color> onSave) {
        // Find the RadMergerGUI
        Component root = parent;
        while (root != null && !(root instanceof RadMergerGUI)) {
            root = root.getParent();
        }
        
        if (!(root instanceof RadMergerGUI)) {
            return null;
        }
        
        RadMergerGUI gui = (RadMergerGUI) root;
        
        // Create overlay panel without background
        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setOpaque(false);
        
        // Main panel with rounded rectangle
        JPanel mainPanel = new JPanel() {
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
        
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));  // Reduced top/bottom
        
        // Title
        JLabel titleLabel = new JLabel("Edit Polygon Color");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        
        // Color preview panel (smaller)
        JPanel previewPanel = new JPanel();
        previewPanel.setPreferredSize(new Dimension(100, 100));
        previewPanel.setMaximumSize(new Dimension(100, 100));
        previewPanel.setBackground(initialColor);
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        
        // Sliders panel
        JPanel slidersPanel = new JPanel();
        slidersPanel.setLayout(new BoxLayout(slidersPanel, BoxLayout.Y_AXIS));
        slidersPanel.setOpaque(false);
        
        float[] hsb = Color.RGBtoHSB(initialColor.getRed(), initialColor.getGreen(), initialColor.getBlue(), null);
        
        // Hue slider with rainbow gradient
        JSlider hueSlider = new JSlider(0, 360, (int)(hsb[0] * 360));
        hueSlider.setOpaque(false);
        hueSlider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
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
        JPanel huePanel = createStyledSliderPanel("Hue:", hueSlider);
        slidersPanel.add(huePanel);
        slidersPanel.add(Box.createVerticalStrut(2));
        
        // Brightness slider with black to white gradient
        JSlider brightSlider = new JSlider(0, 100, (int)(hsb[2] * 100));
        brightSlider.setOpaque(false);
        brightSlider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
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
        JPanel brightPanel = createStyledSliderPanel("Brightness:", brightSlider);
        slidersPanel.add(brightPanel);
        slidersPanel.add(Box.createVerticalStrut(2));
        
        // Saturation slider with dynamic gradient based on hue
        JSlider satSlider = new JSlider(0, 100, (int)(hsb[1] * 100));
        satSlider.setOpaque(false);
        satSlider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                int width = trackRect.width;
                int height = trackRect.height;
                
                // Get current hue from hue slider to show proper saturation gradient
                float currentHue = hueSlider.getValue() / 360f;
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
        JPanel satPanel = createStyledSliderPanel("Saturation:", satSlider);
        slidersPanel.add(satPanel);
        
        // Create horizontal layout with sliders on left, preview on right
        JPanel contentPanel = new JPanel(new BorderLayout(15, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(slidersPanel, BorderLayout.CENTER);
        contentPanel.add(previewPanel, BorderLayout.EAST);
        
        mainPanel.add(contentPanel);
        mainPanel.add(Box.createVerticalStrut(2));
        
        // RGB input
        JPanel rgbPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rgbPanel.setOpaque(false);
        JLabel rgbLabel = new JLabel("RGB:");
        rgbLabel.setForeground(Color.WHITE);
        rgbPanel.add(rgbLabel);
        
        JTextField rgbField = new JTextField(
            String.format("(%d,%d,%d)", initialColor.getRed(), initialColor.getGreen(), initialColor.getBlue()),
            12
        );
        rgbPanel.add(rgbField);
        
        JButton applyRGBBtn = new JButton("Set");
        rgbPanel.add(applyRGBBtn);
    mainPanel.add(rgbPanel);
    mainPanel.add(Box.createVerticalStrut(2));
        
        // Update preview when sliders change
        ChangeListener updatePreview = e -> {
            float h = hueSlider.getValue() / 360f;
            float s = satSlider.getValue() / 100f;
            float b = brightSlider.getValue() / 100f;
            Color newColor = Color.getHSBColor(h, s, b);
            previewPanel.setBackground(newColor);
            rgbField.setText(String.format("(%d,%d,%d)", newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
            
            // Repaint saturation slider when hue changes
            if (e.getSource() == hueSlider) {
                satSlider.repaint();
            }
        };
        
        hueSlider.addChangeListener(updatePreview);
        satSlider.addChangeListener(updatePreview);
        brightSlider.addChangeListener(updatePreview);
        
        // Apply RGB button
        applyRGBBtn.addActionListener(e -> {
            try {
                String text = rgbField.getText().trim().replaceAll("[()]", "");
                String[] values = text.split(",");
                
                int r = Math.max(0, Math.min(255, Integer.parseInt(values[0].trim())));
                int g = Math.max(0, Math.min(255, Integer.parseInt(values[1].trim())));
                int b = Math.max(0, Math.min(255, Integer.parseInt(values[2].trim())));
                
                Color color = new Color(r, g, b);
                float[] newHsb = Color.RGBtoHSB(r, g, b, null);
                
                hueSlider.setValue((int)(newHsb[0] * 360));
                satSlider.setValue((int)(newHsb[1] * 100));
                brightSlider.setValue((int)(newHsb[2] * 100));
                
                previewPanel.setBackground(color);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel, "Invalid RGB format", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton okButton = new JButton("Save Color");
        JButton cancelButton = new JButton("Cancel");
        
        final Color[] result = {null};
        
        okButton.addActionListener(e -> {
            float h = hueSlider.getValue() / 360f;
            float s = satSlider.getValue() / 100f;
            float b = brightSlider.getValue() / 100f;
            result[0] = Color.getHSBColor(h, s, b);
            onSave.accept(result[0]);
            gui.removeCompactColorEditor(overlay);
        });
        
        cancelButton.addActionListener(e -> {
            result[0] = null;
            gui.removeCompactColorEditor(overlay);
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel);
        
        // Position it same as main color palette editor (top center)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(-350, 0, 0, 0);  // Same positioning as main editor
        overlay.add(mainPanel, gbc);
        
        // Add overlay to GUI
        gui.showCompactColorEditor(overlay);
        
        return result[0];
    }

    private static JPanel createStyledSliderPanel(String label, JSlider slider) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(400, 25));
        
        JLabel lbl = new JLabel(label);
        lbl.setForeground(Color.WHITE);
        lbl.setPreferredSize(new Dimension(90, 20));
        panel.add(lbl);
        
        slider.setPreferredSize(new Dimension(300, 20));
        panel.add(slider);
        
        return panel;
    }
}