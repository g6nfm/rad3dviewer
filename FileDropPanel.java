import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;

public class FileDropPanel extends JPanel {
    private JLabel label;
    private File file;

    public FileDropPanel(String text, int slotNumber) {
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setLayout(new BorderLayout());
        label = new JLabel(text, SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
        setPreferredSize(new Dimension(180, 150));

        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    Transferable t = support.getTransferable();
                    List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (!fileList.isEmpty()) {
                        file = fileList.get(0);
                        label.setText(file.getName());
                        return true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });
    }

    public File getFile() {
        return file;
    }
}
