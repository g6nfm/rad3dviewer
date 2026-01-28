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

    private File carsFolder = new File("cars");

    public RadMergerGUI() {
        setTitle("RAD File Merger & Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 1024);
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
        wheelViewerContainer.add(wheelViewer, BorderLayout.CENTER);

        wheelPanel.add(wheelViewerContainer);

        // 1. ADD THE BUTTON - Right after wheelPanel.add(wheelViewerContainer); around line 87
        wheelPanel.add(wheelViewerContainer);

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

        // Layer them using JLayeredPane
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(1024, 768));

        viewer.setBounds(0, 0, 1024, 768);
        wheelAnchorEditor.setBounds(0, 0, 1024, 768);

        layeredPane.add(viewer, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(wheelAnchorEditor, JLayeredPane.PALETTE_LAYER);

        viewerContainer.add(layeredPane, BorderLayout.CENTER);

        // Add button to toggle editor
        JButton toggleEditorBtn = new JButton("Edit Wheel Anchors");
        toggleEditorBtn.addActionListener(e -> {
            if (viewer.getCarModel() != null) {
                wheelAnchorEditor.loadCarModel(viewer.getCarModel(), radTextArea.getText());
                wheelAnchorEditor.setVisible(!wheelAnchorEditor.isVisible());
            } else {
                JOptionPane.showMessageDialog(this, "Load a car first!", "No Car", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel viewerToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewerToolbar.add(toggleEditorBtn);
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
            String searchTerm = JOptionPane.showInputDialog(this, "Enter text to find:");
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String replaceTerm = JOptionPane.showInputDialog(this, "Replace with:");
                if (replaceTerm != null) {
                    String content = radTextArea.getText();
                    content = content.replace(searchTerm, replaceTerm);
                    radTextArea.setText(content);
                }
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RadMergerGUI::new);
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
            
            // Read wheel file
            String wheelContent = new String(Files.readAllBytes(wheelFile.toPath()));
            
            // Extract wheel model from wheel file
            String wheelModel = extractWheelModel(wheelContent);
            if (wheelModel == null) {
                JOptionPane.showMessageDialog(this, "No wheel model found in selected wheel file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check if car already has wheel models
            if (carContent.contains("<wheelModel(")) {
                int result = JOptionPane.showConfirmDialog(this, 
                    "This car already has custom wheels. Replace them?", 
                    "Replace Wheels?", 
                    JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) return;
                
                // Remove existing wheel models
                carContent = removeAllWheelModels(carContent);
            }
            
            // Convert 6-arg w() calls to 7-arg
            carContent = convertWheelCallsTo7Args(carContent);
            
            // Append wheel model before the last wheel definition line
            carContent = appendWheelModel(carContent, wheelModel);
            
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

    private String extractWheelModel(String content) {
        // Wheel files are regular .rad files, so we need to:
        // 1. Extract all <p>...</p> blocks
        // 2. Convert them to [p]...[/p]
        // 3. Wrap in <wheelModel(0)>...</wheelModel>
        
        StringBuilder wheelModel = new StringBuilder();
        wheelModel.append("<wheelModel(0)>\n\n");
        
        // Find all <p>...</p> blocks
        int searchStart = 0;
        boolean foundAnyPolygon = false;
        
        while (true) {
            int pStart = content.indexOf("<p>", searchStart);
            if (pStart == -1) break;
            
            int pEnd = content.indexOf("</p>", pStart);
            if (pEnd == -1) break;
            
            // Extract the polygon block including the tags
            String polygonBlock = content.substring(pStart, pEnd + "</p>".length());
            
            // Convert <p> to [p] and </p> to [/p]
            polygonBlock = polygonBlock.replace("<p>", "[p]");
            polygonBlock = polygonBlock.replace("</p>", "[/p]");
            
            // Add to wheel model
            wheelModel.append(polygonBlock).append("\n\n");
            
            foundAnyPolygon = true;
            searchStart = pEnd + "</p>".length();
        }
        
        wheelModel.append("</wheelModel>");
        
        if (!foundAnyPolygon) {
            return null; // No polygons found
        }
        
        return wheelModel.toString();
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

}
