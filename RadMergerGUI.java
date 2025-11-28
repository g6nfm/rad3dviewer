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
        wheelViewerContainer.add(wheelViewer, BorderLayout.CENTER);

        wheelPanel.add(wheelViewerContainer);

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
        viewer = new Rad3DViewer();                // always create one
        viewerContainer.add(viewer, BorderLayout.CENTER);
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
                wheelViewer.loadRadFile(wfile.getAbsolutePath());
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



}
