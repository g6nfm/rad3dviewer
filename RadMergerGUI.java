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
    // Use a JTabbedPane for two tabs: 3D viewer and .rad file editor.
    private JTabbedPane tabbedPane;

    // Tab 1: 3D Viewer container.
    private JPanel viewerContainer;
    // Tab 2: Panel for editing the .rad file text.
    private JPanel radFilePanel;
    private JTextArea radTextArea;

    // We will save/operate on the merged file.
    private String mergedFilePath = "merged.rad";

    public RadMergerGUI() {
        setTitle("RAD File Merger & Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 1024);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top panel: drag-and-drop file slots.
        JPanel dropPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        filePanel1 = new FileDropPanel("Slot 1 (.rad file)", 1);
        filePanel2 = new FileDropPanel("Slot 2 (.rad file)", 2);
        filePanel3 = new FileDropPanel("Slot 3 (.rad file)", 3);
        dropPanel.add(filePanel1);
        dropPanel.add(filePanel2);
        dropPanel.add(filePanel3);
        add(dropPanel, BorderLayout.NORTH);

        // Create the tabbed pane.
        tabbedPane = new JTabbedPane();

        // Tab 1: Model Viewer.
        viewerContainer = new JPanel(new BorderLayout());
        File mergedFile = new File(mergedFilePath);
        if (mergedFile.exists()) {
            Rad3DViewer viewer = new Rad3DViewer();
            viewer.loadRadFile(mergedFilePath);
            viewerContainer.add(viewer, BorderLayout.CENTER);
        } else {
            viewerContainer.add(new JLabel("Merged model will display here.", SwingConstants.CENTER), BorderLayout.CENTER);
        }
        tabbedPane.addTab("Model Viewer", viewerContainer);

        // Tab 2: RAD File Editor.
        radFilePanel = new JPanel(new BorderLayout());
        // Create a toolbar panel at the top for Save, Find, and Replace.
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSave = new JButton("Save");
        JButton btnFind = new JButton("Find");
        JButton btnReplace = new JButton("Replace");
        toolbar.add(btnSave);
        toolbar.add(btnFind);
        toolbar.add(btnReplace);

        // Create the text area with a monospaced, bold font.
        radTextArea = new JTextArea();
        radTextArea.setFont(new Font("Monospaced", Font.BOLD, 16));
        radTextArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(radTextArea);
        radFilePanel.add(toolbar, BorderLayout.NORTH);
        radFilePanel.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.addTab(".rad File Editor", radFilePanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel with merge button and status label.
        JPanel bottomPanel = new JPanel();
        JButton mergeButton = new JButton("Merge");
        mergeButton.addActionListener(e -> onMerge());
        bottomPanel.add(mergeButton);
        statusLabel = new JLabel("Drag and drop your .rad files into the slots above.");
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // Button actions for the RAD File Editor toolbar.
        btnSave.addActionListener(e -> {
            try {
                // Save the current text in radTextArea to the merged file.
                try (PrintWriter out = new PrintWriter(new FileWriter(mergedFilePath))) {
                    out.print(radTextArea.getText());
                }
                JOptionPane.showMessageDialog(this, "File saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnFind.addActionListener(e -> {
            String searchTerm = JOptionPane.showInputDialog(this, "Enter text to find:");
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String content = radTextArea.getText();
                int pos = content.indexOf(searchTerm, radTextArea.getCaretPosition());
                if (pos == -1) {
                    pos = content.indexOf(searchTerm);
                }
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

        // If the merged file exists, load its text.
        if (mergedFile.exists()) {
            loadRadFileText(mergedFilePath);
        }

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RadMergerGUI::new);
    }

    private void onMerge() {
        // Verify that a file has been dropped into each slot.
        if (filePanel1.getFile() == null || filePanel2.getFile() == null || filePanel3.getFile() == null) {
            JOptionPane.showMessageDialog(this, "Please drop a file into each slot.", "Missing File", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            mergeFiles(filePanel1.getFile(), filePanel2.getFile(), filePanel3.getFile(), mergedFilePath);
            statusLabel.setText("Merging complete. Output file: " + mergedFilePath);
            JOptionPane.showMessageDialog(this, "Merging complete!\nOutput file: " + mergedFilePath, "Success", JOptionPane.INFORMATION_MESSAGE);

            // Update the 3D viewer.
            Rad3DViewer viewer = new Rad3DViewer();
            //viewer.loadRadFile(mergedFilePath);
            viewerContainer.removeAll();
            viewerContainer.add(viewer, BorderLayout.CENTER);
            viewerContainer.revalidate();
            viewerContainer.repaint();

            // Update the editable text area.
            loadRadFileText(mergedFilePath);
            // Optionally switch to the RAD File Editor tab:
            tabbedPane.setSelectedIndex(1);
        } catch (IOException ex) {
            statusLabel.setText("Error during merge: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error during merge:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

    /**
     * Load the contents of the merged .rad file into the editable text area.
     */
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
}
