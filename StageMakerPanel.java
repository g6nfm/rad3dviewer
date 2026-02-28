import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class StageMakerPanel extends JPanel {

    private static final String[] TRACK_MODELS = {
        "road","froad","twister2","twister1","turn","offroad","bumproad","offturn",
        "nroad","nturn","roblend","noblend","rnblend","roadend","offroadend","hpground",
        "ramp30","cramp35","dramp15","dhilo15","slide10","takeoff","sramp22","offbump",
        "offramp","sofframp","halfpipe","spikes","rail","thewall","checkpoint","fixpoint",
        "offcheckpoint","sideoff","bsideoff","uprise","riseroad","sroad","soffroad"
    };

    private static final String[] EXTRA_MODELS = {
        "offroadtakeoff","offlanding","invisibleroad","floatingroad","floatingturn",
        "floatingriseroad","floatingend","floatingramp","floatingcheckpoint",
        "pillar1500","pillar3000","pillar4500","floatingtakeoff","singlewallroad",
        "single1500","single3000","dtwister1","dtwister2","rbump","invisroad",
        "nocramp35","twister1d","twister2d","droadend","bigtree1","bigtree2",
        "bigtree3","bigtree4","bigtree5","shortroad","shortnroad","smallrampwide",
        "offrampwide","halfbasicramp","glideramp"
    };

    private static final String[] CAT_NAMES = {
        "Roads","Ramps","Checkpoints","Obstacles","Fix Hoops","Extra / Custom"
    };

    private static final int[][] CAT_INDICES = {
        {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,33,34,36,37,38},
        {16,17,18,19,20,21,22,23,24,25,26},
        {30,32},
        {27,28},
        {31},
        {}
    };

    private static final int SNAP = 5600;

    private byte[][] modelBytes;
    private ContO    previewModel;
    private int      selectedModelIdx = 0;
    private int      selectedCatIdx   = 0;

    private final java.util.List<PlacedPart> stage     = new ArrayList<PlacedPart>();
    private final Deque<String>              undoStack = new ArrayDeque<String>();

    private CardLayout mainCards;
    private JPanel mainCardPanel;

    

    private int[]  snapV    = {0,0,0};
    private int[]  skyV     = {217,251,207};
    private int[]  fogV     = {200,204,153};
    private int[]  cloudsV  = {210,205,144,4,-2000};
    private int[]  groundV  = {205,193,170};
    private int[]  texV     = {0,0,0,10};
    private int    fadefrom = 7100;
    private int    density  = 8;
    private int    mountains= 45403;
    private int    nlaps    = 3;
    private String stageName= "";
    private File   stageFile= null;

    private int rot = 0;

    private final Medium medium = new Medium();

    private int camX = 0, camZ = 0;
    private int camY = -10000;

    private int[] ghostATP = null;

    private static final int SCROLL_STEP = 800;

    private boolean removeMode = false;
    private boolean snapEnabled = true;

    private PlacedPart selectedPart = null;

    private JButton[] tabBtns;
    

    private JComboBox<String> catCombo;
    private JComboBox<String> stageCombo;
    private JComboBox<String> partCombo;
    private PreviewPanel      previewPanel;
    private JLabel            rotLabel;
    private JLabel            idLabel;
    private StageCanvas       stageCanvas;

    public StageMakerPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(220,220,220));
        buildUI();
        loadModelsAsync();
    }

    private void buildUI() {
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildBottomBar(), BorderLayout.SOUTH);

        stageCanvas = new StageCanvas();
        JPanel canvasWrapper = new JPanel(new BorderLayout());
        canvasWrapper.setBackground(new Color(220,220,220));
        canvasWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8,8,8,8),
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2)
        ));
        JButton arrowUp    = arrowBtn(0);
        JButton arrowDown  = arrowBtn(180);
        JButton arrowLeft  = arrowBtn(270);
        JButton arrowRight = arrowBtn(90);
        arrowUp   .addActionListener(e -> { camZ += SCROLL_STEP; stageCanvas.repaint(); });
        arrowDown .addActionListener(e -> { camZ -= SCROLL_STEP; stageCanvas.repaint(); });
        arrowLeft .addActionListener(e -> { camX -= SCROLL_STEP; stageCanvas.repaint(); });
        arrowRight.addActionListener(e -> { camX += SCROLL_STEP; stageCanvas.repaint(); });
        JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));
        north.setOpaque(false); north.add(arrowUp);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));
        south.setOpaque(false); south.add(arrowDown);
        JPanel west = new JPanel(new GridLayout(3,1));
        west.setOpaque(false);
        west.add(new JLabel()); west.add(arrowLeft); west.add(new JLabel());
        JPanel east = new JPanel(new GridLayout(3,1));
        east.setOpaque(false);
        east.add(new JLabel()); east.add(arrowRight); east.add(new JLabel());
        canvasWrapper.add(north, BorderLayout.NORTH);
        canvasWrapper.add(south, BorderLayout.SOUTH);
        canvasWrapper.add(west,  BorderLayout.WEST);
        canvasWrapper.add(east,  BorderLayout.EAST);
        canvasWrapper.add(stageCanvas, BorderLayout.CENTER);

        JPanel buildPanel = new JPanel(new BorderLayout(0,0));
        buildPanel.setBackground(new Color(220,220,220));
        buildPanel.add(buildLeftPanel(), BorderLayout.WEST);
        buildPanel.add(canvasWrapper, BorderLayout.CENTER);

        mainCards = new CardLayout();
        mainCardPanel = new JPanel(mainCards);
        mainCardPanel.setBackground(new Color(220,220,220));
        mainCardPanel.add(buildStageSelectPanel(), "stage");
        mainCardPanel.add(buildPanel, "build");
        mainCards.show(mainCardPanel, "stage");
        add(mainCardPanel, BorderLayout.CENTER);

        InputMap  im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),"undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),"save");
        am.put("undo", new AbstractAction(){public void actionPerformed(ActionEvent e){doUndo();}});
        am.put("save", new AbstractAction(){public void actionPerformed(ActionEvent e){saveStage();}});
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "scrollUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "scrollDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "scrollLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "scrollRight");
        am.put("scrollUp",    new AbstractAction(){ public void actionPerformed(ActionEvent e){ camZ += 300; stageCanvas.repaint(); }});
        am.put("scrollDown",  new AbstractAction(){ public void actionPerformed(ActionEvent e){ camZ -= 300; stageCanvas.repaint(); }});
        am.put("scrollLeft",  new AbstractAction(){ public void actionPerformed(ActionEvent e){ camX -= 300; stageCanvas.repaint(); }});
        am.put("scrollRight", new AbstractAction(){ public void actionPerformed(ActionEvent e){ camX += 300; stageCanvas.repaint(); }});
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(195,195,195));
        bar.setBorder(BorderFactory.createMatteBorder(0,0,2,0,Color.DARK_GRAY));
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        tabs.setOpaque(false);
        String[] tabNames = {"Stage","Build","View & Edit","Publish"};
        tabBtns = new JButton[tabNames.length];
        for (int i = 0; i < tabNames.length; i++) {
            final int ti = i;
            JButton tb = new JButton(tabNames[i]);
            tb.setFont(new Font("Arial",Font.BOLD,13));
            tb.setFocusPainted(false);
            tb.setBackground(i==0 ? new Color(220,220,220) : new Color(175,175,175));
            tb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2,2,0,2,Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(4,14,4,14)));
            tb.addActionListener(e -> {
                if (ti == 0) {
                    refreshStageCombo(stageCombo);
                    mainCards.show(mainCardPanel, "stage");
                }
                if (ti == 1) {
                    String sel = (String) stageCombo.getSelectedItem();
                    if (sel != null && (stageFile == null || !stageFile.getName().equals(sel + ".txt"))) {
                        stageFile = new File("data/stages/" + sel + ".txt");
                        stage.clear(); undoStack.clear();
                        try {
                            String content = new String(Files.readAllBytes(stageFile.toPath()));
                            parseHeader(content); parseBstage(content);
                            stageName = sel;
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Could not load stage:\n" + ex.getMessage());
                        }
                    }
                    mainCards.show(mainCardPanel, "build");
                    stageCanvas.repaint();
                }
                for (int j = 0; j < tabBtns.length; j++)
                    tabBtns[j].setBackground(j == ti ? new Color(220,220,220) : new Color(175,175,175));
            });
            tabBtns[i] = tb;
            tabs.add(tb);
        }
        bar.add(tabs, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,4));
        right.setOpaque(false);
        JButton sv  = new JButton("  Save  ");
        JButton svp = new JButton("  Save & Preview  ");
        sv .addActionListener(e -> saveStage());
        svp.addActionListener(e -> saveStage());
        right.add(sv); right.add(svp);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildStageSelectPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(220,220,220));
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(220,220,220));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
            BorderFactory.createEmptyBorder(20,40,20,40)
        ));
        box.setMaximumSize(new Dimension(480, 300));

        JLabel title = new JLabel("Select Stage to Edit", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.PLAIN, 14));
        title.setAlignmentX(CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(10));

        stageCombo = new JComboBox<>();
        stageCombo.setMaximumSize(new Dimension(360, 26));
        stageCombo.setAlignmentX(CENTER_ALIGNMENT);
        refreshStageCombo(stageCombo);
        box.add(stageCombo);
        box.add(Box.createVerticalStrut(14));

        JButton makeNew = new JButton("Make new Stage");
        makeNew.setAlignmentX(CENTER_ALIGNMENT);
        makeNew.addActionListener(e -> {
            initNewStage();
            mainCards.show(mainCardPanel, "build");
            for (int j = 0; j < tabBtns.length; j++)
                tabBtns[j].setBackground(j == 1 ? new Color(220,220,220) : new Color(175,175,175));
        });
        box.add(makeNew);
        box.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        JButton rename = new JButton("Rename Stage");
        JButton delete = new JButton("Delete Stage");

        rename.addActionListener(e -> {
            String sel = (String) stageCombo.getSelectedItem();
            if (sel == null) return;
            String newName = JOptionPane.showInputDialog(this, "New name:", sel);
            if (newName == null || newName.trim().isEmpty()) return;
            File oldFile = new File("data/stages/" + sel + ".txt");
            File newFile = new File("data/stages/" + newName.trim() + ".txt");
            if (oldFile.renameTo(newFile)) {
                if (stageFile != null && stageFile.getName().equals(sel + ".txt")) {
                    stageFile = newFile;
                    stageName = newName.trim();
                }
                refreshStageCombo(stageCombo);
            }
        });

        delete.addActionListener(e -> {
            String sel = (String) stageCombo.getSelectedItem();
            if (sel == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + sel + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                new File("data/stages/" + sel + ".txt").delete();
                refreshStageCombo(stageCombo);
            }
        });

        btnRow.add(rename); btnRow.add(delete);
        box.add(btnRow);
        p.add(box);
        return p;
    }

    private void refreshStageCombo(JComboBox<String> combo) {
        combo.removeAllItems();
        File dir = new File("data/stages");
        if (dir.exists()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files)
                    combo.addItem(f.getName().replace(".txt", ""));
            }
        }
    }

    private JButton arrowBtn(int deg) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean pressed = getModel().isPressed() || getModel().isArmed();
                int cx = getWidth()/2, cy = getHeight()/2, r = 12;
                double a = Math.toRadians(deg - 90);
                int[] px = { cx+(int)(r*Math.cos(a)), cx+(int)(r*Math.cos(a+Math.toRadians(130))), cx+(int)(r*Math.cos(a-Math.toRadians(130))) };
                int[] py = { cy+(int)(r*Math.sin(a)), cy+(int)(r*Math.sin(a+Math.toRadians(130))), cy+(int)(r*Math.sin(a-Math.toRadians(130))) };
                g2.setColor(pressed ? new Color(90,95,120,240) : new Color(170,175,190,210));
                g2.fillPolygon(px, py, 3);
                g2.setColor(new Color(120,125,145));
                g2.setStroke(new BasicStroke(1f));
                g2.drawPolygon(px, py, 3);
            }
        };
        b.setPreferredSize(new Dimension(32, 32));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        return b;
    }


    private JPanel buildLeftPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(new Color(218,218,218));
        left.setPreferredSize(new Dimension(360,0));
        left.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JLabel ps = new JLabel("Part Selection");
        ps.setFont(new Font("Arial",Font.BOLD,11));
        ps.setAlignmentX(LEFT_ALIGNMENT);
        left.add(ps);
        left.add(Box.createVerticalStrut(3));

        JPanel catRow = new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
        catRow.setOpaque(false); catRow.setAlignmentX(LEFT_ALIGNMENT);
        catCombo = new JComboBox<String>(CAT_NAMES);
        catCombo.setPreferredSize(new Dimension(160,26));
        catCombo.setBackground(new Color(90,100,170));
        catCombo.setForeground(Color.WHITE);
        catCombo.setFont(new Font("Arial",Font.BOLD,13));
        catCombo.addActionListener(e -> {
            selectedCatIdx   = catCombo.getSelectedIndex();
            selectedModelIdx = catToModelIdx(selectedCatIdx, 0);
            rebuildPartCombo();
            rebuildPreview();
        });
        JButton about = new JButton("About Part");
        about.setFont(new Font("Arial",Font.PLAIN,12));
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Part:    " + modelName(selectedModelIdx) +
            "\nFile ID: " + modelFileId(selectedModelIdx),
            "About Part", JOptionPane.INFORMATION_MESSAGE));
        catRow.add(catCombo); catRow.add(about);
        left.add(catRow);
        left.add(Box.createVerticalStrut(4));

        partCombo = new JComboBox<String>();
        partCombo.setBackground(new Color(70,80,155));
        partCombo.setForeground(Color.WHITE);
        partCombo.setFont(new Font("Arial",Font.BOLD,13));
        partCombo.setMaximumSize(new Dimension(344,26));
        partCombo.setAlignmentX(LEFT_ALIGNMENT);
        partCombo.addActionListener(e -> {
            int li = partCombo.getSelectedIndex();
            if (li >= 0) { selectedModelIdx = catToModelIdx(selectedCatIdx, li); rebuildPreview(); }
        });
        left.add(partCombo);
        left.add(Box.createVerticalStrut(6));

        JPanel previewOuter = new JPanel(null);
        previewOuter.setPreferredSize(new Dimension(344,290));
        previewOuter.setMaximumSize (new Dimension(344,290));
        previewOuter.setAlignmentX(LEFT_ALIGNMENT);
        previewOuter.setBackground(new Color(205,205,205));
        previewOuter.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        previewPanel = new PreviewPanel();
        previewPanel.setBounds(0,0,344,290);
        previewOuter.add(previewPanel);

        rotLabel = new JLabel("0°");
        rotLabel.setFont(new Font("Arial",Font.PLAIN,11));
        rotLabel.setBounds(6,4,50,16);
        previewOuter.add(rotLabel);

        idLabel = new JLabel("#10");
        idLabel.setFont(new Font("Arial",Font.PLAIN,11));
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        idLabel.setBounds(288,4,50,16);
        previewOuter.add(idLabel);

        left.add(previewOuter);
        left.add(Box.createVerticalStrut(6));

        JPanel rotRow = new JPanel(new FlowLayout(FlowLayout.CENTER,10,0));
        rotRow.setOpaque(false); rotRow.setAlignmentX(LEFT_ALIGNMENT);
        JButton prev = new JButton("<");
        JButton rotB = new JButton("  Rotate  ");
        JButton next = new JButton(">");
        for (JButton b : new JButton[]{prev,rotB,next}) b.setFont(new Font("Arial",Font.BOLD,13));
        prev.addActionListener(e -> { int li = (partCombo.getSelectedIndex()-1+partCombo.getItemCount())%partCombo.getItemCount(); partCombo.setSelectedIndex(li); });
        next.addActionListener(e -> { int li = (partCombo.getSelectedIndex()+1)%partCombo.getItemCount(); partCombo.setSelectedIndex(li); });
        rotB.addActionListener(e -> rotate90());
        rotRow.add(prev); rotRow.add(rotB); rotRow.add(next);
        left.add(rotRow);
        left.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(0,2,6,6));
        grid.setOpaque(false); grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(344,120));
        JButton editSrc = btn("Edit Source...");
        JButton reload  = btn("Reload");
        JButton props   = btn("Stage Properties");
        JButton undo    = btn("< Undo");
        editSrc.addActionListener(e -> openSourceEditor());
        reload .addActionListener(e -> reloadStage());
        props  .addActionListener(e -> showStageProperties());
        undo   .addActionListener(e -> doUndo());
        grid.add(editSrc); grid.add(reload);
        grid.add(props);   grid.add(undo);
        left.add(grid);
        left.add(Box.createVerticalStrut(6));

        JButton removeBtn = wideBtn("Remove / Edit Part");
        JButton gotoBtn   = wideBtn("Go to >  Startline");
        removeBtn.addActionListener(e -> toggleRemoveMode());
        gotoBtn  .addActionListener(e -> { camX=0; camZ=0; stageCanvas.repaint(); });
        left.add(removeBtn); left.add(Box.createVerticalStrut(4));
        left.add(gotoBtn);
        left.add(Box.createVerticalGlue());
        return left;
    }

    private void toggleRemoveMode() {
        removeMode = !removeMode;
        if (stageCanvas != null)
            stageCanvas.setCursor(removeMode
                ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(210,210,210));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.GRAY));
        bar.setPreferredSize(new Dimension(0,22));
        JLabel mc = new JLabel("  Memory Consumption : ");
        mc.setFont(new Font("Arial",Font.BOLD,11));
        bar.add(mc, BorderLayout.WEST);
        JPanel mp = new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
        mp.setOpaque(false);
        JPanel greenBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = Math.max(1, Math.min(200, stage.size() * 2));
                g.setColor(w < 100 ? new Color(w+100,225,30) : new Color(200,325-w,30));
                g.fillRect(0,3,w,12);
                g.setColor(Color.BLACK);
                g.drawRect(0,3,200,12);
                g.setFont(new Font("Arial",Font.BOLD,9));
                g.setColor(Color.BLACK);
                String txt = "Stage Area";
                g.drawString(txt, 100-g.getFontMetrics().stringWidth(txt)/2, 13);
            }
        };
        greenBar.setOpaque(false);
        greenBar.setPreferredSize(new Dimension(202,18));
        mp.add(greenBar);
        JLabel pct = new JLabel("  " + Math.min(100, stage.size()*2) + " % used");
        pct.setFont(new Font("Arial",Font.PLAIN,11));
        mp.add(pct);
        bar.add(mp, BorderLayout.CENTER);
        JButton kb = new JButton("Keyboard Controls");
        kb.setFont(new Font("Arial",Font.PLAIN,11));
        kb.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Arrow keys: scroll stage\n+/-: zoom in/out\n" +
            "R: rotate selected part\nLeft click: place part\n" +
            "Right click / Remove mode: remove part\nCtrl+Z: undo   Ctrl+S: save"));
        bar.add(kb, BorderLayout.EAST);
        return bar;
    }

    private JButton btn(String t)     { JButton b = new JButton(t); b.setFont(new Font("Arial",Font.BOLD,12)); return b; }
    private JButton wideBtn(String t) { JButton b = btn(t); b.setAlignmentX(LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(344,28)); return b; }

    // ── Part combo ──────────────────────────────────────────────────────────
    private void rebuildPartCombo() {
        partCombo.removeAllItems();
        int cat = selectedCatIdx;
        if (cat < CAT_INDICES.length - 1) {
            for (int idx : CAT_INDICES[cat]) partCombo.addItem(modelName(idx));
        } else {
            for (String s : EXTRA_MODELS) partCombo.addItem(s);
        }
        if (partCombo.getItemCount() > 0) partCombo.setSelectedIndex(0);
    }

    private int catToModelIdx(int cat, int listPos) {
        if (cat < CAT_INDICES.length - 1) {
            int[] indices = CAT_INDICES[cat];
            if (listPos < indices.length) return indices[listPos];
            return indices[0];
        }
        return TRACK_MODELS.length + listPos;
    }

    private String modelName(int idx) {
        return idx < TRACK_MODELS.length ? TRACK_MODELS[idx] : EXTRA_MODELS[idx - TRACK_MODELS.length];
    }

    private int modelFileId(int idx) { return idx + 10; }
    private int fileIdToIdx(int fid) { return fid - 10; }

    // ── Model loading ────────────────────────────────────────────────────────
    private void loadModelsAsync() {
        SwingWorker<byte[][], Void> w = new SwingWorker<byte[][], Void>() {
            @Override protected byte[][] doInBackground() throws Exception { return loadZip(); }
            @Override protected void done() {
                try {
                    modelBytes = get();
                    rebuildPartCombo();
                    rebuildPreview();
                    //initNewStage();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StageMakerPanel.this,
                        "Could not load models:\n" + ex.getMessage() +
                        "\n\nPlace models.radq in data/",
                        "Model Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void initNewStage() {
        stage.clear();
        undoStack.clear();

        String name = JOptionPane.showInputDialog(this, "Stage name:", "New Stage", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) name = "untitled";
        stageName = name.trim();
        stageFile = new File("data/stages/" + stageName + ".txt");

        int choice = JOptionPane.showOptionDialog(this,
            "Choose a start piece for your new stage:",
            "New Stage",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new String[]{ "Road Start", "Offroad Start" },
            "Road Start"
        );
        addPart(choice == 1 ? 48 : 47, 0, 0, 0, 0);
        stageCanvas.repaint();
    }

    private byte[][] loadZip() throws IOException {
        int total = TRACK_MODELS.length + EXTRA_MODELS.length;
        byte[][] r = new byte[total][];
        Map<String,Integer> map = new HashMap<String,Integer>();
        for (int i=0;i<TRACK_MODELS.length;i++) map.put(TRACK_MODELS[i].toLowerCase(),i);
        for (int i=0;i<EXTRA_MODELS.length; i++) map.put(EXTRA_MODELS[i].toLowerCase(), TRACK_MODELS.length+i);
        File zip = new File("data/models.radq");
        if (!zip.exists()) throw new FileNotFoundException(zip.getAbsolutePath());
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                int sl = name.lastIndexOf('/'); if (sl>=0) name=name.substring(sl+1);
                if (name.toLowerCase().endsWith(".rad")) name=name.substring(0,name.length()-4);
                Integer idx = map.get(name.toLowerCase());
                if (idx!=null) r[idx] = slurp(zis);
                zis.closeEntry();
            }
        }
        return r;
    }

    private static byte[] slurp(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192]; int n;
        while ((n=zis.read(tmp))!=-1) b.write(tmp,0,n);
        return b.toByteArray();
    }

    // ── Preview ──────────────────────────────────────────────────────────────
    private void rebuildPreview() {
        previewModel = null;
        if (modelBytes!=null && selectedModelIdx<modelBytes.length && modelBytes[selectedModelIdx]!=null) {
            try {
                previewModel = new ContO(modelBytes[selectedModelIdx], medium);
                ContO atpModel = new ContO(modelBytes[selectedModelIdx], medium);
                ghostATP = atpModel.getAttachPoints();
                // Ensure clean starting position
                previewModel.x = 0; previewModel.y = 0; previewModel.z = 0; previewModel.xz = 0;
            }
            catch (Exception ignored) {}
        }
        rotLabel.setText(rot + "°");
        idLabel.setText("#" + modelFileId(selectedModelIdx));
        if (previewPanel != null) previewPanel.repaint();
    }

    private void rotate90() {
        rot = (rot + 90) % 360;
        if (rot == 270) rot = -90;
        rotLabel.setText(rot + "°");
        if (previewPanel != null) previewPanel.repaint();
        if (stageCanvas  != null) stageCanvas.repaint();
    }

    private void placePart(int wx, int wz) {
        if (modelBytes==null || selectedModelIdx>=modelBytes.length || modelBytes[selectedModelIdx]==null) return;
        int[] snapped = snapToNearestPiece(wx, wz);
        wx = snapped[0]; wz = snapped[1];
        pushUndo();
        ContO c = new ContO(modelBytes[selectedModelIdx], medium);
        int wy = Medium.ground - c.grat;
        c.x=wx; c.y=wy; c.z=wz; c.xz=rot;
        PlacedPart pp = new PlacedPart();
        pp.modelIdx=selectedModelIdx; pp.fileId=modelFileId(selectedModelIdx);
        pp.x=wx; pp.y=wy; pp.z=wz; pp.rot=rot;
        pp.type=partType(selectedModelIdx); pp.conto=c;
        ContO atpC = new ContO(modelBytes[selectedModelIdx], medium);
        pp.atp = atpC.getAttachPoints();
        stage.add(pp);
        stageCanvas.repaint();
    }

    private void removePart(int wx, int wz) {
        PlacedPart best=null; double bestD=Double.MAX_VALUE;
        for (PlacedPart pp : stage) {
            double d = Math.hypot(pp.x-wx, pp.z-wz);
            if (d<bestD) { bestD=d; best=pp; }
        }
        if (best!=null && bestD<SNAP) { pushUndo(); stage.remove(best); stageCanvas.repaint(); }
    }

    private static int partType(int idx) {
        if (idx == 30 || idx == 32) return 1;
        if (idx == 31) return 2;
        return 0;
    }

    // ── Undo ─────────────────────────────────────────────────────────────────
    private void pushUndo() { undoStack.push(buildBstage()); }

    private void doUndo() {
        if (undoStack.isEmpty()) return;
        String prev = undoStack.pop();
        stage.clear();
        parseBstage(prev);
        stageCanvas.repaint();
    }

    // ── Stage I/O ────────────────────────────────────────────────────────────
    private String buildHeader() {
        return "snap("   +snapV[0]+","+snapV[1]+","+snapV[2]+")\r\n"
             + "sky("    +skyV[0]+","+skyV[1]+","+skyV[2]+")\r\n"
             + "fog("    +fogV[0]+","+fogV[1]+","+fogV[2]+")\r\n"
             + "clouds(" +cloudsV[0]+","+cloudsV[1]+","+cloudsV[2]+","+cloudsV[3]+","+cloudsV[4]+")\r\n"
             + "ground(" +groundV[0]+","+groundV[1]+","+groundV[2]+")\r\n"
             + "texture("+texV[0]+","+texV[1]+","+texV[2]+","+texV[3]+")\r\n"
             + "fadefrom("+fadefrom+")\r\n"
             + "density("+density+")\r\n"
             + "mountains("+mountains+")\r\n"
             + "nlaps("+nlaps+")\r\n"
             + (stageName.isEmpty() ? "" : "name("+stageName+")\r\n")
             + "\r\n";
    }

    private String buildBstage() {
        StringBuilder sb = new StringBuilder();
        for (PlacedPart pp : stage) {
            switch (pp.type) {
                case 0: sb.append("set(").append(pp.fileId).append(',').append(pp.x).append(',').append(pp.z).append(',').append(pp.rot).append(")\r\n"); break;
                case 1: sb.append("chk(").append(pp.fileId).append(',').append(pp.x).append(',').append(pp.z).append(',').append(pp.rot).append(")\r\n"); break;
                case 2: sb.append("fix(").append(pp.fileId).append(',').append(pp.x).append(',').append(pp.z).append(',').append(pp.y).append(',').append(pp.rot).append(")\r\n"); break;
            }
        }
        return sb.toString();
    }

    private void parseBstage(String src) {
        if (modelBytes==null) return;
        for (String raw : src.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("//")) continue;
            if (line.contains(")")) line = line.substring(0, line.indexOf(')')+1);
            try {
                if      (line.startsWith("set(")) addPart(gi("set",line,0),gi("set",line,1),gi("set",line,2),gi("set",line,3),0);
                else if (line.startsWith("chk(")) addPart(gi("chk",line,0),gi("chk",line,1),gi("chk",line,2),gi("chk",line,3),1);
                else if (line.startsWith("fix(")) addPart(gi("fix",line,0),gi("fix",line,1),gi("fix",line,2),gi("fix",line,4),2);
            } catch (Exception ignored) {}
        }
    }

    private void addPart(int fid, int x, int z, int r, int type) {
        int idx = fileIdToIdx(fid);
        if (idx<0||idx>=modelBytes.length||modelBytes[idx]==null) return;
        ContO c = new ContO(modelBytes[idx], medium);
        int wy = Medium.ground - c.grat;
        c.x=x; c.y=wy; c.z=z; c.xz=r;
        PlacedPart pp = new PlacedPart();
        pp.modelIdx=idx; pp.fileId=fid; pp.x=x; pp.y=wy; pp.z=z; pp.rot=r; pp.type=type; pp.conto=c;
        ContO atpC = new ContO(modelBytes[idx], medium);
        pp.atp = atpC.getAttachPoints();
        stage.add(pp);
    }

    private void saveStage() {
        if (stageFile==null) {
            JFileChooser fc = new JFileChooser("data/stages");
            fc.setSelectedFile(new File("data/stages/untitled.txt"));
            if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
            stageFile = fc.getSelectedFile();
            if (!stageFile.getName().endsWith(".txt")) stageFile = new File(stageFile.getPath()+".txt");
            stageName = stageFile.getName().replace(".txt","");
        }
        try {
            stageFile.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(stageFile))) {
                bw.write(buildHeader()); bw.write(buildBstage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Save failed:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadStage() {
        if (stageFile==null||!stageFile.exists()) return;
        stage.clear(); undoStack.clear();
        try {
            String content = new String(Files.readAllBytes(stageFile.toPath()));
            parseHeader(content); parseBstage(content); stageCanvas.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Reload failed:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSourceEditor() {
        JTextArea ta = new JTextArea(buildHeader()+buildBstage(), 30, 70);
        ta.setFont(new Font("Monospaced",Font.PLAIN,12));
        int res = JOptionPane.showConfirmDialog(this, new JScrollPane(ta),
            "Edit Stage Source", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res==JOptionPane.OK_OPTION) {
            pushUndo();
            String src = ta.getText();
            parseHeader(src); stage.clear(); parseBstage(src); stageCanvas.repaint();
        }
    }

    private void showStageProperties() {
        JTextField nf  = new JTextField(stageName,20);
        JTextField lf  = new JTextField(String.valueOf(nlaps),5);
        JTextField sf  = new JTextField(skyV[0]+","+skyV[1]+","+skyV[2],12);
        JTextField gf  = new JTextField(groundV[0]+","+groundV[1]+","+groundV[2],12);
        JTextField ff  = new JTextField(fogV[0]+","+fogV[1]+","+fogV[2],12);
        JTextField mf  = new JTextField(String.valueOf(mountains),8);
        JTextField fdf = new JTextField(String.valueOf(fadefrom),8);
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Name:"));      p.add(nf);
        p.add(new JLabel("Laps:"));      p.add(lf);
        p.add(new JLabel("Sky r,g,b:")); p.add(sf);
        p.add(new JLabel("Ground:"));    p.add(gf);
        p.add(new JLabel("Fog:"));       p.add(ff);
        p.add(new JLabel("Mountains:")); p.add(mf);
        p.add(new JLabel("Fade From:")); p.add(fdf);
        if (JOptionPane.showConfirmDialog(this,p,"Stage Properties",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
            try {
                stageName = nf.getText().trim();
                nlaps     = Integer.parseInt(lf.getText().trim());
                mountains = Integer.parseInt(mf.getText().trim());
                fadefrom  = Integer.parseInt(fdf.getText().trim());
                int[] s=rgb(sf.getText()); if(s!=null){skyV=s;Medium.setSky(s[0],s[1],s[2]);}
                int[] g=rgb(gf.getText()); if(g!=null){groundV=g;Medium.setGround(g[0],g[1],g[2]);}
                int[] f=rgb(ff.getText()); if(f!=null) fogV=f;
                stageCanvas.repaint();
            } catch (Exception ignored) {}
        }
    }

    private void parseHeader(String src) {
        for (String raw : src.split("\\r?\\n")) {
            String line = raw.trim();
            try {
                if (line.startsWith("sky("))       { int[] v=new int[]{gi("sky",line,0),gi("sky",line,1),gi("sky",line,2)}; skyV=v; Medium.setSky(v[0],v[1],v[2]); }
                if (line.startsWith("ground("))    { int[] v=new int[]{gi("ground",line,0),gi("ground",line,1),gi("ground",line,2)}; groundV=v; Medium.setGround(v[0],v[1],v[2]); }
                if (line.startsWith("fog("))       { fogV=new int[]{gi("fog",line,0),gi("fog",line,1),gi("fog",line,2)}; }
                if (line.startsWith("nlaps("))     nlaps     = gi("nlaps",line,0);
                if (line.startsWith("mountains(")) mountains = gi("mountains",line,0);
                if (line.startsWith("fadefrom("))  fadefrom  = gi("fadefrom",line,0);
                if (line.startsWith("density("))   density   = gi("density",line,0);
                if (line.startsWith("name("))      stageName = line.substring(5, line.indexOf(')'));
                if (line.startsWith("snap("))      snapV = new int[]{gi("snap",line,0),gi("snap",line,1),gi("snap",line,2)};
            } catch (Exception ignored) {}
        }
    }

    private int[] rgb(String s) {
        try { String[] p=s.split(","); return new int[]{Integer.parseInt(p[0].trim()),Integer.parseInt(p[1].trim()),Integer.parseInt(p[2].trim())}; }
        catch(Exception e){return null;}
    }

    private static int gi(String name, String src, int pos) {
        int var=0; StringBuilder part = new StringBuilder();
        for (int k=name.length()+1; k<src.length(); k++) {
            char c = src.charAt(k);
            if (c==','||c==')') { var++; k++; if(k<src.length()) c=src.charAt(k); else break; }
            if (var==pos) part.append(c);
        }
        return Integer.parseInt(part.toString().trim());
    }

    // ── Coordinate helpers ───────────────────────────────────────────────────
    // Screen → world using zoom (matches original: (xm-505)*(|sy|/focusPoint)+sx)
    private int stw(int sx, int w) {
        return (sx - Medium.cx) * Math.abs(camY) / Medium.focusPoint + camX;
    }
    private int stz(int sz, int h) {
        return (Medium.cy - sz) * Math.abs(camY) / Medium.focusPoint + camZ;
    }
    // World → screen (inverse)
    private int wts(int wx, int w) {
        return (wx - camX) * Medium.focusPoint / Math.abs(camY) + Medium.cx;
    }
    private int wtz(int wz, int h) {
        return Medium.cy - (wz - camZ) * Medium.focusPoint / Math.abs(camY);
    }
    private int snap(int v)         { return Math.round((float)v / SNAP) * SNAP; }


    private int[] snapToNearestPiece(int wx, int wz) {
        if (!snapEnabled || previewModel == null) return new int[]{ wx, wz };
        int bestDist = 1400;
        int bestX = wx, bestZ = wz;
        if (ghostATP == null) return new int[]{ wx, wz };
        int[] gatp = ghostATP;
        int[] gx = { wx + gatp[0], wx + gatp[2] };
        int[] gz = { wz + gatp[1], wz + gatp[3] };
        rotAtp(gx, gz, wx, wz, rot, 2);
        for (PlacedPart pp : stage) {
            if (pp.conto == null) continue;
            int coarseDist = (int) Math.hypot(wx - pp.x, wz - pp.z);
            if (coarseDist > 8000) continue;
            int[] patp = pp.atp;
            if (patp == null) continue;
            int[] px = { pp.x + patp[0], pp.x + patp[2] };
            int[] pz = { pp.z + patp[1], pp.z + patp[3] };
            rotAtp(px, pz, pp.x, pp.z, pp.rot, 2);
            for (int gi = 0; gi < 2; gi++) {
                for (int pi = 0; pi < 2; pi++) {
                    int dx = px[pi] - gx[gi];
                    int dz = pz[pi] - gz[gi];
                    int d  = (int) Math.sqrt(dx*dx + dz*dz);
                    if (d < bestDist && d > 0) {
                        bestDist = d;
                        bestX = wx + dx;
                        bestZ = wz + dz;
                    }
                }
            }
        }
        return new int[]{ bestX, bestZ };
    }

    private void rotAtp(int[] xs, int[] zs, int cx, int cz, int deg, int n) {
        if (deg == 0) return;
        double rad = Math.toRadians(deg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        for (int i = 0; i < n; i++) {
            int ox = xs[i] - cx, oz = zs[i] - cz;
            xs[i] = cx + (int)(ox*cos - oz*sin);
            zs[i] = cz + (int)(ox*sin + oz*cos);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PreviewPanel
    // ══════════════════════════════════════════════════════════════════════
    private class PreviewPanel extends JPanel {
        PreviewPanel() { setBackground(new Color(205,205,205)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (previewModel==null) {
                g.setColor(Color.GRAY);
                g.setFont(new Font("Arial",Font.PLAIN,11));
                String msg = modelBytes==null ? "Loading models..." : "Model not available";
                g.drawString(msg, getWidth()/2-50, getHeight()/2);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pw=getWidth(), ph=getHeight();
            Medium.setViewport(0,0,pw,ph);
            Medium.w  = pw; Medium.h  = ph;
            Medium.cx = pw/2; Medium.cy = ph/2;
            Medium.cz = ph/8;
            Medium.trk= true;
            Medium.zy = 90;
            Medium.xz = 0;
            Medium.x  = -pw/2;
            Medium.z  = -ph/2;
            Medium.y  = -15000;
            Medium.crs= false;
            Medium.fogd = 3;
            Medium.fadeFrom(15000);
            previewModel.x  = pw/2;
            previewModel.y  = 0;
            previewModel.z  = ph/2;
            previewModel.xz = rot;
            previewModel.d(g2, -1, null);
        }
    }

    private void showPartContextMenu(PlacedPart target, int screenX, int screenY) {
        snapEnabled = false;
        selectedPart = target;
        stageCanvas.repaint();
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JButton editBtn   = new JButton("Edit");
        JButton removeBtn = new JButton("Remove");
        JButton copyBtn   = new JButton("Copy");

        for (JButton b : new JButton[]{editBtn, removeBtn, copyBtn}) {
            b.setFont(new Font("Arial", Font.PLAIN, 13));
            b.setFocusPainted(false);
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(120, 28));
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    b.setBackground(new Color(200, 210, 240));
                }
                @Override public void mouseExited(MouseEvent e) {
                    b.setBackground(UIManager.getColor("Button.background"));
                }
            });
        }
        

        // X close button
        JButton closeBtn = new JButton("x");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 11));
        closeBtn.setForeground(Color.RED);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.addActionListener(e -> menu.setVisible(false));

        editBtn.addActionListener(e -> {
            menu.setVisible(false);
            // Switch selected model to match this part
            selectedModelIdx = target.modelIdx;
            rot = target.rot;
            rotLabel.setText(rot + "°");
            // Rebuild ghost from this part's model
            if (modelBytes[selectedModelIdx] != null) {
                previewModel = new ContO(modelBytes[selectedModelIdx], medium);
                ContO atpModel = new ContO(modelBytes[selectedModelIdx], medium);
                ghostATP = atpModel.getAttachPoints();
            }
            // Remove the part
            pushUndo();
            stage.remove(target);
            stageCanvas.repaint();
        });

        removeBtn.addActionListener(e -> {
            menu.setVisible(false);
            pushUndo();
            stage.remove(target);
            stageCanvas.repaint();
        });

        copyBtn.addActionListener(e -> {
            menu.setVisible(false);
            // Switch selected model to match, leave part in place
            selectedModelIdx = target.modelIdx;
            rot = target.rot;
            rotLabel.setText(rot + "°");
            if (modelBytes[selectedModelIdx] != null) {
                previewModel = new ContO(modelBytes[selectedModelIdx], medium);
                ContO atpModel = new ContO(modelBytes[selectedModelIdx], medium);
                ghostATP = atpModel.getAttachPoints();
            }
            rebuildPreview();
            stageCanvas.repaint();
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(108, 18));
        closeBtn.setMargin(new Insets(0, 0, 0, 0));
        top.add(closeBtn, BorderLayout.EAST);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIManager.getColor("Panel.background"));
        content.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
        content.add(top);
        for (JButton b : new JButton[]{editBtn, removeBtn, copyBtn}) {
            b.setAlignmentX(CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(100, 26));
            b.setPreferredSize(new Dimension(100, 26));
            content.add(b);
            content.add(Box.createVerticalStrut(2));
        }

            menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                snapEnabled = true;
                selectedPart = null;
                stageCanvas.repaint();
            }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                snapEnabled = true;
                selectedPart = null;
                stageCanvas.repaint();
            }
        });

        menu.add(content);
        menu.show(stageCanvas, screenX, screenY);
    }

    // ══════════════════════════════════════════════════════════════════════
    // StageCanvas
    // ══════════════════════════════════════════════════════════════════════
    private class StageCanvas extends JPanel {
        private int ghostX, ghostZ;
        private boolean showGhost = false;
        private int dragSX, dragSY, dragCX, dragCZ;
        private boolean panning = false;

        private final Set<Integer> heldKeys = new HashSet<>();
        private javax.swing.Timer scrollTimer;

        private PlacedPart hoveredPart = null;

        StageCanvas() {
            setBackground(new Color(210,210,210));
            setFocusable(true);
            addMouseListener(new MouseAdapter(){
               @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int w = getWidth(), h = getHeight();
                int ex = e.getX(), ey = e.getY();
                    boolean pan = SwingUtilities.isMiddleMouseButton(e)
                               || (e.isAltDown()&&SwingUtilities.isLeftMouseButton(e));
                    if (pan) {
                        panning=true; dragSX=e.getX(); dragSY=e.getY(); dragCX=camX; dragCZ=camZ;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        int wx = stw(e.getX(), getWidth());
                        int wz = stz(e.getY(), getHeight());
                        PlacedPart target = findNearestPart(wx, wz);
                        if (target != null) {
                            showPartContextMenu(target, e.getX(), e.getY());
                        }
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        placePart(stw(e.getX(),getWidth()), stz(e.getY(),getHeight()));
                    }
                }
                @Override public void mouseReleased(MouseEvent e) { panning=false; }
                @Override public void mouseExited(MouseEvent e)   { showGhost=false; repaint(); }
            });
            addMouseMotionListener(new MouseMotionAdapter(){
                @Override public void mouseMoved(MouseEvent e) {
                    int wx = stw(e.getX(), getWidth());
                    int wz = stz(e.getY(), getHeight());
                    int[] snapped = snapToNearestPiece(wx, wz);
                    ghostX = snapped[0];
                    ghostZ = snapped[1];
                    hoveredPart = findNearestPart(wx, wz);
                    showGhost = true;
                    repaint();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (panning) {
                        camX = dragCX - (e.getX() - dragSX);
                        camZ = dragCZ + (e.getY() - dragSY);
                    } else {
                        int wx = stw(e.getX(), getWidth());
                        int wz = stz(e.getY(), getHeight());
                        int[] snapped = snapToNearestPiece(wx, wz);
                        ghostX = snapped[0];
                        ghostZ = snapped[1];
                        hoveredPart = findNearestPart(wx, wz);
                        showGhost = true;
                    }
                    repaint();
                }
            });
            addMouseWheelListener(e -> {
                camY = Math.max(-55000, Math.min(-2500, camY + e.getWheelRotation() * camY / 8));
                repaint();
            });
            scrollTimer = new javax.swing.Timer(16, ev -> {
            boolean moved = false;
            if (heldKeys.contains(KeyEvent.VK_UP))    { camZ += 300; moved = true; }
            if (heldKeys.contains(KeyEvent.VK_DOWN))  { camZ -= 300; moved = true; }
            if (heldKeys.contains(KeyEvent.VK_LEFT))  { camX -= 300; moved = true; }
            if (heldKeys.contains(KeyEvent.VK_RIGHT)) { camX += 300; moved = true; }
            if (moved) repaint();
        });
        scrollTimer.start();

        addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e) {
                heldKeys.add(e.getKeyCode());
                switch(e.getKeyCode()){
                    case KeyEvent.VK_PLUS: case KeyEvent.VK_ADD: case KeyEvent.VK_EQUALS:
                        camY = Math.min(-2500, camY + 500); repaint(); break;
                    case KeyEvent.VK_MINUS: case KeyEvent.VK_SUBTRACT:
                        camY = Math.max(-55000, camY - 500); repaint(); break;
                    case KeyEvent.VK_R: rotate90(); break;
                }
            }
            @Override public void keyReleased(KeyEvent e) { heldKeys.remove(e.getKeyCode()); }
        });
        }

        private void setupMedium(int w, int h) {
            Medium.setViewport(0, 0, w, h);
            Medium.w  = w;  Medium.h  = h;
            Medium.cx = w / 2;  Medium.cy = h / 2;
            Medium.cz = h / 8;  // add this
            Medium.trk = true;
            Medium.zy = 90;  Medium.xz = 0;
            Medium.x  = camX - Medium.cx;
            Medium.z  = camZ - Medium.cy;
            Medium.y  = camY;
            Medium.crs = false;
            Medium.fogd = 3;
            Medium.fadeFrom(25000);
        }

        private PlacedPart findNearestPart(int wx, int wz) {
            PlacedPart best = null;
            double bestD = Double.MAX_VALUE;
            for (PlacedPart pp : stage) {
                double d = Math.hypot(pp.x - wx, pp.z - wz);
                if (d < bestD) { bestD = d; best = pp; }
            }
            // Convert 20 pixels to world units based on current zoom
            int threshold = 45 * Math.abs(camY) / Medium.focusPoint;
            return (bestD < SNAP * 0.33) ? best : null;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            setupMedium(w, h);
            //drawGrid(g2,w,h);
            //drawOrigin(g2,w,h);
            drawParts(g2,w,h);
            if (showGhost) drawGhost(g2,w,h);
            //drawArrows(g2,w,h);
        }

        

        

        private void drawParts(Graphics2D g2, int w, int h) {
            for (PlacedPart pp : stage) {
                if (pp.conto == null) continue;
                pp.conto.x  = pp.x;
                pp.conto.y  = pp.y;
                pp.conto.z  = pp.z;
                pp.conto.xz = pp.rot;
                if (pp == selectedPart) {
                    Set<Integer> allPolys = new HashSet<>();
                    for (int i = 0; i < pp.conto.npl; i++) allPolys.add(i);
                    pp.conto.wholeHover = false;
                    pp.conto.d(g2, -1, allPolys);
                } else {
                    pp.conto.wholeHover = (pp == hoveredPart);
                    pp.conto.d(g2, -1, null);
                    pp.conto.wholeHover = false;
                }
            }
        }

        private void drawGhost(Graphics2D g2, int w, int h) {
            if (previewModel == null) return;
            previewModel.x  = ghostX;
            previewModel.y  = Medium.ground - previewModel.grat;
            previewModel.z  = ghostZ;
            previewModel.xz = rot;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            previewModel.d(g2, -1, null);
            g2.setComposite(old);
            // Reset position so no placed part shares these coordinates next frame
            previewModel.x  = 0;
            previewModel.y  = 0;
            previewModel.z  = 0;
            previewModel.xz = 0;
            int sx = wts(ghostX, w), sz = wtz(ghostZ, h);
            g2.setFont(new Font("Arial",Font.PLAIN,10));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(modelName(selectedModelIdx)+" | "+rot+"° | #"+modelFileId(selectedModelIdx), sx+8, sz-4);
        }

        private void drawArrows(Graphics2D g2, int w, int h) {
            arrow(g2, w/2, 16,    0);
            arrow(g2, w/2, h-16, 180);
            arrow(g2, 16,  h/2,  270);
            arrow(g2, w-16,h/2,   90);
        }

        private void arrow(Graphics2D g2, int cx, int cy, int deg) {
            double a = Math.toRadians(deg-90);
            int r = 14;
            int[] px = { cx+(int)(r*Math.cos(a)), cx+(int)(r*Math.cos(a+Math.toRadians(130))), cx+(int)(r*Math.cos(a-Math.toRadians(130))) };
            int[] pz = { cy+(int)(r*Math.sin(a)), cy+(int)(r*Math.sin(a+Math.toRadians(130))), cy+(int)(r*Math.sin(a-Math.toRadians(130))) };
            g2.setColor(new Color(170,175,190,210));
            g2.fillPolygon(px,pz,3);
            g2.setColor(new Color(120,125,145));
            g2.setStroke(new BasicStroke(1f));
            g2.drawPolygon(px,pz,3);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PlacedPart
    // ══════════════════════════════════════════════════════════════════════
    private static class PlacedPart {
        int modelIdx, fileId, x, y, z, rot, type;
        ContO conto;
        int[] atp;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Standalone test
    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("Stage Maker");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setContentPane(new StageMakerPanel());
                f.setSize(1280, 768);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }
}