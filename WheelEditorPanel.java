import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class WheelEditorPanel extends JPanel {

    private ContO car;

    private List<JCheckBox> wheelChecks = new ArrayList<>();
    private JComboBox<Integer> modelIdSelect;
    private JCheckBox stockWheelsCheck;

    private JPanel axleGroupContainer;
    private final List<AxleGroup> axleGroups = new ArrayList<>();

    // ----------------------------------------------------------
    // Internal data for each axle (one left + one right wheel)
    // ----------------------------------------------------------
    private static class AxleGroup {
        int leftIndex;
        int rightIndex;

        JTextField xField, yField, zField;
        JTextField widthField, heightField;

        JTextField rimR, rimG, rimB;
        JTextField rimSizeField, rimDepthField;
        JTextField hideField;

        JButton applyButton;
    }

    


    // ===========================================================
    // TOP PANEL
    // ===========================================================
    private JPanel buildTopPanel() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createTitledBorder("Wheel Selection"));

        // Wheel checkboxes row
        JPanel wheelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        wheelRow.setName("WHEEL_CHECK_ROW");
        top.add(wheelRow);

        // Model ID select
        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        modelRow.add(new JLabel("ModelID:"));
        modelIdSelect = new JComboBox<>();
        modelRow.add(modelIdSelect);
        top.add(modelRow);

        // Stock wheels toggle
        stockWheelsCheck = new JCheckBox("Use Stock Wheels");
        top.add(stockWheelsCheck);

        return top;
    }

    // ===========================================================
    // LOAD CAR INTO EDITOR
    // ===========================================================
    public void setContO(ContO car) {
        this.car = car;
        if (car == null) return;

        rebuildWheelCheckboxes();
        rebuildModelIdList();
        rebuildAxleGroups();
    }

    private void rebuildWheelCheckboxes() {
        JPanel wheelRow = null;

        JPanel topPanel = (JPanel)getComponent(0);
        for (Component c : topPanel.getComponents()) {
            if ("WHEEL_CHECK_ROW".equals(c.getName()))
                wheelRow = (JPanel)c;
        }

        if (wheelRow == null) return;

        wheelRow.removeAll();
        wheelChecks.clear();

        for (int i = 0; i < car.wheelCount; i++) {
            JCheckBox b = new JCheckBox(String.valueOf(i));
            wheelChecks.add(b);
            wheelRow.add(b);
        }

        wheelRow.revalidate();
        wheelRow.repaint();
    }

    private void rebuildModelIdList() {
        modelIdSelect.removeAllItems();
        for (Integer id : car.wheelModels.keySet())
            modelIdSelect.addItem(id);
        modelIdSelect.addItem(-1); // stock/default
    }

    // ===========================================================
    // AXLE DETECTION + PANEL CREATION
    // ===========================================================
    private void rebuildAxleGroups() {
        axleGroups.clear();
        axleGroupContainer.removeAll();

        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();

        for (int i = 0; i < car.wheelCount; i++) {
            ContO.WheelAnchor w = car.anchors[i];
            if (w.x < 0) left.add(i);
            else right.add(i);
        }

        int pairs = Math.min(left.size(), right.size());

        for (int i = 0; i < pairs; i++) {

            AxleGroup g = new AxleGroup();
            g.leftIndex  = left.get(i);
            g.rightIndex = right.get(i);

            JPanel innerPanel = buildAxlePanel(i, g);

            String title = (i == 0 ? "FRONT WHEELS" :
                            i == 1 ? "BACK WHEELS" :
                                     "AXLE " + i);

            CollapsiblePanel cp = new CollapsiblePanel(title, innerPanel);

            axleGroups.add(g);
            axleGroupContainer.add(cp);
            axleGroupContainer.add(Box.createVerticalStrut(6));
        }

        axleGroupContainer.revalidate();
        axleGroupContainer.repaint();
    }

    // ===========================================================
    // BUILD AXLE EDITOR (inside collapsible panel)
    // ===========================================================
    private JPanel buildAxlePanel(int axleId, AxleGroup g) {

        int L = g.leftIndex;
        ContO.WheelAnchor wa = car.anchors[L];

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(0, 6, 4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // X
        p.add(new JLabel("±X:"));
        g.xField = new JTextField(String.valueOf(Math.abs(wa.x)));
        p.add(g.xField);

        // Y
        p.add(new JLabel("Y:"));
        g.yField = new JTextField(String.valueOf(wa.y));
        p.add(g.yField);

        // Z
        p.add(new JLabel("Z:"));
        g.zField = new JTextField(String.valueOf(wa.z));
        p.add(g.zField);

        // Width
        p.add(new JLabel("Width:"));
        g.widthField = new JTextField(String.valueOf(wa.width));
        p.add(g.widthField);

        // Height
        p.add(new JLabel("Height:"));
        g.heightField = new JTextField(String.valueOf(wa.size));
        p.add(g.heightField);

        // Rim RGB
        p.add(new JLabel("Rims RGB:"));
        g.rimR = new JTextField("220");
        g.rimG = new JTextField("220");
        g.rimB = new JTextField("220");
        p.add(g.rimR);
        p.add(g.rimG);
        p.add(g.rimB);

        // Rim size
        p.add(new JLabel("Rim Size:"));
        g.rimSizeField = new JTextField("23");
        p.add(g.rimSizeField);

        // Depth
        p.add(new JLabel("Depth:"));
        g.rimDepthField = new JTextField("25");
        p.add(g.rimDepthField);

        // Hide
        p.add(new JLabel("Hide:"));
        g.hideField = new JTextField("40");
        p.add(g.hideField);

        // Apply
        g.applyButton = new JButton("Apply");
        g.applyButton.addActionListener(e -> applyAxleGroup(g));
        p.add(g.applyButton);

        return p;
    }

    // ===========================================================
    // APPLY WHEEL EDITS
    // ===========================================================
    private void applyAxleGroup(AxleGroup g) {

        int L = g.leftIndex;
        int R = g.rightIndex;

        int x   = Integer.parseInt(g.xField.getText());
        int y   = Integer.parseInt(g.yField.getText());
        int z   = Integer.parseInt(g.zField.getText());
        int wid = Integer.parseInt(g.widthField.getText());
        int hei = Integer.parseInt(g.heightField.getText());

        // Left wheel = -x
        applyWheel(L, -x, y, z, wid, hei);

        // Right wheel = +x
        applyWheel(R,  x, y, z, wid, hei);
    }

    private void applyWheel(int idx, int x, int y, int z, int width, int height) {
        ContO.WheelAnchor wa = car.anchors[idx];

        wa.x = x;
        wa.y = y;
        wa.z = z;
        wa.width = width;
        wa.size = height;
    }
}

// ================================================================
// Collapsible Panel Utility
// ================================================================
class CollapsiblePanel extends JPanel {

    private final JPanel header;
    private final JPanel content;
    private boolean expanded = false;

    public CollapsiblePanel(String title, JPanel inner) {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        content = inner;

        header = new JPanel(new BorderLayout());
        header.setBackground(new Color(230, 230, 230));
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel lbl = new JLabel("▸ " + title);
        header.add(lbl, BorderLayout.WEST);

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle(lbl);
            }
        });

        add(header, BorderLayout.NORTH);
    }

    private void toggle(JLabel lbl) {
        expanded = !expanded;

        if (expanded) {
            lbl.setText("▾ " + lbl.getText().substring(2));
            add(content, BorderLayout.CENTER);
        } else {
            lbl.setText("▸ " + lbl.getText().substring(2));
            remove(content);
        }

        revalidate();
        repaint();
    }
}
