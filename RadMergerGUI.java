import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.*;

public class RadMergerGUI extends JFrame {
    private FileDropPanel filePanel1;
    private FileDropPanel filePanel2;
    private FileDropPanel filePanel3;
    private JLabel statusLabel;

    private JTabbedPane tabbedPane;
    private JComboBox<String> carDropdown;

    // Tab 1: 3D Viewer container.
    private JPanel viewerContainer;
    // Tab 2: Panel for editing the .rad file text.
    private JPanel radFilePanel;
    private JTextArea radTextArea;
    
    private JComboBox<String> wheelDropdown;
    private Rad3DViewer wheelViewer;
    private File wheelsFolder = new File("cars/wheels");
    private JPanel wheelViewerContainer;

    // Path of the last merged file (null until first merge)
    private String mergedFilePath = null;

    private Rad3DViewer viewer;

    private WheelAnchorEditor wheelAnchorEditor;
    private ColorPaletteEditor colorPaletteEditor;
    
    private JButton scheme3Btn;

    private File carsFolder = new File("cars");

    public RadMergerGUI() {
        setTitle("RAD File Merger & Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 1024);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));


        // =========================
        // LEFT SIDE PANEL (Car + Wheel Selectors)
        // =========================
        JPanel leftSide = new JPanel();
        leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
        leftSide.setPreferredSize(new Dimension(260, 900)); 
        leftSide.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(leftSide, BorderLayout.WEST);

        // =========================
        // CAR SELECTOR PANEL
        // =========================
        JPanel carSelectorPanel = new JPanel();
        carSelectorPanel.setLayout(new BoxLayout(carSelectorPanel, BoxLayout.Y_AXIS));
        carSelectorPanel.setBorder(BorderFactory.createTitledBorder("Select Car"));

        carDropdown = new JComboBox<>();
        carDropdown.setFont(new Font("SansSerif", Font.PLAIN, 15));
        carDropdown.setMaximumSize(new Dimension(220, 35));
        carDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ADD THIS:
        JButton createCarBtn = new JButton("Create New Car");
        createCarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createCarBtn.setMaximumSize(new Dimension(220, 35));
        createCarBtn.addActionListener(e -> createNewCar());
        carSelectorPanel.add(createCarBtn);
        carSelectorPanel.add(Box.createVerticalStrut(5));

        // ADD THIS:
        JButton importCarBtn = new JButton("Import Car Code");
        importCarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        importCarBtn.setMaximumSize(new Dimension(220, 35));
        importCarBtn.addActionListener(e -> importCarCode());
        carSelectorPanel.add(importCarBtn);
        carSelectorPanel.add(Box.createVerticalStrut(5));

        JButton deleteCarBtn = new JButton("Delete Car");
        deleteCarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteCarBtn.setMaximumSize(new Dimension(220, 35));
        deleteCarBtn.addActionListener(e -> deleteSelectedCar());
        carSelectorPanel.add(deleteCarBtn);
        carSelectorPanel.add(Box.createVerticalStrut(10));

        carSelectorPanel.add(Box.createVerticalStrut(10));
        carSelectorPanel.add(carDropdown);
        carSelectorPanel.add(Box.createVerticalStrut(10));

        leftSide.add(carSelectorPanel);

        // =========================
        // WHEEL SELECTOR PANEL
        // =========================
        JPanel wheelPanel = new JPanel();
        wheelPanel.setLayout(new BoxLayout(wheelPanel, BoxLayout.Y_AXIS));
        wheelPanel.setBorder(BorderFactory.createTitledBorder("Select Wheel Model"));

        // Dropdown
        wheelDropdown = new JComboBox<>();
        wheelDropdown.setFont(new Font("SansSerif", Font.PLAIN, 15));
        wheelDropdown.setMaximumSize(new Dimension(220, 35));
        wheelDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);

        wheelPanel.add(Box.createVerticalStrut(10));
        wheelPanel.add(wheelDropdown);
        wheelPanel.add(Box.createVerticalStrut(10));

        // Mini wheel viewer
        wheelViewerContainer = new JPanel(new BorderLayout());
        wheelViewerContainer.setPreferredSize(new Dimension(240, 240));
        wheelViewerContainer.setMaximumSize(new Dimension(240, 240));

        wheelViewer = new Rad3DViewer();
        wheelViewer.setWheelViewer(true); 
        wheelViewer.setPreferredSize(new Dimension(240, 240));
        
        wheelViewerContainer.add(wheelViewer, BorderLayout.CENTER);

        wheelPanel.add(wheelViewerContainer);

        // 1. ADD THE BUTTON - Right after wheelPanel.add(wheelViewerContainer); around line 87
        //wheelPanel.add(wheelViewerContainer);

        // ADD THIS - Import Wheel Code button
        JButton importWheelBtn = new JButton("Import Wheel Code");
        importWheelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        importWheelBtn.setMaximumSize(new Dimension(220, 35));
        importWheelBtn.addActionListener(e -> importWheelCode());
        wheelPanel.add(Box.createVerticalStrut(10));
        wheelPanel.add(importWheelBtn);

        // ADD THIS:
        JButton applyWheelsBtn = new JButton("Apply Wheels to Car");
        applyWheelsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        applyWheelsBtn.setMaximumSize(new Dimension(220, 35));
        applyWheelsBtn.addActionListener(e -> applyWheelsToSelectedCar());
        wheelPanel.add(Box.createVerticalStrut(10));
        wheelPanel.add(applyWheelsBtn);

        // ADD THIS:
        JButton removeWheelsBtn = new JButton("Remove Custom Wheels");
        removeWheelsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeWheelsBtn.setMaximumSize(new Dimension(220, 35));
        removeWheelsBtn.addActionListener(e -> removeCustomWheels());
        wheelPanel.add(Box.createVerticalStrut(5));
        wheelPanel.add(removeWheelsBtn);

        // Add wheel section to left side
        leftSide.add(Box.createVerticalStrut(20));
        leftSide.add(wheelPanel);

        // =========================
        // COLOR SCHEME PANEL
        // =========================
        JPanel colorSchemePanel = new JPanel();
        colorSchemePanel.setLayout(new BoxLayout(colorSchemePanel, BoxLayout.Y_AXIS));
        colorSchemePanel.setBorder(BorderFactory.createTitledBorder("Color Schemes"));

        JButton originalBtn = new JButton("Original");
        originalBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        originalBtn.setMaximumSize(new Dimension(220, 35));
        originalBtn.addActionListener(e -> {
            viewer.setColorScheme(0);
            colorPaletteEditor.syncScheme(0);
        });

        JButton scheme1Btn = new JButton("Color Scheme 1");
        scheme1Btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheme1Btn.setMaximumSize(new Dimension(220, 35));
        scheme1Btn.addActionListener(e -> {
            viewer.setColorScheme(1);
            colorPaletteEditor.syncScheme(1);
        });

        JButton scheme2Btn = new JButton("Color Scheme 2");
        scheme2Btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheme2Btn.setMaximumSize(new Dimension(220, 35));
        scheme2Btn.addActionListener(e -> {
            viewer.setColorScheme(2);
            colorPaletteEditor.syncScheme(2);
        });

        scheme3Btn = new JButton("Color Scheme 3");
        scheme3Btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheme3Btn.setMaximumSize(new Dimension(220, 35));
        scheme3Btn.setVisible(false);
        scheme3Btn.addActionListener(e -> {
            viewer.setColorScheme(3);
            colorPaletteEditor.syncScheme(3);
        });

        colorSchemePanel.add(Box.createVerticalStrut(10));
        colorSchemePanel.add(originalBtn);
        colorSchemePanel.add(Box.createVerticalStrut(5));
        colorSchemePanel.add(scheme1Btn);
        colorSchemePanel.add(Box.createVerticalStrut(5));
        colorSchemePanel.add(scheme2Btn);
        colorSchemePanel.add(Box.createVerticalStrut(5));
        colorSchemePanel.add(scheme3Btn);
        colorSchemePanel.add(Box.createVerticalStrut(10));

        leftSide.add(Box.createVerticalStrut(20));
        leftSide.add(colorSchemePanel);
        


        // Fill dropdown
        refreshDropdown();
        refreshWheelDropdown();     // wheels


        // --- Top drag-and-drop slots ---
        JPanel dropPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        filePanel1 = new FileDropPanel("Slot 1 (.rad file)", 1);
        filePanel2 = new FileDropPanel("Slot 2 (.rad file)", 2);
        filePanel3 = new FileDropPanel("Slot 3 (.rad file)", 3);
        dropPanel.add(filePanel1);
        dropPanel.add(filePanel2);
        dropPanel.add(filePanel3);
        add(dropPanel, BorderLayout.NORTH);

        // --- Tabs ---
        tabbedPane = new JTabbedPane();

        // Tab 1: Model Viewer
        viewerContainer = new JPanel(new BorderLayout());
        viewer = new Rad3DViewer();

        // Create the wheel anchor editor overlay
        wheelAnchorEditor = new WheelAnchorEditor(viewer);

        // Create the color palette editor overlay
        colorPaletteEditor = new ColorPaletteEditor(viewer);

        // Layer them using JLayeredPane
        final JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1024, 768));

        layeredPane.add(viewer, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(wheelAnchorEditor, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(colorPaletteEditor, JLayeredPane.PALETTE_LAYER);

        // Add component listener to resize all layers when viewerContainer resizes
        viewerContainer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = viewerContainer.getWidth();
                int h = viewerContainer.getHeight();
                
                // Resize layered pane to match container
                layeredPane.setBounds(0, 0, w, h);
                layeredPane.setPreferredSize(new Dimension(w, h));
                
                // Resize all children to match
                viewer.setBounds(0, 0, w, h);
                wheelAnchorEditor.setBounds(0, 0, w, h);
                colorPaletteEditor.setBounds(0, 0, w, h);
                
                layeredPane.revalidate();
                layeredPane.repaint();
            }
        });

        // Set initial bounds
        viewer.setBounds(0, 0, 1024, 768);
        wheelAnchorEditor.setBounds(0, 0, 1024, 768);
        colorPaletteEditor.setBounds(0, 0, 1024, 768);

        viewerContainer.add(layeredPane, BorderLayout.CENTER);

        // Add buttons to toggle editors
        JButton toggleWheelEditorBtn = new JButton("Edit Wheel Anchors");
        toggleWheelEditorBtn.addActionListener(e -> {
            if (viewer.getCarModel() != null) {
                // Hide toolbar if open
                if (viewer.isToolbarVisible()) {
                    viewer.hideToolbar();
                }
                
                wheelAnchorEditor.loadCarModel(viewer.getCarModel(), radTextArea.getText());
                wheelAnchorEditor.setVisible(!wheelAnchorEditor.isVisible());
                // Hide color editor when showing wheel editor
                if (wheelAnchorEditor.isVisible()) {
                    colorPaletteEditor.setVisible(false);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Load a car first!", "No Car", JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton toggleColorEditorBtn = new JButton("Edit Colors");
        toggleColorEditorBtn.addActionListener(e -> {
            if (mergedFilePath != null) {
                // Hide toolbar if open
                if (viewer.isToolbarVisible()) {
                    viewer.hideToolbar();
                }
                
                colorPaletteEditor.loadCarColors(radTextArea.getText());
                colorPaletteEditor.setVisible(!colorPaletteEditor.isVisible());
                // Hide wheel editor when showing color editor
                if (colorPaletteEditor.isVisible()) {
                    wheelAnchorEditor.setVisible(false);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Load a car first!", "No Car", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel viewerToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewerToolbar.add(toggleWheelEditorBtn);
        viewerToolbar.add(toggleColorEditorBtn); // ADD THIS
        viewerContainer.add(viewerToolbar, BorderLayout.SOUTH);

        tabbedPane.addTab("Model Viewer", viewerContainer);

        // Tab 2: RAD File Editor
        radFilePanel = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSave = new JButton("Save");
        JButton btnFind = new JButton("Find");
        JButton btnReplace = new JButton("Replace");
        toolbar.add(btnSave);
        toolbar.add(btnFind);
        toolbar.add(btnReplace);

        radTextArea = new JTextArea();
        radTextArea.setFont(new Font("Monospaced", Font.BOLD, 16));
        radTextArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(radTextArea);

        radFilePanel.add(toolbar, BorderLayout.NORTH);
        radFilePanel.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab(".rad File Editor", radFilePanel);

        add(tabbedPane, BorderLayout.CENTER);

        // --- Bottom merge + status ---
        JPanel bottomPanel = new JPanel();
        JButton mergeButton = new JButton("Merge");
        mergeButton.addActionListener(e -> onMerge());
        bottomPanel.add(mergeButton);
        statusLabel = new JLabel("Drag and drop your .rad files into the slots above.");
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Toolbar actions ---
        btnSave.addActionListener(e -> {
        if (mergedFilePath == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "No merged file yet. Click Merge first.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(mergedFilePath))) {
            out.print(radTextArea.getText());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving file: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // ADD THESE LINES TO RELOAD THE VIEWER:
        try {
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();
        } catch (Exception ex) {
            // Silent fail on reload, file is still saved
        }
        
        JOptionPane.showMessageDialog(this, "File saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
    });

        btnFind.addActionListener(e -> {
            String searchTerm = JOptionPane.showInputDialog(this, "Enter text to find:");
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String content = radTextArea.getText();
                int pos = content.indexOf(searchTerm, radTextArea.getCaretPosition());
                if (pos == -1) pos = content.indexOf(searchTerm);
                if (pos != -1) {
                    radTextArea.requestFocus();
                    radTextArea.select(pos, pos + searchTerm.length());
                } else {
                    JOptionPane.showMessageDialog(this, "Text not found.", "Find", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        btnReplace.addActionListener(e -> {
        // Create a panel with options
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        JLabel findLabel = new JLabel("Find:");
        JTextField findField = new JTextField(20);
        
        JLabel replaceLabel = new JLabel("Replace with:");
        JTextField replaceField = new JTextField(20);
        
        JCheckBox matchCaseBox = new JCheckBox("Match case");
        
        panel.add(findLabel);
        panel.add(findField);
        panel.add(replaceLabel);
        panel.add(replaceField);
        panel.add(matchCaseBox);
        panel.add(new JLabel("")); // Empty cell
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Find and Replace",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String searchTerm = findField.getText();
            String replaceTerm = replaceField.getText();
            
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String content = radTextArea.getText();
                
                if (matchCaseBox.isSelected()) {
                    content = content.replace(searchTerm, replaceTerm);
                } else {
                    // Case-insensitive replace
                    content = content.replaceAll("(?i)" + java.util.regex.Pattern.quote(searchTerm), 
                                                java.util.regex.Matcher.quoteReplacement(replaceTerm));
                }
                
                radTextArea.setText(content);
                JOptionPane.showMessageDialog(this, 
                    "Replaced all occurrences.", 
                    "Replace Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RadMergerGUI::new);
    }
    
    public Rad3DViewer getViewer() {
        return viewer;
    }

    public void replaceRimColorInFile(Color oldColor, Color newColor, int scheme) {
        if (mergedFilePath == null) return;
        
        try {
            String content = radTextArea.getText();
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            
            String rimTag = "";
            switch (scheme) {
                case 0: rimTag = "rims("; break;
                case 1: rimTag = "rims1("; break;
                case 2: rimTag = "rims2("; break;
                case 3: rimTag = "rims3("; break;
            }
            
            for (String line : lines) {
                String trimmed = line.trim();
                
                if (trimmed.startsWith(rimTag)) {
                    // Parse the rim color
                    try {
                        int start = trimmed.indexOf('(') + 1;
                        int end = trimmed.indexOf(')');
                        String params = trimmed.substring(start, end);
                        String[] values = params.split(",");
                        
                        int r = Integer.parseInt(values[0].trim());
                        int g = Integer.parseInt(values[1].trim());
                        int b = Integer.parseInt(values[2].trim());
                        
                        if (r == oldColor.getRed() && g == oldColor.getGreen() && b == oldColor.getBlue()) {
                            // Replace the rim color but keep the other parameters
                            String indent = line.substring(0, line.indexOf(rimTag.charAt(0)));
                            result.append(indent).append(String.format("%s%d,%d,%d,%s,%s)\n",
                                rimTag, newColor.getRed(), newColor.getGreen(), newColor.getBlue(),
                                values[3].trim(), values[4].trim()));
                            continue;
                        }
                    } catch (Exception e) {
                        // Keep original if parsing fails
                    }
                }
                
                result.append(line).append("\n");
            }
            
            radTextArea.setText(result.toString());
            Files.write(Paths.get(mergedFilePath), result.toString().getBytes());
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onMerge() {
        // Need three files
        if (filePanel1.getFile() == null || filePanel2.getFile() == null || filePanel3.getFile() == null) {
            JOptionPane.showMessageDialog(this, "Please drop a file into each slot.", "Missing File", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ask user for output file name
        String name = JOptionPane.showInputDialog(
                this,
                "Enter a name for the merged car file (without .rad):",
                "Save As",
                JOptionPane.PLAIN_MESSAGE
        );

        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid name. Merge cancelled.");
            return;
        }

        name = name.trim();

        // Ensure cars/ directory exists
        File carsDir = new File("cars");
        if (!carsDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            carsDir.mkdirs();
        }

        mergedFilePath = "cars/" + name + ".rad";

        try {
            mergeFiles(filePanel1.getFile(), filePanel2.getFile(), filePanel3.getFile(), mergedFilePath);

            statusLabel.setText("Merging complete. Saved as: " + mergedFilePath);
            JOptionPane.showMessageDialog(this,
                    "Merging complete!\nSaved as: " + mergedFilePath,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Update 3D viewer
            
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();

            // Load into editor
            loadRadFileText(mergedFilePath);
            detectSchemes(new String(Files.readAllBytes(Paths.get(mergedFilePath))));
            refreshDropdown();
            refreshWheelDropdown();     // wheels
            tabbedPane.setSelectedIndex(1);

        } catch (IOException ex) {
            statusLabel.setText("Error during merge: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error during merge:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mergeFiles(File file1, File file2, File file3, String outputFileName) throws IOException {
        List<String> file1Lines = Files.readAllLines(Paths.get(file1.getAbsolutePath()));
        List<String> file2Lines = Files.readAllLines(Paths.get(file2.getAbsolutePath()));
        List<String> file3Lines = Files.readAllLines(Paths.get(file3.getAbsolutePath()));

        try (PrintWriter out = new PrintWriter(new FileWriter(outputFileName))) {
            for (int i = 0; i < file1Lines.size(); i++) {
                String line = file1Lines.get(i);
                if (line.trim().startsWith("c(")) {
                    out.println(line);
                    out.println(replaceColor(file2Lines.get(i), "c1"));
                    out.println(replaceColor(file3Lines.get(i), "c2"));
                } else {
                    out.println(line);
                }
            }
        }
    }

    private String replaceColor(String line, String prefix) {
        if (line.trim().startsWith("c(")) {
            return prefix + line.substring(1);
        }
        return line;
    }

    private void loadRadFileText(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            String content = String.join(System.lineSeparator(), lines);
            radTextArea.setText(content);
            radTextArea.setCaretPosition(0);
        } catch (IOException e) {
            radTextArea.setText("Error reading file:\n" + e.getMessage());
        }
    }
    

    private void refreshDropdown() {
        carDropdown.removeAllItems();
        carDropdown.addItem("Select a Car");

        if (!carsFolder.exists()) carsFolder.mkdirs();

        File[] list = carsFolder.listFiles(
            (d, name) -> name.toLowerCase().endsWith(".rad")
        );

        if (list != null) {
            for (File f : list) {
                carDropdown.addItem(f.getName().replace(".rad", ""));
            }
        }

        // Handle selection
        carDropdown.addActionListener(e -> {
            Object selected = carDropdown.getSelectedItem();
            if (selected == null) return;

            String name = selected.toString();
            if (name.equals("Select a Car")) return;

            File f = new File(carsFolder, name + ".rad");
            if (!f.exists()) return;

            mergedFilePath = f.getAbsolutePath();

            try {
                viewer.loadRadFile(mergedFilePath);
                viewerContainer.revalidate();
                viewerContainer.repaint();

                loadRadFileText(mergedFilePath);
                try {
                    String content = new String(Files.readAllBytes(Paths.get(mergedFilePath)));
                    detectSchemes(content);
                } catch (Exception ex) { /* silent */ }
                tabbedPane.setSelectedIndex(1);

                statusLabel.setText("Loaded: " + f.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error loading file:\n" + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void refreshWheelDropdown() {
        wheelDropdown.removeAllItems();
        wheelDropdown.addItem("Select Wheel");

        if (!wheelsFolder.exists()) wheelsFolder.mkdirs();

        File[] wheels = wheelsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".rad"));
        if (wheels == null) return;

        for (File f : wheels) {
            wheelDropdown.addItem(f.getName().replace(".rad", ""));
        }

        wheelDropdown.addActionListener(e -> {
            Object selected = wheelDropdown.getSelectedItem();
            if (selected == null) return;

            String wheelName = selected.toString();
            if (wheelName.equals("Select Wheel")) return;

            File wfile = new File(wheelsFolder, wheelName + ".rad");
            if (!wfile.exists()) return;

            try {
                wheelViewer.loadWheelFile(wfile.getAbsolutePath());   // NEW dedicated wheel loader
                wheelViewerContainer.revalidate();
                wheelViewerContainer.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error loading wheel:\n" + ex.getMessage(),
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void applyWheelsToSelectedCar() {
        // Get selected car
        Object selectedCar = carDropdown.getSelectedItem();
        if (selectedCar == null || selectedCar.toString().equals("Select a Car")) {
            JOptionPane.showMessageDialog(this, "Please select a car first.", "No Car Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get selected wheel
        Object selectedWheel = wheelDropdown.getSelectedItem();
        if (selectedWheel == null || selectedWheel.toString().equals("Select Wheel")) {
            JOptionPane.showMessageDialog(this, "Please select a wheel model first.", "No Wheel Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String carName = selectedCar.toString();
        String wheelName = selectedWheel.toString();
        
        File carFile = new File(carsFolder, carName + ".rad");
        File wheelFile = new File(wheelsFolder, wheelName + ".rad");
        
        try {
            // Read car file
            String carContent = new String(Files.readAllBytes(carFile.toPath()));
            
            // Count how many wheels the car has
            int wheelCount = countWheels(carContent);
            
            // Ask user: Replace all or custom selection?
            String[] options = {"Replace All Wheels", "Custom Selection", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                this,
                "How would you like to apply the wheels?",
                "Wheel Application",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return; // Cancel
            
            boolean replaceAll = (choice == 0);
            boolean[] selectedWheels = null;
            
            if (!replaceAll) {
                // Show wheel selection dialog
                selectedWheels = showWheelSelectionDialog(wheelCount);
                if (selectedWheels == null) return; // User cancelled
                
                // Check if any wheels are selected
                boolean anySelected = false;
                for (boolean selected : selectedWheels) {
                    if (selected) {
                        anySelected = true;
                        break;
                    }
                }
                if (!anySelected) {
                    JOptionPane.showMessageDialog(this, "No wheels selected!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // Read wheel file
            String wheelContent = new String(Files.readAllBytes(wheelFile.toPath()));

            int schemeCount = 1;
            if (carContent.contains("c1(")) schemeCount = 2;
            if (carContent.contains("c2(")) schemeCount = 3;
            if (carContent.contains("c3(")) schemeCount = 4;
            
            // Extract wheel model from wheel file
            String wheelModel = extractWheelModel(wheelContent, schemeCount);
            if (wheelModel == null) {
                JOptionPane.showMessageDialog(this, "No wheel model found in selected wheel file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (replaceAll) {
                // Original behavior: replace all wheels
                if (carContent.contains("<wheelModel(")) {
                    int result = JOptionPane.showConfirmDialog(this, 
                        "This car already has custom wheels. Replace them?", 
                        "Replace Wheels?", 
                        JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) return;
                    
                    carContent = removeAllWheelModels(carContent);
                }
                
                carContent = convertWheelCallsTo7Args(carContent);
                carContent = appendWheelModel(carContent, wheelModel);
                
            } else {
                // Custom selection: check if selected wheels already have model IDs
                int existingModelId = getExistingModelIdForWheels(carContent, selectedWheels);
                
                if (existingModelId >= 0) {
                    // Replace existing wheel model
                    carContent = replaceWheelModel(carContent, existingModelId, wheelModel);
                } else {
                    // Create new wheel model
                    int nextModelId = getNextWheelModelId(carContent);
                    
                    // Update the wheel model to use the new ID
                    wheelModel = wheelModel.replace("<wheelModel(0)>", "<wheelModel(" + nextModelId + ")>");
                    
                    // Convert selected wheels to use the new model ID
                    carContent = convertSelectedWheelsTo7Args(carContent, selectedWheels, nextModelId);
                    
                    // Append the new wheel model
                    carContent = appendWheelModel(carContent, wheelModel);
                }
            }
            
            // Save modified car file
            Files.write(carFile.toPath(), carContent.getBytes());
            
            // Reload the car in viewer
            viewer.loadRadFile(carFile.getAbsolutePath());
            loadRadFileText(carFile.getAbsolutePath());
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
            statusLabel.setText("Wheels applied to " + carName);
            JOptionPane.showMessageDialog(this, "Wheels successfully applied!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error applying wheels:\n" + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private int getExistingModelIdForWheels(String content, boolean[] selectedWheels) {
        String[] lines = content.split("\n");
        int wheelIndex = 0;
        boolean insideWheelModel = false;
        int foundModelId = -1;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.startsWith("<wheelModel(")) {
                insideWheelModel = true;
            } else if (trimmed.startsWith("</wheelModel>")) {
                insideWheelModel = false;
            }
            
            if (!insideWheelModel && trimmed.startsWith("w(")) {
                if (wheelIndex < selectedWheels.length && selectedWheels[wheelIndex]) {
                    // Check if this wheel has a model ID
                    int commaCount = 0;
                    for (char c : trimmed.toCharArray()) {
                        if (c == ',') commaCount++;
                    }
                    
                    if (commaCount == 6) {
                        // 7-arg format, get the model ID
                        try {
                            int start = trimmed.indexOf('(') + 1;
                            int end = trimmed.indexOf(')');
                            String params = trimmed.substring(start, end);
                            String[] values = params.split(",");
                            int modelId = Integer.parseInt(values[6].trim());
                            
                            if (foundModelId == -1) {
                                foundModelId = modelId;
                            } else if (foundModelId != modelId) {
                                // Multiple different model IDs selected - return -1 to create new
                                return -1;
                            }
                        } catch (Exception e) {
                            // Parse error
                        }
                    }
                }
                wheelIndex++;
            }
        }
        
        return foundModelId;
    }

    private String replaceWheelModel(String content, int modelId, String newWheelModel) {
        // Find and replace the wheelModel block with this ID
        String searchTag = "<wheelModel(" + modelId + ")>";
        int start = content.indexOf(searchTag);
        
        if (start == -1) return content; // Model not found
        
        int end = content.indexOf("</wheelModel>", start);
        if (end == -1) return content;
        
        // Replace with new wheel model (update ID in new model)
        newWheelModel = newWheelModel.replace("<wheelModel(0)>", searchTag);
        
        String before = content.substring(0, start);
        String after = content.substring(end + "</wheelModel>".length());
        
        return before + newWheelModel + after;
    }

    private String extractWheelModel(String content, int schemeCount) {
        StringBuilder wheelModel = new StringBuilder();
        wheelModel.append("<wheelModel(0)>\n\n");
        
        int searchStart = 0;
        boolean foundAnyPolygon = false;
        
        while (true) {
            int pStart = content.indexOf("<p>", searchStart);
            if (pStart == -1) break;
            
            int pEnd = content.indexOf("</p>", pStart);
            if (pEnd == -1) break;
            
            String polygonBlock = content.substring(pStart, pEnd + "</p>".length());
            
            // Run addColorSchemes on it, then convert to wheel poly format
            polygonBlock = addColorSchemes(polygonBlock, schemeCount);
            
            polygonBlock = polygonBlock.replace("<p>", "[p]");
            polygonBlock = polygonBlock.replace("</p>", "[/p]");
            
            wheelModel.append(polygonBlock).append("\n\n");
            
            foundAnyPolygon = true;
            searchStart = pEnd + "</p>".length();
        }
        
        wheelModel.append("</wheelModel>");
        
        if (!foundAnyPolygon) {
            return null;
        }
        
        return wheelModel.toString();
    }
    private int countWheels(String content) {
        int count = 0;
        String[] lines = content.split("\n");
        boolean insideWheelModel = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.startsWith("<wheelModel(")) {
                insideWheelModel = true;
            } else if (trimmed.startsWith("</wheelModel>")) {
                insideWheelModel = false;
            }
            
            if (!insideWheelModel && trimmed.startsWith("w(")) {
                count++;
            }
        }
        
        return count;
    }

    private boolean[] showWheelSelectionDialog(int wheelCount) {
        // Group wheels by axle (pairs based on position)
        int axleCount = wheelCount / 2;
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel instruction = new JLabel("Select which axles to replace:");
        panel.add(instruction);
        panel.add(Box.createVerticalStrut(10));
        
        JCheckBox[] axleCheckboxes = new JCheckBox[axleCount];
        String[] axleNames = {"Front Axle", "Middle Axle 1", "Middle Axle 2", "Rear Axle"};
        
        for (int i = 0; i < axleCount; i++) {
            String name;
            if (i == 0) {
                name = "Front Axle";
            } else if (i == axleCount - 1) {
                name = "Rear Axle";
            } else {
                name = "Middle Axle " + i;
            }
            
            axleCheckboxes[i] = new JCheckBox(name);
            panel.add(axleCheckboxes[i]);
        }
        
        int result = JOptionPane.showConfirmDialog(
            this, 
            panel, 
            "Select Axles", 
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result != JOptionPane.OK_OPTION) return null;
        
        // Convert axle selection to wheel selection
        // Each axle has 2 wheels (left and right)
        boolean[] selectedWheels = new boolean[wheelCount];
        for (int i = 0; i < axleCount; i++) {
            if (axleCheckboxes[i].isSelected()) {
                selectedWheels[i * 2] = true;      // Left wheel
                selectedWheels[i * 2 + 1] = true;  // Right wheel
            }
        }
        
        return selectedWheels;
    }

    private int getNextWheelModelId(String content) {
        int maxId = -1;
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<wheelModel(")) {
                try {
                    int start = trimmed.indexOf('(') + 1;
                    int end = trimmed.indexOf(')');
                    int id = Integer.parseInt(trimmed.substring(start, end).trim());
                    if (id > maxId) maxId = id;
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        
        return maxId + 1;
    }

    private String convertSelectedWheelsTo7Args(String content, boolean[] selectedWheels, int modelId) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int wheelIndex = 0;
        boolean insideWheelModel = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.startsWith("<wheelModel(")) {
                insideWheelModel = true;
            } else if (trimmed.startsWith("</wheelModel>")) {
                insideWheelModel = false;
            }
            
            if (!insideWheelModel && trimmed.startsWith("w(")) {
                // Check if this wheel is selected
                if (wheelIndex < selectedWheels.length && selectedWheels[wheelIndex]) {
                    // Convert this wheel to 7-arg with the new model ID
                    int commaCount = 0;
                    for (char c : trimmed.toCharArray()) {
                        if (c == ',') commaCount++;
                    }
                    
                    if (commaCount == 5) {
                        // 6-arg format, convert to 7-arg
                        int start = trimmed.indexOf('(') + 1;
                        int end = trimmed.indexOf(')');
                        String params = trimmed.substring(start, end);
                        String[] values = params.split(",");
                        
                        int x = Integer.parseInt(values[0].trim());
                        int width = Integer.parseInt(values[4].trim());
                        
                        if (x < 0) {
                            values[4] = String.valueOf(Math.abs(width));
                        } else {
                            values[4] = String.valueOf(-Math.abs(width));
                        }
                        
                        String indent = line.substring(0, line.indexOf('w'));
                        String newLine = indent + "w(" + String.join(",", values) + "," + modelId + ")";
                        result.append(newLine).append("\n");
                        wheelIndex++;
                        continue;
                    } else if (commaCount == 6) {
                        // Already 7-arg, just update the model ID
                        int start = trimmed.indexOf('(') + 1;
                        int end = trimmed.indexOf(')');
                        String params = trimmed.substring(start, end);
                        String[] values = params.split(",");
                        
                        values[6] = String.valueOf(modelId);
                        
                        String indent = line.substring(0, line.indexOf('w'));
                        String newLine = indent + "w(" + String.join(",", values) + ")";
                        result.append(newLine).append("\n");
                        wheelIndex++;
                        continue;
                    }
                }
                
                // Not selected or couldn't process - keep as-is
                wheelIndex++;
            }
            
            result.append(line).append("\n");
        }
        
        return result.toString();
    }

    private String addColorSchemesToPolygon(String polygonBlock) {
        // Add c1() and c2() after c() if they don't exist
        String[] lines = polygonBlock.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Found c() color definition
            if (trimmed.startsWith("c(") && !trimmed.startsWith("c1(") && !trimmed.startsWith("c2(")) {
                result.append(line).append("\n");
                
                // Add c1() and c2() with same color
                String c1Line = trimmed.replace("c(", "c1(");
                String c2Line = trimmed.replace("c(", "c2(");
                
                // Preserve indentation
                String indent = line.substring(0, line.indexOf('c'));
                result.append(indent).append(c1Line).append("\n");
                result.append(indent).append(c2Line).append("\n");
                
                continue;
            }
            
            result.append(line).append("\n");
        }
        
        return result.toString();
    }

    private String removeAllWheelModels(String content) {
        while (content.contains("<wheelModel(")) {
            int start = content.indexOf("<wheelModel(");
            int end = content.indexOf("</wheelModel>", start);
            if (end == -1) break;
            
            content = content.substring(0, start) + content.substring(end + "</wheelModel>".length());
        }
        return content;
    }

    private void removeCustomWheels() {
        Object selectedCar = carDropdown.getSelectedItem();
        if (selectedCar == null || selectedCar.toString().equals("Select a Car")) {
            JOptionPane.showMessageDialog(this, "Please select a car first.", "No Car Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String carName = selectedCar.toString();
        File carFile = new File(carsFolder, carName + ".rad");
        
        try {
            String content = new String(Files.readAllBytes(carFile.toPath()));
            
            // Check if car has custom wheels
            if (!content.contains("<wheelModel(")) {
                JOptionPane.showMessageDialog(this, 
                    "This car doesn't have custom wheels.", 
                    "No Custom Wheels", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int result = JOptionPane.showConfirmDialog(this,
                "Remove custom wheels and revert to stock wheels?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);
            
            if (result != JOptionPane.YES_OPTION) return;
            
            // Remove all wheelModel blocks
            content = removeAllWheelModels(content);
            
            // Convert 7-arg w() calls back to 6-arg
            content = convertWheelCallsTo6Args(content);
            
            // Save modified car file
            Files.write(carFile.toPath(), content.getBytes());
            
            // Update text editor
            radTextArea.setText(content);
            
            // Reload the car in viewer
            viewer.loadRadFile(carFile.getAbsolutePath());
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
            statusLabel.setText("Custom wheels removed from " + carName);
            JOptionPane.showMessageDialog(this, 
                "Custom wheels removed! Car now uses stock wheels.", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error removing custom wheels:\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private String convertWheelCallsTo6Args(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("w(")) {
                // Count commas
                int commaCount = 0;
                for (char c : trimmed.toCharArray()) {
                    if (c == ',') commaCount++;
                }
                
                // If 7 args (6 commas), convert to 6 args
                if (commaCount == 6) {
                    // Parse and remove the last argument (modelId)
                    int start = trimmed.indexOf('(') + 1;
                    int end = trimmed.indexOf(')');
                    String params = trimmed.substring(start, end);
                    String[] values = params.split(",");
                    
                    // Get X position and width
                    int x = Integer.parseInt(values[0].trim());
                    int width = Integer.parseInt(values[4].trim());
                    
                    // For stock wheels (6 args):
                    // - Left wheels (X < 0): positive width
                    // - Right wheels (X > 0): negative width
                    if (x < 0) {
                        values[4] = String.valueOf(Math.abs(width));
                    } else {
                        values[4] = String.valueOf(-Math.abs(width));
                    }
                    
                    // Only keep first 6 values
                    String[] first6 = new String[6];
                    System.arraycopy(values, 0, first6, 0, 6);
                    
                    // Reconstruct without modelId
                    String indent = line.substring(0, line.indexOf('w'));
                    String newLine = indent + "w(" + String.join(",", first6) + ")";
                    result.append(newLine).append("\n");
                } else {
                    // Already 6 args or something else, keep as-is
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
    }

    private String convertWheelCallsTo7Args(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("w(")) {
                int commaCount = 0;
                for (char c : trimmed.toCharArray()) {
                    if (c == ',') commaCount++;
                }
                
                if (commaCount == 5) {
                    // Parse the wheel parameters
                    int start = trimmed.indexOf('(') + 1;
                    int end = trimmed.indexOf(')');
                    String params = trimmed.substring(start, end);
                    String[] values = params.split(",");
                    
                    // Get X position and width
                    int x = Integer.parseInt(values[0].trim());
                    int width = Integer.parseInt(values[4].trim());
                    
                    // For custom wheels (7 args):
                    // - Left wheels (negative X): positive width
                    // - Right wheels (positive X): negative width
                    if (x < 0) {
                        values[4] = String.valueOf(Math.abs(width));
                    } else {
                        values[4] = String.valueOf(-Math.abs(width));
                    }
                    
                    // Reconstruct the line and add wheelModel ID (0)
                    String indent = line.substring(0, line.indexOf('w'));
                    String newLine = indent + "w(" + String.join(",", values) + ",0)";
                    result.append(newLine).append("\n");
                } else {
                    // Already 7 args, keep as-is
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
    }

    private String appendWheelModel(String content, String wheelModel) {
        // Find the LAST </p> tag - wheel model goes after this
        int lastPTag = content.lastIndexOf("</p>");
        if (lastPTag == -1) {
            // No </p> found, try to find first gwgr( or w(
            int firstWheelCall = content.indexOf("w(");
            int firstGwgr = content.indexOf("gwgr(");
            int insertPos = -1;
            
            if (firstGwgr != -1 && (firstWheelCall == -1 || firstGwgr < firstWheelCall)) {
                insertPos = firstGwgr;
            } else if (firstWheelCall != -1) {
                insertPos = firstWheelCall;
            } else {
                return content + "\n\n" + wheelModel + "\n";
            }
            
            int lineStart = content.lastIndexOf("\n", insertPos);
            if (lineStart == -1) lineStart = 0;
            else lineStart++;
            
            return content.substring(0, lineStart) + "\n" + wheelModel + "\n\n" + content.substring(lineStart);
        }
        
        // Found </p>, insert wheel model after it
        int insertPos = lastPTag + "</p>".length();
        
        // Move to the end of the line (after any newline)
        if (insertPos < content.length() && content.charAt(insertPos) == '\n') {
            insertPos++;
        }
        
        // Insert wheel model here
        return content.substring(0, insertPos) + "\n" + wheelModel + "\n\n" + content.substring(insertPos);
    }

    // 2. ADD THESE METHODS at the end of the class with the other helper methods

    private void createNewCar() {
        String carName = JOptionPane.showInputDialog(
            this,
            "Enter name for the new car (without .rad):",
            "Create New Car",
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (carName == null || carName.trim().isEmpty()) {
            return;
        }
        
        carName = carName.trim();
        File newCarFile = new File(carsFolder, carName + ".rad");
        
        // Check if file already exists
        if (newCarFile.exists()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "A car with this name already exists. Overwrite?",
                "File Exists",
                JOptionPane.YES_NO_OPTION
            );
            if (result != JOptionPane.YES_OPTION) return;
        }
        
        // Create a basic empty car template
        String template = "// New Car: " + carName + "\n" +
                        "// Created with RAD File Merger\n\n" +
                        "shadow()\n\n" +
                        "1stColor(140,16,16)\n" +
                        "2ndColor(210,210,210)\n\n" +
                        "// Add your car body polygons here\n\n" +
                        "<p>\n" +
                        "c(140,16,16)\n" +
                        "c1(141,141,141)\n" +
                        "c2(52,52,52)\n\n" +
                        "p(0,0,50)\n" +
                        "p(30,0,0)\n" +
                        "p(0,0,-50)\n" +
                        "p(-30,0,0)\n" +
                        "</p>\n\n" +
                        "// Wheel definitions\n" +
                        "gwgr(40)\n" +
                        "w(-30,10,30,11,10,15)\n" +
                        "w(30,10,30,11,10,15)\n\n" +
                        "gwgr(40)\n" +
                        "w(-30,10,-30,0,10,15)\n" +
                        "w(30,10,-30,0,10,15)\n";
        
        try {
            Files.write(newCarFile.toPath(), template.getBytes());
            
            JOptionPane.showMessageDialog(
                this,
                "New car created: " + carName + ".rad",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            // Refresh dropdown and select the new car
            refreshDropdown();
            carDropdown.setSelectedItem(carName);
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error creating car:\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteSelectedCar() {
        Object selected = carDropdown.getSelectedItem();
        if (selected == null || selected.toString().equals("Select a Car")) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a car to delete.",
                "No Car Selected",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        String carName = selected.toString();
        
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete " + carName + ".rad?\nThis cannot be undone!",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION) return;
        
        File carFile = new File(carsFolder, carName + ".rad");
        
        if (carFile.delete()) {
            JOptionPane.showMessageDialog(
                this,
                "Car deleted: " + carName + ".rad",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            // Clear viewer and editor
            radTextArea.setText("");
            mergedFilePath = null;
            
            // Refresh dropdown
            refreshDropdown();
            carDropdown.setSelectedIndex(0); // Select "Select a Car"
            
            statusLabel.setText("Car deleted: " + carName);
            
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to delete car file.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    

    private void importCarCode() {
        String carName = JOptionPane.showInputDialog(
            this,
            "Enter name for the imported car (without .rad):",
            "Import Car Code",
            JOptionPane.PLAIN_MESSAGE
        );

        if (carName == null || carName.trim().isEmpty()) return;

        carName = carName.trim();
        File newCarFile = new File(carsFolder, carName + ".rad");

        if (newCarFile.exists()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "A car with this name already exists. Overwrite?",
                "File Exists",
                JOptionPane.YES_NO_OPTION
            );
            if (result != JOptionPane.YES_OPTION) return;
        }

        // Ask how many color schemes to generate
        String[] schemeOptions = {"None (or exists in code)", "2 Color Schemes", "3 Color Schemes ", "4 Color Schemes"};
        int schemeChoice = JOptionPane.showOptionDialog(
            this,
            "How many color schemes do you want to generate?",
            "Color Schemes",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, schemeOptions, schemeOptions[1]
        );

        if (schemeChoice == JOptionPane.CLOSED_OPTION) return;
        int schemeCount = schemeChoice + 1; // 1=none, 2=c+c1, 3=c+c1+c2, 4=c+c1+c2+c3

        // Show text area for pasting code
        JTextArea importArea = new JTextArea(20, 50);
        importArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(importArea);

        int result = JOptionPane.showConfirmDialog(
            this,
            scrollPane,
            "Paste car code here:",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        String carCode = importArea.getText().trim();

        if (carCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No code was pasted!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (schemeCount > 1) {
            carCode = addColorSchemes(carCode, schemeCount);
        }

        try {
            Files.write(newCarFile.toPath(), carCode.getBytes());

            JOptionPane.showMessageDialog(
                this,
                "Car imported successfully: " + carName + ".rad" +
                (schemeCount > 1 ? "\nWith " + schemeCount + " color schemes generated!" : ""),
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            refreshDropdown();
            carDropdown.setSelectedItem(carName);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error importing car:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importWheelCode() {
        String wheelName = JOptionPane.showInputDialog(
            this,
            "Enter name for the imported wheel (without .rad):",
            "Import Wheel Code",
            JOptionPane.PLAIN_MESSAGE
        );

        if (wheelName == null || wheelName.trim().isEmpty()) return;

        wheelName = wheelName.trim();
        File wheelsFolder = new File("wheels");
        if (!wheelsFolder.exists()) wheelsFolder.mkdirs();
        File newWheelFile = new File(wheelsFolder, wheelName + ".rad");

        if (newWheelFile.exists()) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "A wheel with this name already exists. Overwrite?",
                "File Exists",
                JOptionPane.YES_NO_OPTION
            );
            if (result != JOptionPane.YES_OPTION) return;
        }

        String[] schemeOptions = {"None", "2 (Original + Scheme 1)", "3 (+ Scheme 2)", "4 (+ Scheme 3)"};
        int schemeChoice = JOptionPane.showOptionDialog(
            this,
            "How many color schemes do you want to generate?",
            "Color Schemes",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, schemeOptions, schemeOptions[1]
        );

        if (schemeChoice == JOptionPane.CLOSED_OPTION) return;
        int schemeCount = schemeChoice + 1;
        boolean generateColorSchemes = schemeCount > 1;

        JTextArea importArea = new JTextArea(20, 50);
        importArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(importArea);

        int result = JOptionPane.showConfirmDialog(
            this,
            scrollPane,
            "Paste wheel code here:",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        String wheelCode = importArea.getText().trim();

        if (wheelCode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No code was pasted!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (generateColorSchemes) {
            wheelCode = addColorSchemes(wheelCode, schemeCount);
        }

        try {
            Files.write(newWheelFile.toPath(), wheelCode.getBytes());

            JOptionPane.showMessageDialog(
                this,
                "Wheel imported successfully: " + wheelName + ".rad" +
                (generateColorSchemes ? "\nWith " + schemeCount + " color schemes generated!" : ""),
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );

            refreshWheelDropdown();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error importing wheel:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String addColorSchemes(String carCode) {
        // Add c1() and c2() tags to all polygons that only have c()
        String[] lines = carCode.split("\n");
        StringBuilder result = new StringBuilder();
        
        boolean inPoly = false;
        String lastCColor = null;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.startsWith("<p>")) {
                inPoly = true;
                lastCColor = null;
            }
            
            if (inPoly) {
                // Found c() color definition
                if (trimmed.startsWith("c(")) {
                    lastCColor = trimmed;
                    result.append(line).append("\n");
                    
                    // Check if next lines already have c1() or c2()
                    // For now, we'll add them right after c()
                    String c1Line = lastCColor.replace("c(", "c1(");
                    String c2Line = lastCColor.replace("c(", "c2(");
                    
                    // Add with same indentation as c() line
                    String indent = line.substring(0, line.indexOf('c'));
                    result.append(indent).append(c1Line).append("\n");
                    result.append(indent).append(c2Line).append("\n");
                    
                    continue;
                }
                
                // Skip existing c1() and c2() lines to avoid duplicates
                if (trimmed.startsWith("c1(") || trimmed.startsWith("c2(")) {
                    continue;
                }
            }
            
            if (trimmed.startsWith("</p>")) {
                inPoly = false;
            }
            
            result.append(line).append("\n");
        }
        
        return result.toString();
    }

    public void updateWheelAnchorsInFile() {
        if (mergedFilePath == null) {
            JOptionPane.showMessageDialog(this, 
                "No file loaded to save changes to.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String content = radTextArea.getText();
            
            // Find and replace each w(...) line with updated values
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            int wheelIndex = 0;
            boolean insideWheelModel = false;
            
            for (String line : lines) {
                String trimmed = line.trim();
                
                // Track if we're inside a wheelModel block
                if (trimmed.startsWith("<wheelModel(")) {
                    insideWheelModel = true;
                } else if (trimmed.startsWith("</wheelModel>")) {
                    insideWheelModel = false;
                }
                
                // Only process w(...) lines that are OUTSIDE wheelModel blocks
                if (trimmed.startsWith("w(") && !insideWheelModel) {
                    // Parse the original rotation value from the line
                    int start = trimmed.indexOf('(') + 1;
                    int end = trimmed.indexOf(')');
                    String params = trimmed.substring(start, end);
                    String[] values = params.split(",");
                    
                    // Get rotation from original line (index 3)
                    int originalRotation = Integer.parseInt(values[3].trim());
                    
                    // Determine if this is a 6-arg or 7-arg wheel call
                    boolean hasModelId = (values.length >= 7);
                    
                    // Get values from the wheel anchor editor
                    WheelAnchorEditor.WheelData wheelData = wheelAnchorEditor.getWheelData(wheelIndex);
                    
                    if (wheelData != null) {
                        int width = wheelData.width;
                        
                        // For right-side wheels (positive X), make width negative
                        if (wheelData.x > 0) {
                            width = -Math.abs(width);
                        } else {
                            width = Math.abs(width);
                        }
                        
                        String newLine;
                        if (hasModelId) {
                            // 7-arg format (custom wheels)
                            newLine = String.format("w(%d,%d,%d,%d,%d,%d,%d)",
                                wheelData.x, wheelData.y, wheelData.z, originalRotation, 
                                width, wheelData.height, wheelData.modelId);
                        } else {
                            // 6-arg format (stock wheels)
                            newLine = String.format("w(%d,%d,%d,%d,%d,%d)",
                                wheelData.x, wheelData.y, wheelData.z, originalRotation, 
                                width, wheelData.height);
                        }
                        
                        // Preserve the original indentation
                        String indent = line.substring(0, line.indexOf('w'));
                        result.append(indent).append(newLine).append("\n");
                    } else {
                        result.append(line).append("\n");
                    }
                    
                    wheelIndex++;
                } else {
                    result.append(line).append("\n");
                }
            }
            
            // Update the text area
            radTextArea.setText(result.toString());
            
            // Save to file
            Files.write(Paths.get(mergedFilePath), result.toString().getBytes());
            
            // RELOAD THE VIEWER with the updated file
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error updating wheel anchors:\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    public void saveColorsToFile(Color color1, Color color2, Color color3, Color color4, Color color5, Color color6) {
        if (mergedFilePath == null) {
            JOptionPane.showMessageDialog(this, "No file loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String content = radTextArea.getText();
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            
            // Find OLD color values
            Color old1st = null, old2nd = null, old3rd = null, old4th = null, old5th = null, old6th = null;
            
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("1stColor(")) old1st = parseColorFromLine(trimmed);
                else if (trimmed.startsWith("2ndColor(")) old2nd = parseColorFromLine(trimmed);
                else if (trimmed.startsWith("3rdColor(")) old3rd = parseColorFromLine(trimmed);
                else if (trimmed.startsWith("4thColor(")) old4th = parseColorFromLine(trimmed);
                else if (trimmed.startsWith("5thColor(")) old5th = parseColorFromLine(trimmed);
                else if (trimmed.startsWith("6thColor(")) old6th = parseColorFromLine(trimmed);
            }
            
            // Get current scheme from color editor
            int currentScheme = colorPaletteEditor.getCurrentScheme();
            
            // Process all lines
            for (String line : lines) {
                String trimmed = line.trim();
                
                // Update color definitions
                if (trimmed.startsWith("1stColor(")) {
                    result.append(formatColorLine(line, "1stColor", color1));
                    continue;
                }
                if (trimmed.startsWith("2ndColor(")) {
                    result.append(formatColorLine(line, "2ndColor", color2));
                    continue;
                }
                if (trimmed.startsWith("3rdColor(")) {
                    result.append(formatColorLine(line, "3rdColor", color3));
                    continue;
                }
                if (trimmed.startsWith("4thColor(")) {
                    result.append(formatColorLine(line, "4thColor", color4));
                    continue;
                }
                if (trimmed.startsWith("5thColor(")) {
                    result.append(formatColorLine(line, "5thColor", color5));
                    continue;
                }
                if (trimmed.startsWith("6thColor(")) {
                    result.append(formatColorLine(line, "6thColor", color6));
                    continue;
                }
                
                // Update ONLY the tags for the current scheme
                if (currentScheme == 0) {
                    // Original scheme - only update c() tags
                    if (trimmed.startsWith("c(")) {
                        Color current = parseColorFromLine(trimmed);
                        Color newColor = null;
                        
                        if (old1st != null && colorsMatch(current, old1st)) {
                            newColor = color1;
                        } else if (old2nd != null && colorsMatch(current, old2nd)) {
                            newColor = color2;
                        }
                        
                        if (newColor != null) {
                            String indent = line.substring(0, line.indexOf('c'));
                            result.append(indent).append(String.format("c(%d,%d,%d)\n", 
                                newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                        } else {
                            result.append(line).append("\n");
                        }
                        continue;
                    }
                } else if (currentScheme == 1) {
                    // Scheme 1 - only update c1() tags
                    if (trimmed.startsWith("c1(")) {
                        Color current = parseColorFromLine(trimmed);
                        Color newColor = null;
                        
                        if (old3rd != null && colorsMatch(current, old3rd)) {
                            newColor = color3;
                        } else if (old4th != null && colorsMatch(current, old4th)) {
                            newColor = color4;
                        }
                        
                        if (newColor != null) {
                            String indent = line.substring(0, line.indexOf('c'));
                            result.append(indent).append(String.format("c1(%d,%d,%d)\n", 
                                newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                        } else {
                            result.append(line).append("\n");
                        }
                        continue;
                    }
                } else if (currentScheme == 2) {
                    // Scheme 2 - only update c2() tags
                    if (trimmed.startsWith("c2(")) {
                        Color current = parseColorFromLine(trimmed);
                        Color newColor = null;
                        
                        if (old5th != null && colorsMatch(current, old5th)) {
                            newColor = color5;
                        } else if (old6th != null && colorsMatch(current, old6th)) {
                            newColor = color6;
                        }
                        
                        if (newColor != null) {
                            String indent = line.substring(0, line.indexOf('c'));
                            result.append(indent).append(String.format("c2(%d,%d,%d)\n", 
                                newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                        } else {
                            result.append(line).append("\n");
                        }
                        continue;
                    }
                }
                
                result.append(line).append("\n");
            }
            
            // Save and reload
            radTextArea.setText(result.toString());
            Files.write(Paths.get(mergedFilePath), result.toString().getBytes());
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
            JOptionPane.showMessageDialog(this, "Colors saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private String formatColorLine(String line, String colorName, Color color) {
        String indent = line.substring(0, line.indexOf(colorName.charAt(0)));
        return indent + String.format("%s(%d,%d,%d)\n", colorName, color.getRed(), color.getGreen(), color.getBlue());
    }

    private Color getReplacementColor(Color current, Color... oldNewPairs) {
        for (int i = 0; i < oldNewPairs.length; i += 2) {
            Color oldColor = oldNewPairs[i];
            Color newColor = oldNewPairs[i + 1];
            if (oldColor != null && colorsMatch(current, oldColor)) {
                return newColor;
            }
        }
        return null;
    }

    private Color parseColorFromLine(String line) {
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
            return Color.BLACK;
        }
    }

    private boolean colorsMatch(Color c1, Color c2) {
        if (c1 == null || c2 == null) return false;
        return c1.getRed() == c2.getRed() && 
            c1.getGreen() == c2.getGreen() && 
            c1.getBlue() == c2.getBlue();
    }

    public String getRadTextContent() {
        return radTextArea.getText();
    }

    public void replaceColorInFile(Color oldColor, Color newColor, int scheme) {
        System.out.println("replaceColorInFile called: scheme=" + scheme + " old=" + oldColor + " new=" + newColor);
        if (mergedFilePath == null) return;
        
        try {
            String content = radTextArea.getText();
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            
            String colorTag = "";
            switch (scheme) {
                case 0: colorTag = "c("; break;
                case 1: colorTag = "c1("; break;
                case 2: colorTag = "c2("; break;
                case 3: colorTag = "c3("; break;
            }
            
            System.out.println("Looking for colorTag: " + colorTag);
            
            for (String line : lines) {
                String trimmed = line.trim();
                
                if (trimmed.startsWith(colorTag)) {
                    Color lineColor = parseColorFromLine(trimmed);
                    System.out.println("Found tag: " + trimmed + " lineColor=" + lineColor + " match=" + colorsMatch(lineColor, oldColor));
                    if (lineColor != null && colorsMatch(lineColor, oldColor)) {
                        String indent = line.substring(0, line.indexOf(colorTag.charAt(0)));
                        result.append(indent).append(String.format("%s%d,%d,%d)\n",
                            colorTag, newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                        continue;
                    }
                }
                
                result.append(line).append("\n");
            }
            
            radTextArea.setText(result.toString());
            Files.write(Paths.get(mergedFilePath), result.toString().getBytes());
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void goToPolygonInEditor(int polyIndex) {
        if (mergedFilePath == null) {
            JOptionPane.showMessageDialog(this, "No file loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String content = radTextArea.getText();
        String[] lines = content.split("\n");
        
        int currentPoly = -1;
        int targetLine = -1;
        boolean inPoly = false;
        
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            
            if (trimmed.startsWith("<p>")) {
                inPoly = true;
                currentPoly++;
                
                if (currentPoly == polyIndex) {
                    targetLine = i;
                    break;
                }
            }
            
            if (trimmed.startsWith("</p>")) {
                inPoly = false;
            }
        }
        
        if (targetLine != -1) {
            // Switch to editor tab
            tabbedPane.setSelectedIndex(1);
            
            // Calculate character position
            int charPos = 0;
            for (int i = 0; i < targetLine; i++) {
                charPos += lines[i].length() + 1; // +1 for newline
            }
            
            // Select the line
            radTextArea.setCaretPosition(charPos);
            radTextArea.requestFocus();
            
            // Scroll to make it visible
            try {
                radTextArea.scrollRectToVisible(radTextArea.modelToView(charPos));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            JOptionPane.showMessageDialog(this, 
                "Jumped to polygon " + polyIndex + " at line " + (targetLine + 1), 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Could not find polygon " + polyIndex + " in file.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updatePolygonColorInFile(int polyIndex, Color newColor, int scheme) {
        if (mergedFilePath == null) return;
        
        try {
            String content = radTextArea.getText();
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            
            String colorTag = "";
            switch (scheme) {
                case 0: colorTag = "c("; break;
                case 1: colorTag = "c1("; break;
                case 2: colorTag = "c2("; break;
                case 3: colorTag = "c3("; break;
            }
            
            int currentPoly = -1;
            boolean inPoly = false;
            boolean inWheelModel = false;
            
            for (String line : lines) {
                String trimmed = line.trim();
                
                // Track wheelModel blocks
                if (trimmed.startsWith("<wheelModel(")) {
                    inWheelModel = true;
                } else if (trimmed.startsWith("</wheelModel>")) {
                    inWheelModel = false;
                }
                
                // Only count body polygons (not wheel model polygons)
                if (!inWheelModel && trimmed.startsWith("<p>")) {
                    inPoly = true;
                    currentPoly++;
                }
                
                if (!inWheelModel && trimmed.startsWith("</p>")) {
                    inPoly = false;
                }
                
                // Update color if we're in the target polygon
                if (inPoly && currentPoly == polyIndex && trimmed.startsWith(colorTag)) {
                    String indent = line.substring(0, line.indexOf(colorTag.charAt(0)));
                    result.append(indent).append(String.format("%s%d,%d,%d)\n",
                        colorTag, newColor.getRed(), newColor.getGreen(), newColor.getBlue()));
                    continue;
                }
                
                result.append(line).append("\n");
            }
            
            radTextArea.setText(result.toString());
            Files.write(Paths.get(mergedFilePath), result.toString().getBytes());
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error updating color: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public void updatePolygonTranslationInFile(int polyIndex, int dx, int dy, int dz) {
        if (mergedFilePath == null) return;
        
        try {
            String content = radTextArea.getText();
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            
            int currentPoly = -1;
            boolean inPoly = false;
            boolean inWheelModel = false;
            
            for (String line : lines) {
                String trimmed = line.trim();
                
                // Track wheelModel blocks
                if (trimmed.startsWith("<wheelModel(")) {
                    inWheelModel = true;
                } else if (trimmed.startsWith("</wheelModel>")) {
                    inWheelModel = false;
                }
                
                // Only count body polygons
                if (!inWheelModel && trimmed.startsWith("<p>")) {
                    inPoly = true;
                    currentPoly++;
                }
                
                if (!inWheelModel && trimmed.startsWith("</p>")) {
                    inPoly = false;
                }
                
                // Update vertex positions if we're in the target polygon
                if (inPoly && currentPoly == polyIndex && trimmed.startsWith("p(")) {
                    try {
                        int start = trimmed.indexOf('(') + 1;
                        int end = trimmed.indexOf(')');
                        String params = trimmed.substring(start, end);
                        String[] values = params.split(",");
                        
                        int x = Integer.parseInt(values[0].trim()) + dx;
                        int y = Integer.parseInt(values[1].trim()) + dy;
                        int z = Integer.parseInt(values[2].trim()) + dz;
                        
                        String indent = line.substring(0, line.indexOf('p'));
                        result.append(indent).append(String.format("p(%d,%d,%d)\n", x, y, z));
                        continue;
                    } catch (Exception e) {
                        // If parsing fails, keep original line
                    }
                }
                
                result.append(line).append("\n");
            }
            
            radTextArea.setText(result.toString());
            Files.write(Paths.get(mergedFilePath), result.toString().getBytes());
            
            // Reload viewer to show changes
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.revalidate();
            viewerContainer.repaint();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error updating translation: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private JPanel compactColorEditorOverlay = null;

    public void showCompactColorEditor(JPanel overlay) {
        if (compactColorEditorOverlay != null) {
            viewerContainer.remove(compactColorEditorOverlay);
        }
        
        compactColorEditorOverlay = overlay;
        
        // Add to the layered pane at highest layer
        Component layeredPane = viewerContainer.getComponent(0); // The JLayeredPane
        if (layeredPane instanceof JLayeredPane) {
            JLayeredPane lp = (JLayeredPane) layeredPane;
            overlay.setBounds(0, 0, 1024, 768);
            lp.add(overlay, JLayeredPane.MODAL_LAYER);
            lp.revalidate();
            lp.repaint();
        }
    }

    public void removeCompactColorEditor(JPanel overlay) {
        Component layeredPane = viewerContainer.getComponent(0);
        if (layeredPane instanceof JLayeredPane) {
            JLayeredPane lp = (JLayeredPane) layeredPane;
            lp.remove(overlay);
            lp.revalidate();
            lp.repaint();
        }
        compactColorEditorOverlay = null;
    }

    private String addColorSchemes(String carCode, int schemeCount) {
        String[] lines = carCode.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inPoly = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("<p>")) inPoly = true;

            if (inPoly && trimmed.startsWith("c(") && !trimmed.startsWith("c1(")
                    && !trimmed.startsWith("c2(") && !trimmed.startsWith("c3(")) {
                result.append(line).append("\n");
                String indent = line.substring(0, line.indexOf('c'));
                if (schemeCount >= 2) result.append(indent).append(trimmed.replace("c(", "c1(")).append("\n");
                if (schemeCount >= 3) result.append(indent).append(trimmed.replace("c(", "c2(")).append("\n");
                if (schemeCount >= 4) result.append(indent).append(trimmed.replace("c(", "c3(")).append("\n");
                continue;
            }

            // Skip existing scheme tags to avoid duplicates
            if (inPoly && (trimmed.startsWith("c1(") || trimmed.startsWith("c2(") || trimmed.startsWith("c3("))) {
                continue;
            }

            if (trimmed.startsWith("</p>")) inPoly = false;

            result.append(line).append("\n");
        }
        return result.toString();
    }

    private void detectSchemes(String content) {
        boolean hasScheme3 = content.contains("c3(");
        scheme3Btn.setVisible(hasScheme3);
        colorPaletteEditor.setAvailableSchemes(hasScheme3 ? 4 : 3);
    }

    public ColorPaletteEditor getColorPaletteEditor() {
        return colorPaletteEditor;
    }
}
