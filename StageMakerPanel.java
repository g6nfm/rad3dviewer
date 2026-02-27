import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * StageMakerPanel — replicates the NFMM Stage Maker "Build" tab UI.
 *
 * LEFT   : Part Selection dropdown, part name dropdown, 3D part preview,
 *          rotation label (top-left) + file-id label (top-right),
 *          < Rotate >, action buttons, memory bar
 * CENTER : top-down stage canvas with nav arrows, pan/zoom
 *
 * File format:
 *   trackModels[0]="road" → file-id 10  (index + 10)
 *   extraModels[0]="offroadtakeoff" → file-id 49  (index + 49)
 *   set(id,x,z,rot)  chk(id,x,z,rot)  fix(id,x,z,yoff,rot)
 *   Suffix flags (p/r/t/s/o) are stripped on load, not written.
 *
 * Models: data/stages/models.radq (standard zip containing *.rad files)
 */
public class StageMakerPanel extends JPanel {

    // ── Part tables ─────────────────────────────────────────────────────────
    private static final String[] TRACK_MODELS = {
        "road","froad","twister2","twister1","turn","offroad","bumproad","offturn",
        "nroad","nturn","roblend","noblend","rnblend","roadend","offroadend","hpground",
        "ramp30","cramp35","dramp15","dhilo15","slide10","takeoff","sramp22","offbump",
        "offramp","sofframp","halfpipe","spikes","rail","thewall","checkpoint","fixpoint",
        "offcheckpoint","sideoff","bsideoff","uprise","riseroad","sroad","soffroad"
    };
    // extraModels[0] → file-id 49
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
        "Roads","Ramps","Obstacles","Checkpoints","Fix Hoops","Extra / Custom"
    };
    // track-model index ranges per category
    private static final int[][] CAT_RANGES = {
        {0, 15},  // Roads
        {16,25},  // Ramps
        {26,29},  // Obstacles
        {30,30},  // Checkpoints
        {31,31},  // Fix Hoops
        {32,38}   // soffroad group (last of track + all extra handled separately)
    };

    // ── Model data ──────────────────────────────────────────────────────────
    private byte[][] modelBytes; // index: 0..38 = track, 39..73 = extra

    // ── Preview ─────────────────────────────────────────────────────────────
    private ContO previewModel;
    private int   selectedModelIdx = 0;
    private int   selectedCatIdx   = 0;

    // ── Stage data ──────────────────────────────────────────────────────────
    private final java.util.List<PlacedPart> stage     = new ArrayList<PlacedPart>();
    private final Deque<String>              undoStack = new ArrayDeque<String>();

    // ── Stage header ────────────────────────────────────────────────────────
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

    // ── Current rotation (0/90/180/-90) ────────────────────────────────────
    private int rot = 0;

    // ── Renderer ────────────────────────────────────────────────────────────
    private final Medium medium = new Medium();

    // ── Camera ──────────────────────────────────────────────────────────────
    private int camX = 0, camZ = 1500;
    private int camY = -15000;   // more negative = zoom out

    // ── Remove mode ─────────────────────────────────────────────────────────
    private boolean removeMode = false;

    // ── UI refs ─────────────────────────────────────────────────────────────
    private JComboBox<String> catCombo;
    private JComboBox<String> partCombo;
    private PreviewPanel      previewPanel;
    private JLabel            rotLabel;
    private JLabel            idLabel;
    private StageCanvas       stageCanvas;

    // ══════════════════════════════════════════════════════════════════════
    // Constructor
    // ══════════════════════════════════════════════════════════════════════
    public StageMakerPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(220,220,220));
        buildUI();
        loadModelsAsync();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildUI() {
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildBottomBar(), BorderLayout.SOUTH);

        JPanel main = new JPanel(new BorderLayout(0,0));
        main.setBackground(new Color(220,220,220));
        main.add(buildLeftPanel(), BorderLayout.WEST);
        stageCanvas = new StageCanvas();
        main.add(stageCanvas, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        // Global keyboard shortcuts
        InputMap  im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),"undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),"save");
        am.put("undo", new AbstractAction(){public void actionPerformed(ActionEvent e){doUndo();}});
        am.put("save", new AbstractAction(){public void actionPerformed(ActionEvent e){saveStage();}});
    }

    // ── Top bar: tabs + save buttons ────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(195,195,195));
        bar.setBorder(BorderFactory.createMatteBorder(0,0,2,0,Color.DARK_GRAY));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        tabs.setOpaque(false);
        String[] tabNames = {"Stage","Build","View & Edit","Publish"};
        for (int i = 0; i < tabNames.length; i++) {
            JButton tb = new JButton(tabNames[i]);
            tb.setFont(new Font("Arial",Font.BOLD,13));
            tb.setFocusPainted(false);
            tb.setBackground(i==1 ? new Color(220,220,220) : new Color(175,175,175));
            tb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2,2,0,2,Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(4,14,4,14)));
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

    // ── Left panel ──────────────────────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(new Color(218,218,218));
        left.setPreferredSize(new Dimension(360,0));
        left.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // "Part Selection"
        JLabel ps = new JLabel("Part Selection");
        ps.setFont(new Font("Arial",Font.BOLD,11));
        ps.setAlignmentX(LEFT_ALIGNMENT);
        left.add(ps);
        left.add(Box.createVerticalStrut(3));

        // Category row
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

        // Part name combo
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

        // Preview container (absolute layout for rot/id overlay)
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

        // < Rotate >
        JPanel rotRow = new JPanel(new FlowLayout(FlowLayout.CENTER,10,0));
        rotRow.setOpaque(false); rotRow.setAlignmentX(LEFT_ALIGNMENT);
        JButton prev = new JButton("<");
        JButton rotB = new JButton("  Rotate  ");
        JButton next = new JButton(">");
        for (JButton b : new JButton[]{prev,rotB,next}) b.setFont(new Font("Arial",Font.BOLD,13));
        prev.addActionListener(e -> { int li = (partCombo.getSelectedIndex()-1+partCombo.getItemCount())%partCombo.getItemCount(); partCombo.setSelectedIndex(li); });
        next.addActionListener(e -> { int li = (partCombo.getSelectedIndex()+1)%partCombo.getItemCount();                          partCombo.setSelectedIndex(li); });
        rotB.addActionListener(e -> rotate90());
        rotRow.add(prev); rotRow.add(rotB); rotRow.add(next);
        left.add(rotRow);
        left.add(Box.createVerticalStrut(10));

        // 2-column action grid
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

        // Wide buttons
        JButton removeBtn = wideBtn("Remove / Edit Part");
        JButton gotoBtn   = wideBtn("Go to >  Startline");
        removeBtn.addActionListener(e -> toggleRemoveMode());
        gotoBtn  .addActionListener(e -> { camX=0; camZ=1500; stageCanvas.repaint(); });
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

    // ── Bottom memory bar ────────────────────────────────────────────────────
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(210,210,210));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.GRAY));
        bar.setPreferredSize(new Dimension(0,22));

        JLabel mc = new JLabel("  Memory Consumption : ");
        mc.setFont(new Font("Arial",Font.BOLD,11));
        bar.add(mc, BorderLayout.WEST);

        // green bar + label
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

    // ══════════════════════════════════════════════════════════════════════
    // Part combo
    // ══════════════════════════════════════════════════════════════════════
    private void rebuildPartCombo() {
        partCombo.removeAllItems();
        int cat = selectedCatIdx;
        if (cat < CAT_RANGES.length) {
            for (int i = CAT_RANGES[cat][0]; i <= CAT_RANGES[cat][1]; i++)
                partCombo.addItem(TRACK_MODELS[i]);
        } else {
            for (String s : EXTRA_MODELS) partCombo.addItem(s);
        }
        if (partCombo.getItemCount() > 0) partCombo.setSelectedIndex(0);
    }

    /** flat model index from category + list position */
    private int catToModelIdx(int cat, int listPos) {
        if (cat < CAT_RANGES.length) return CAT_RANGES[cat][0] + listPos;
        return TRACK_MODELS.length + listPos;
    }

    private String modelName(int idx) {
        return idx < TRACK_MODELS.length ? TRACK_MODELS[idx] : EXTRA_MODELS[idx - TRACK_MODELS.length];
    }

    /** file ID: track[0]=10, extra[0]=49 → both are just idx+10 since extra starts at index 39 (39+10=49) */
    private int modelFileId(int idx) { return idx + 10; }
    private int fileIdToIdx(int fid) { return fid - 10; }

    // ══════════════════════════════════════════════════════════════════════
    // Model loading
    // ══════════════════════════════════════════════════════════════════════
    private void loadModelsAsync() {
        SwingWorker<byte[][], Void> w = new SwingWorker<byte[][], Void>() {
            @Override protected byte[][] doInBackground() throws Exception { return loadZip(); }
            @Override protected void done() {
                try {
                    modelBytes = get();
                    rebuildPartCombo();
                    rebuildPreview();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StageMakerPanel.this,
                        "Could not load models:\n" + ex.getMessage() +
                        "\n\nPlace models.radq in data/stages/",
                        "Model Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private byte[][] loadZip() throws IOException {
        int total = TRACK_MODELS.length + EXTRA_MODELS.length;
        byte[][] r = new byte[total][];
        Map<String,Integer> map = new HashMap<String,Integer>();
        for (int i=0;i<TRACK_MODELS.length;i++) map.put(TRACK_MODELS[i].toLowerCase(),i);
        for (int i=0;i<EXTRA_MODELS.length; i++) map.put(EXTRA_MODELS[i].toLowerCase(), TRACK_MODELS.length+i);
        File zip = new File("data/stages/models.radq");
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

    // ══════════════════════════════════════════════════════════════════════
    // Preview
    // ══════════════════════════════════════════════════════════════════════
    private void rebuildPreview() {
        previewModel = null;
        if (modelBytes!=null && selectedModelIdx<modelBytes.length && modelBytes[selectedModelIdx]!=null) {
            try { previewModel = new ContO(modelBytes[selectedModelIdx], medium); }
            catch (Exception ignored) {}
        }
        rotLabel.setText(rot + "°");
        idLabel .setText("#" + modelFileId(selectedModelIdx));
        if (previewPanel != null) previewPanel.repaint();
    }

    private void rotate90() {
        rot = (rot + 90) % 360;
        rotLabel.setText(rot + "°");
        if (previewPanel != null) previewPanel.repaint();
        if (stageCanvas  != null) stageCanvas.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Placement / removal
    // ══════════════════════════════════════════════════════════════════════
    private void placePart(int wx, int wz) {
        if (modelBytes==null || selectedModelIdx>=modelBytes.length || modelBytes[selectedModelIdx]==null) return;
        pushUndo();
        ContO c = new ContO(modelBytes[selectedModelIdx], medium);
        int wy = Medium.ground - c.grat;
        c.x=wx; c.y=wy; c.z=wz; c.xz=rot;
        PlacedPart pp = new PlacedPart();
        pp.modelIdx=selectedModelIdx; pp.fileId=modelFileId(selectedModelIdx);
        pp.x=wx; pp.y=wy; pp.z=wz; pp.rot=rot;
        pp.type=partType(selectedModelIdx); pp.conto=c;
        stage.add(pp);
        stageCanvas.repaint();
    }

    private void removePart(int wx, int wz) {
        PlacedPart best=null; double bestD=Double.MAX_VALUE;
        for (PlacedPart pp : stage) {
            double d = Math.hypot(pp.x-wx, pp.z-wz);
            if (d<bestD) { bestD=d; best=pp; }
        }
        if (best!=null && bestD<4000) { pushUndo(); stage.remove(best); stageCanvas.repaint(); }
    }

    private static int partType(int idx) {
        if (idx==30||idx==32||idx==TRACK_MODELS.length+8) return 1; // chk
        if (idx==31) return 2; // fix
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Undo
    // ══════════════════════════════════════════════════════════════════════
    private void pushUndo() { undoStack.push(buildBstage()); }

    private void doUndo() {
        if (undoStack.isEmpty()) return;
        String prev = undoStack.pop();
        stage.clear();
        parseBstage(prev);
        stageCanvas.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Stage I/O
    // ══════════════════════════════════════════════════════════════════════
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
            // strip suffix flags and comments
            String line = raw.trim();
            if (line.startsWith("//")) continue;
            if (line.contains(")")) line = line.substring(0, line.indexOf(')')+1);
            try {
                if      (line.startsWith("set(")) addPart(gi("set",line,0), gi("set",line,1), gi("set",line,2), gi("set",line,3), 0);
                else if (line.startsWith("chk(")) addPart(gi("chk",line,0), gi("chk",line,1), gi("chk",line,2), gi("chk",line,3), 1);
                else if (line.startsWith("fix(")) addPart(gi("fix",line,0), gi("fix",line,1), gi("fix",line,2), gi("fix",line,4), 2);
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
        stage.add(pp);
    }

    private void saveStage() {
        if (stageFile==null) {
            JFileChooser fc = new JFileChooser("mystages");
            fc.setSelectedFile(new File("mystages/untitled.txt"));
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
                if (line.startsWith("sky("))       { int[] v=rgb3("sky",line);    if(v!=null){skyV=v;Medium.setSky(v[0],v[1],v[2]);}}
                if (line.startsWith("ground("))    { int[] v=rgb3("ground",line); if(v!=null){groundV=v;Medium.setGround(v[0],v[1],v[2]);}}
                if (line.startsWith("fog("))       { int[] v=rgb3("fog",line);    if(v!=null) fogV=v; }
                if (line.startsWith("nlaps("))     nlaps     = gi("nlaps",line,0);
                if (line.startsWith("mountains(")) mountains = gi("mountains",line,0);
                if (line.startsWith("fadefrom("))  fadefrom  = gi("fadefrom",line,0);
                if (line.startsWith("density("))   density   = gi("density",line,0);
                if (line.startsWith("name("))      stageName = line.substring(5, line.indexOf(')'));
                if (line.startsWith("snap("))      snapV = new int[]{gi("snap",line,0),gi("snap",line,1),gi("snap",line,2)};
            } catch (Exception ignored) {}
        }
    }

    private int[] rgb3(String name, String line) { return new int[]{gi(name,line,0),gi(name,line,1),gi(name,line,2)}; }
    private int[] rgb(String s) { try { String[] p=s.split(","); return new int[]{Integer.parseInt(p[0].trim()),Integer.parseInt(p[1].trim()),Integer.parseInt(p[2].trim())}; } catch(Exception e){return null;} }

    /** Parse the nth comma-separated integer from "name(v0,v1,...)" */
    private static int gi(String name, String src, int pos) {
        int var=0; StringBuilder part = new StringBuilder();
        for (int k=name.length()+1; k<src.length(); k++) {
            char c = src.charAt(k);
            if (c==','||c==')') { var++; k++; if(k<src.length()) c=src.charAt(k); else break; }
            if (var==pos) part.append(c);
        }
        return Integer.parseInt(part.toString().trim());
    }

    // ══════════════════════════════════════════════════════════════════════
    // Coordinate helpers
    // ══════════════════════════════════════════════════════════════════════
    private float scale()                      { return 200000f / Math.abs(camY); }
    private int stw(int sx, int cw)            { return (int)((sx - cw/2f) / scale()) + camX; }
    private int stz(int sy, int ch)            { return (int)((sy - ch/2f) / scale()) + camZ; }
    private int wts(int wx, int cw)            { return (int)((wx - camX) * scale()) + cw/2;  }
    private int wtz(int wz, int ch)            { return (int)((wz - camZ) * scale()) + ch/2;  }

    // ══════════════════════════════════════════════════════════════════════
    // PreviewPanel — renders bco[selectedPart] at fixed camera
    // ══════════════════════════════════════════════════════════════════════
    private class PreviewPanel extends JPanel {
        PreviewPanel() { setBackground(new Color(205,205,205)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (previewModel==null) {
                g.setColor(Color.GRAY); g.setFont(new Font("Arial",Font.PLAIN,11));
                String msg = modelBytes==null ? "Loading models..." : "Model not available";
                g.drawString(msg, getWidth()/2-50, getHeight()/2);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pw=getWidth(), ph=getHeight();

            // Mirror NFMM preview camera exactly
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

    // ══════════════════════════════════════════════════════════════════════
    // StageCanvas — top-down build view
    // ══════════════════════════════════════════════════════════════════════
    private class StageCanvas extends JPanel {
        private int ghostX, ghostZ;
        private boolean showGhost = false;
        private int dragSX, dragSY, dragCX, dragCZ;
        private boolean panning = false;

        StageCanvas() {
            setBackground(new Color(210,210,210));
            setFocusable(true);
            addMouseListener(new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    boolean pan = SwingUtilities.isMiddleMouseButton(e)
                               || (e.isAltDown()&&SwingUtilities.isLeftMouseButton(e));
                    if (pan) {
                        panning=true; dragSX=e.getX(); dragSY=e.getY(); dragCX=camX; dragCZ=camZ;
                    } else if (SwingUtilities.isRightMouseButton(e)||removeMode) {
                        removePart(stw(e.getX(),getWidth()), stz(e.getY(),getHeight()));
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        placePart(stw(e.getX(),getWidth()), stz(e.getY(),getHeight()));
                    }
                }
                @Override public void mouseReleased(MouseEvent e) { panning=false; }
                @Override public void mouseExited(MouseEvent e)   { showGhost=false; repaint(); }
            });
            addMouseMotionListener(new MouseMotionAdapter(){
                @Override public void mouseMoved(MouseEvent e) {
                    ghostX=stw(e.getX(),getWidth()); ghostZ=stz(e.getY(),getHeight());
                    showGhost=true; repaint();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (panning) {
                        camX=dragCX-(int)((e.getX()-dragSX)/scale());
                        camZ=dragCZ-(int)((e.getY()-dragSY)/scale());
                    } else {
                        ghostX=stw(e.getX(),getWidth()); ghostZ=stz(e.getY(),getHeight());
                        showGhost=true;
                    }
                    repaint();
                }
            });
            addMouseWheelListener(e -> {
                camY = Math.max(-55000, Math.min(-2500, camY + e.getWheelRotation()*2000));
                repaint();
            });
            addKeyListener(new KeyAdapter(){
                @Override public void keyPressed(KeyEvent e) {
                    int step = Math.max(500,(int)(2000/scale()));
                    switch(e.getKeyCode()){
                        case KeyEvent.VK_UP:    camZ-=step; break;
                        case KeyEvent.VK_DOWN:  camZ+=step; break;
                        case KeyEvent.VK_LEFT:  camX-=step; break;
                        case KeyEvent.VK_RIGHT: camX+=step; break;
                        case KeyEvent.VK_PLUS: case KeyEvent.VK_ADD: case KeyEvent.VK_EQUALS:
                            camY=Math.min(-2500,camY+2000); break;
                        case KeyEvent.VK_MINUS: case KeyEvent.VK_SUBTRACT:
                            camY=Math.max(-55000,camY-2000); break;
                        case KeyEvent.VK_R: rotate90(); break;
                    }
                    repaint();
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            drawGrid(g2,w,h);
            drawOrigin(g2,w,h);
            drawParts(g2,w,h);
            if (showGhost) drawGhost(g2,w,h);
            drawArrows(g2,w,h);
        }

        private void drawGrid(Graphics2D g2,int w,int h) {
            float sc=scale();
            int step=Math.max(6,(int)(5600*sc));
            g2.setColor(new Color(198,198,198));
            int ox=(int)(((-camX*sc)%step+step)%step);
            int oz=(int)(((-camZ*sc)%step+step)%step);
            for(int x=ox;x<w;x+=step) g2.drawLine(x,0,x,h);
            for(int z=oz;z<h;z+=step) g2.drawLine(0,z,w,z);
        }

        private void drawOrigin(Graphics2D g2,int w,int h) {
            int ox=wts(0,w), oz=wtz(0,h);
            g2.setColor(new Color(140,140,140));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(ox-20,oz,ox+20,oz);
            g2.drawLine(ox,oz-20,ox,oz+20);
        }

        private void drawParts(Graphics2D g2,int w,int h) {
            float sc=scale();
            for (PlacedPart pp : stage) {
                int sx=wts(pp.x,w), sz=wtz(pp.z,h);
                int hw=Math.max(3,(int)(1400*sc)), hd=Math.max(2,(int)(2800*sc));
                double rad=Math.toRadians(pp.rot);
                int[] px=new int[4], pz=new int[4];
                int[][] corners={{-hw,-hd},{hw,-hd},{hw,hd},{-hw,hd}};
                for(int i=0;i<4;i++){
                    px[i]=sx+(int)(corners[i][0]*Math.cos(rad)-corners[i][1]*Math.sin(rad));
                    pz[i]=sz+(int)(corners[i][0]*Math.sin(rad)+corners[i][1]*Math.cos(rad));
                }
                Color fill = pp.type==1 ? new Color(80,150,255,170)
                           : pp.type==2 ? new Color(255,200,40,170)
                           :               new Color(165,165,165,210);
                g2.setColor(fill);
                g2.fillPolygon(px,pz,4);
                g2.setColor(fill.darker());
                g2.setStroke(new BasicStroke(1f));
                g2.drawPolygon(px,pz,4);
                if (sc>0.014f) {
                    g2.setFont(new Font("Arial",Font.PLAIN,9));
                    g2.setColor(Color.DARK_GRAY);
                    String nm=modelName(pp.modelIdx);
                    FontMetrics fm=g2.getFontMetrics();
                    g2.drawString(nm, sx-fm.stringWidth(nm)/2, sz+fm.getAscent()/2-1);
                }
            }
        }

        private void drawGhost(Graphics2D g2,int w,int h) {
            if (modelBytes==null||selectedModelIdx>=modelBytes.length||modelBytes[selectedModelIdx]==null) return;
            float sc=scale();
            int sx=wts(ghostX,w), sz=wtz(ghostZ,h);
            int hw=Math.max(3,(int)(1400*sc)), hd=Math.max(2,(int)(2800*sc));
            double rad=Math.toRadians(rot);
            int[] px=new int[4], pz=new int[4];
            int[][] corners={{-hw,-hd},{hw,-hd},{hw,hd},{-hw,hd}};
            for(int i=0;i<4;i++){
                px[i]=sx+(int)(corners[i][0]*Math.cos(rad)-corners[i][1]*Math.sin(rad));
                pz[i]=sz+(int)(corners[i][0]*Math.sin(rad)+corners[i][1]*Math.cos(rad));
            }
            g2.setColor(removeMode ? new Color(220,60,60,80) : new Color(100,100,100,80));
            g2.fillPolygon(px,pz,4);
            g2.setColor(removeMode ? new Color(200,50,50,180) : new Color(80,80,80,160));
            float[] dash={4,4};
            g2.setStroke(new BasicStroke(1f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,dash,0));
            g2.drawPolygon(px,pz,4);
            // label
            g2.setFont(new Font("Arial",Font.PLAIN,10));
            g2.setColor(Color.DARK_GRAY);
            String lbl = modelName(selectedModelIdx)+" | "+rot+"° | #"+modelFileId(selectedModelIdx);
            g2.drawString(lbl, sx+hw+6, sz+4);
        }

        /** Triangular nav arrows at canvas edges (like original) */
        private void drawArrows(Graphics2D g2,int w,int h) {
            arrow(g2, w/2, 16,  0);   // up
            arrow(g2, w/2, h-16, 180); // down
            arrow(g2, 16, h/2, 270);  // left
            arrow(g2, w-16, h/2, 90); // right
        }

        private void arrow(Graphics2D g2,int cx,int cy,int deg) {
            double a = Math.toRadians(deg-90);
            int r = 14;
            int[] px = {
                cx + (int)(r   * Math.cos(a)),
                cx + (int)(r   * Math.cos(a + Math.toRadians(130))),
                cx + (int)(r   * Math.cos(a - Math.toRadians(130)))
            };
            int[] pz = {
                cy + (int)(r   * Math.sin(a)),
                cy + (int)(r   * Math.sin(a + Math.toRadians(130))),
                cy + (int)(r   * Math.sin(a - Math.toRadians(130)))
            };
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
    }

    // ══════════════════════════════════════════════════════════════════════
    // Standalone test entry point
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
