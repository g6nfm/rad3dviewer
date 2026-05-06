


import java.awt.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.List;

/**
 * models
 *
 * @author Omar Waly, Kaffeinated
 */
public class ContO {
    public final Plane[] p;
    private final Medium m;
    public int npl = 0;
    public int x = 0;
    public int y = 0;
    public int z = 0;
    public int xz = 0;
    public int xy = 0;
    public int zy = 0;
    int[] wx = new int[8];
    int[] pwx = new int[8];
    int[] wy = new int[8];
    int[] wz = new int[8];
    public int wxz = 0;
    public int wzy = 0;
    public int maxR = 0;
    private int disline = 7;
    private boolean shadow = false;
    private boolean noline = false;
    public int grat = 0;

    private int[] cachedATP = null;
    public boolean wholeHover = false;

    public Color hoverColor    = new Color(0, 0, 255, 60);
    public Color selectedColor = new Color(0, 80, 255, 80);

    public final int[] keyx = new int[8];
    public final int[] keyz = new int[8];

    boolean[] isCustomWheel = new boolean[8];

    SimpleColor pendingRim0, pendingRim1, pendingRim2, pendingRim3;

    public SimpleColor[] wheelRimColorOriginal = new SimpleColor[8];
    public SimpleColor[] wheelRimColor1 = new SimpleColor[8];
    public SimpleColor[] wheelRimColor2 = new SimpleColor[8];
    public SimpleColor[] wheelRimColor3 = new SimpleColor[8];

    public int wheelCount = 0;

    public static class WheelAnchor {
        public int x, y, z;
        public int rotates;
        public int width;
        public int size;
        public int modelId;
        public boolean custom;
        public int gwgr;

        public int startIndex;   // first poly index for this wheel
        public int polyCount;    // how many polys this wheel uses
    }

    public final WheelAnchor[] anchors = new WheelAnchor[8];

    

    Wheels wheels = new Wheels();


    //Skins

    ArrayList<SimpleColor> original = new ArrayList<>();
    ArrayList<SimpleColor> skin1 = new ArrayList<>();
    ArrayList<SimpleColor> skin2 = new ArrayList<>();
    ArrayList<SimpleColor> skin3 = new ArrayList<>();

    HashMap<Integer, ArrayList<SimpleColor>> skinMap = new HashMap<>();

    
    

    class TempPoly {
            int[] ox, oy, oz;
            int n;
            SimpleColor c, c1, c2, c3;
            boolean noOutline;
            int gr, fs;
        }

        boolean hasWheelModels = false;
        Map<Integer, List<TempPoly>> wheelModels = new HashMap<>();

        private void makeCustomWheel(
            int nplStart,
            int wxv, int wyv, int wzv,
            int rotates,
            List<TempPoly> model,
            int wheelIndex,
            int width,
            int size,
            int gwgr    // ADD THIS
        ){
    //boolean mirror = (wheelIndex % 2 == 1);

    for (TempPoly poly : model) {

        int[] ox = new int[poly.n];
        int[] oy = new int[poly.n];
        int[] oz = new int[poly.n];

        float scale  = size  / 20f;
        float wscale = width / 20f;

        for (int v = 0; v < poly.n; v++) {

            int lx = poly.ox[v];
            int ly = poly.oy[v];
            int lz = poly.oz[v];

            lx *= wscale;
            ly *= scale;
            lz *= scale;

            ox[v] = (int)(lx) + wxv;
            oy[v] = (int)(ly) + wyv;
            oz[v] = (int)(lz) + wzv;

            if (oy[v] > wheels.ground)
                wheels.ground = oy[v];
        }

        // --- COLOR ARRAYS JUST LIKE OLD SYSTEM ---
        SimpleColor c  = poly.c;
        SimpleColor c1 = poly.c1;
        SimpleColor c2 = poly.c2;
        SimpleColor c3 = poly.c3;

        int[] col = { c.r, c.g, c.b };

        // --- HANDLE ROTATION: ONLY FRONT WHEELS TURN ---
        int px = (wheelIndex <= 1) ? wxv : 0;   // <= front wheels
        int pr = (wheelIndex <= 1) ? rotates : 0;

        int finalGr = poly.gr + gwgr;
        finalGr = Math.max(-40, Math.min(40, finalGr));

        p[nplStart] = new Plane(
            m,
            ox, oz, oy,
            poly.n,
            col,
            false,
            finalGr,
            poly.fs,
            px, wyv, wzv,   // pivot X *controls* rotation
            disline,
            pr,             // rotates only front wheels
            false,
            0,
            poly.noOutline
        );

        // --- MATCH OLD BEHAVIOR: ADD COLORS TO GLOBAL LISTS ---
        original.add(c);
        skin1.add(c1);
        skin2.add(c2);
        skin3.add(c3);  // ADD THIS
        nplStart++;
    }
}





    public ContO(byte abyte0[], Medium medium) {
        p = new Plane[2000];
        m = medium;
        boolean inWheelModel = false;
        int currentWheelModelID = -1;

        boolean inWheelPoly = false;
        int tmpVerts = 0;
        int[] tmpOx = new int[100];
        int[] tmpOy = new int[100];
        int[] tmpOz = new int[100];
        SimpleColor tmpC = null, tmpC1 = null, tmpC2 = null, tmpC3 = null;
        boolean tmpNoOutline = false;
        int tmpGr = 1, tmpFs = 0;

        int[] fsAuto = new int[2000];
        boolean hasWheels = false;
        int roofat = 0;
        
        boolean flag = false;
        boolean flag1 = false;
        int nverts = 0;
        float div = 1.0F; // was 1
        float wid = 1.0F;
        int ox[] = new int[100];
        int oy[] = new int[100];
        int oz[] = new int[100];
        int pcolor[] = new int[3];
        boolean glass = false;
        boolean road = false;
        boolean noOutline = false;
        
        int j = 0;
        int gr = 1;
        int fs = 0;
        int gwgr = 0;
        byte light = 0;

        

        float nfmm_scale[] = {
                1.0F, 1.0F, 1.0F
        };


        SimpleColor pendingOrigColor = null;
        SimpleColor pendingSkin1Color = null;
        SimpleColor pendingSkin2Color = null;
        SimpleColor pendingSkin3Color = null;

        try (BufferedReader bufferedreader = new BufferedReader(
                new InputStreamReader((new ByteArrayInputStream(abyte0))))) {
            for (String line; (line = bufferedreader.readLine()) != null; ) {
                line = line.trim();
                 // ---- CORRECT wheelModel open ----
                if (line.startsWith("<wheelModel(") && line.endsWith(">")) {
                    hasWheelModels = true;
                    inWheelModel = true;
                    int start = line.indexOf('(') + 1;
                    int end   = line.indexOf(')');
                    currentWheelModelID = Integer.parseInt(line.substring(start, end).trim());
                    wheelModels.put(currentWheelModelID, new ArrayList<>());
                    continue;
                }
                // ---- CORRECT wheelModel close ----
                if (line.equals("</wheelModel>")) {

                    // print BEFORE resetting
                    System.out.println(
                        "Loaded wheelModel " + currentWheelModelID +
                        " polys = " + wheelModels.get(currentWheelModelID).size()
                    );

                    inWheelModel = false;
                    currentWheelModelID = -1;
                    continue;
                }
                if (inWheelModel && line.startsWith("[p]")) {
                    inWheelPoly = true;
                    tmpVerts = 0;
                    tmpNoOutline = false;
                    tmpGr = 1;
                    tmpFs = 0;
                    tmpC = tmpC1 = tmpC2 = tmpC3 = null;
                    continue;
                }

                if (inWheelModel && line.startsWith("[/p]")) {
                    inWheelPoly = false;

                    TempPoly poly = new TempPoly();
                    poly.n = tmpVerts;
                    poly.ox = Arrays.copyOf(tmpOx, tmpVerts);
                    poly.oy = Arrays.copyOf(tmpOy, tmpVerts);
                    poly.oz = Arrays.copyOf(tmpOz, tmpVerts);
                    poly.c = tmpC;
                    poly.c1 = tmpC1;
                    poly.c2 = tmpC2;
                    poly.c3 = tmpC3;
                    poly.noOutline = tmpNoOutline;
                    poly.gr = tmpGr;
                    poly.fs = tmpFs;

                    original.add(tmpC);
                    skin1.add(tmpC1);
                    skin2.add(tmpC2);
                    skin3.add(tmpC3);

                    wheelModels.get(currentWheelModelID).add(poly);
                    continue;
                }
                // ----- STEP 3: Parse contents of a wheel polygon -----
                if (inWheelModel && inWheelPoly) {

                    if (line.startsWith("c(")) {
                        tmpC = new SimpleColor(
                            Utility.getint("c", line, 0),
                            Utility.getint("c", line, 1),
                            Utility.getint("c", line, 2)
                        );
                        continue;
                    }

                    if (line.startsWith("c1(")) {
                        tmpC1 = new SimpleColor(
                            Utility.getint("c1", line, 0),
                            Utility.getint("c1", line, 1),
                            Utility.getint("c1", line, 2)
                        );
                        continue;
                    }

                    if (line.startsWith("c2(")) {
                        tmpC2 = new SimpleColor(
                            Utility.getint("c2", line, 0),
                            Utility.getint("c2", line, 1),
                            Utility.getint("c2", line, 2)
                        );
                        continue;
                    }

                    if (line.startsWith("c3(")) {
                        tmpC3 = new SimpleColor(
                            Utility.getint("c3", line, 0),
                            Utility.getint("c3", line, 1),
                            Utility.getint("c3", line, 2)
                        );
                    }

                    if (line.startsWith("gr")) {
                        tmpGr = Utility.getint("gr", line, 0);
                        continue;
                    }

                    if (line.startsWith("fs")) {
                        tmpFs = Utility.getint("fs", line, 0);
                        continue;
                    }

                    if (line.startsWith("noOutline")) {
                        tmpNoOutline = true;
                        continue;
                    }

                    if (line.startsWith("p(")) {
                        tmpOx[tmpVerts] = (int)((Utility.getint("p", line, 0) * div * wid) * nfmm_scale[0]);
                        tmpOy[tmpVerts] = (int)((Utility.getint("p", line, 1) * div) * nfmm_scale[1]);
                        tmpOz[tmpVerts] = (int)((Utility.getint("p", line, 2) * div) * nfmm_scale[2]);
                        tmpVerts++;
                        continue;
                    }
                }
                if (line.startsWith("<p>")) {
                    // If we're inside a wheelModel, never treat <p> as a body poly.
                    if (inWheelModel) continue;
                    flag = true;
                    nverts = 0;
                    gr = 0;
                    fs = 0;
                    light = 0;
                    noOutline = false;
                }
                if (flag) {
                    if (line.startsWith("gr")) {
                        gr = Utility.getint("gr", line, 0);
                    }
                    if (line.startsWith("fs")) {
                        fs = Utility.getint("fs", line, 0);
                        fsAuto[npl] = 2; // mark as explicitly set
                    }
                    if (line.startsWith("c(")) {
                        glass = false;
                        pendingOrigColor = new SimpleColor(
                                Utility.getint("c", line, 0),
                                Utility.getint("c", line, 1),
                                Utility.getint("c", line, 2)
                        );
                        pcolor[0] = pendingOrigColor.r;
                        pcolor[1] = pendingOrigColor.g;
                        pcolor[2] = pendingOrigColor.b;
                    }
                    if (line.startsWith("c1(")) {
                        pendingSkin1Color = new SimpleColor(
                                Utility.getint("c1", line, 0),
                                Utility.getint("c1", line, 1),
                                Utility.getint("c1", line, 2)
                        );
                    }
                    if (line.startsWith("c2(")) {
                        pendingSkin2Color = new SimpleColor(
                                Utility.getint("c2", line, 0),
                                Utility.getint("c2", line, 1),
                                Utility.getint("c2", line, 2)
                        );
                    }
                    if (line.startsWith("c3(")) {
                        pendingSkin3Color = new SimpleColor(
                                Utility.getint("c3", line, 0),
                                Utility.getint("c3", line, 1),
                                Utility.getint("c3", line, 2)
                        );
                    }
                    
                    if (line.startsWith("glass")) {
                        glass = true;
                    }
                    if (line.startsWith("lightF")) {
                        light = 1;
                    }
                    if (line.startsWith("lightB")) {
                        light = 2;
                    }
                    if (line.startsWith("light()")) {
                        light = 1;
                    }
                    if (line.startsWith("p")) {
                        ox[nverts] = (int) ((Utility.getint("p", line, 0) * div * wid) * nfmm_scale[0]);
                        oy[nverts] = (int) ((Utility.getint("p", line, 1) * div) * nfmm_scale[1]);
                        oz[nverts] = (int) ((Utility.getint("p", line, 2) * div) * nfmm_scale[2]);
                        int j1 = (int) Math.sqrt(ox[nverts] * ox[nverts] + oy[nverts] * oy[nverts] + oz[nverts] * oz[nverts]);
                        if (j1 > maxR) {
                            maxR = j1;
                        }
                        nverts++;
                    }
                    if (line.startsWith("noOutline")) {
                        noOutline = true;
                    }
                }
                if (line.startsWith("</p>")) {
                    p[npl] = new Plane(m, ox, oz, oy, nverts, pcolor,
                            glass, gr, fs, 0, 0, 0, disline, 0, road, light, noOutline);
                    if (fsAuto[npl] != 2) { fsAuto[npl] = 1; }
                    npl++;
                    if (!glass) {
                        original.add(pendingOrigColor);
                        skin1.add(pendingSkin1Color);
                        skin2.add(pendingSkin2Color);
                        skin3.add(pendingSkin3Color);
                    }
                    flag = false;
                    continue;
                }
                // ---- rims for next wheels (per-axle pending colours) ----
                if (line.startsWith("rims(")) {
                    pendingRim0 = new SimpleColor(
                            Utility.getint("rims", line, 0),
                            Utility.getint("rims", line, 1),
                            Utility.getint("rims", line, 2)
                    );

                    // reset scheme-specific overrides for this axle
                    pendingRim1 = null;
                    pendingRim2 = null;
                    pendingRim3 = null;

                    wheels.setrims(
                            Utility.getint("rims", line, 0),
                            Utility.getint("rims", line, 1),
                            Utility.getint("rims", line, 2),
                            Utility.getint("rims", line, 3),
                            Utility.getint("rims", line, 4)
                    );
                    continue;
                }

                if (line.startsWith("rims1(")) {
                    pendingRim1 = new SimpleColor(
                            Utility.getint("rims1", line, 0),
                            Utility.getint("rims1", line, 1),
                            Utility.getint("rims1", line, 2)
                    );
                    continue;
                }

                if (line.startsWith("rims2(")) {
                    pendingRim2 = new SimpleColor(
                            Utility.getint("rims2", line, 0),
                            Utility.getint("rims2", line, 1),
                            Utility.getint("rims2", line, 2)
                    );
                    continue;
                }

                if (line.startsWith("rims3(")) {
                    pendingRim3 = new SimpleColor(
                            Utility.getint("rims3", line, 0),
                            Utility.getint("rims3", line, 1),
                            Utility.getint("rims3", line, 2)
                    );
                    continue;
                }


                if (line.startsWith("rims2(")) {
                    SimpleColor sc2 = new SimpleColor(
                        Utility.getint("rims2", line, 0),
                        Utility.getint("rims2", line, 1),
                        Utility.getint("rims2", line, 2)
                    );

                    if (wheelCount < 8)
                        wheelRimColor2[wheelCount] = sc2;

                    continue;
                }

                

                // -------- STEP 6: Wheel anchors + model instancing --------
                if (line.startsWith("w(") && j < 8) {
                    int wxv = (int)(Utility.getint("w", line, 0) * div * nfmm_scale[0]);
                    int wyv = (int)(Utility.getint("w", line, 1) * div * nfmm_scale[1]);
                    int wzv = (int)(Utility.getint("w", line, 2) * div * nfmm_scale[2]);
                    int rotates = Utility.getint("w", line, 3);
                    int width   = (int)(Utility.getint("w", line, 4) * div * wid);
                    int size    = (int)(Utility.getint("w", line, 5) * div);

                    // Parse the optional modelID
                    String inside = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
                    String[] parts = inside.split(",");

                    int modelID = -1;
                    if (parts.length >= 7) {
                        try {
                            modelID = Integer.parseInt(parts[6].trim());
                        } catch (Exception ignored) {}
                    }

                    // store wheel anchor positions
                    keyx[j] = wxv;
                    keyz[j] = wzv;
                    wx[j] = wxv;
                    wy[j] = wyv;
                    wz[j] = wzv;

                    // create metadata entry
                    anchors[j] = new WheelAnchor();
                    WheelAnchor wa = anchors[j];

                    wa.x       = wxv;
                    wa.y       = wyv;
                    wa.z       = wzv;
                    wa.rotates = rotates;
                    wa.width   = width;
                    wa.size    = size;
                    wa.modelId = modelID;

                    // rim colours for this wheel (per axle)
                    if (wheelCount < 8) {
                        SimpleColor base = pendingRim0;
                        if (base != null) {
                            wheelRimColorOriginal[wheelCount] = base;
                            wheelRimColor1[wheelCount] = (pendingRim1 != null) ? pendingRim1 : base;
                            wheelRimColor2[wheelCount] = (pendingRim2 != null) ? pendingRim2 : base;
                            wheelRimColor3[wheelCount] = (pendingRim3 != null) ? pendingRim3 : base;
                        }
                    }

                    hasWheels = true;

                    // Optional custom wheel model
                    List<TempPoly> model = wheelModels.get(modelID);

                    int oldNpl = npl;   // save start index before generating wheel polys

                    // run physics setup (hitbox, ground)
                    wheels.make(m, p, npl, wxv, wyv, wzv, rotates, width, size, gwgr, false);

                    // restore polygon pointer so physics polys don't get added
                    npl = oldNpl;

                    int polyCount;

                    if (modelID != -1 && model != null && !model.isEmpty()) {
                        // custom wheel
                        isCustomWheel[j] = true;
                        wa.custom = true;

                        makeCustomWheel(npl, wxv, wyv, wzv, rotates, model, j, width, size, gwgr);
                        polyCount = model.size();
                        npl += polyCount;

                    } else {
                        // stock wheel
                        isCustomWheel[j] = false;
                        wa.custom = false;

                        wheels.make(m, p, npl, wxv, wyv, wzv, rotates, width, size, gwgr, false);
                        polyCount = 15;        // stock wheel poly count
                        npl += 15;
                    }

                    // now that the polys are added, record the metadata
                    wa.startIndex = oldNpl;
                    wa.polyCount  = polyCount;

                    j++;
                    wheelCount = j;   // total wheels parsed

                    continue;
                }

                if (line.startsWith("shadow")) {
                    shadow = true;
                }
                if (line.startsWith("div")) {
                    div = Utility.getint("div", line, 0) / 10F;
                }
                if (line.startsWith("idiv")) {
                    div = Utility.getint("idiv", line, 0) / 100F;
                }
                if (line.startsWith("iwid")) {
                    wid = Utility.getint("iwid", line, 0) / 100F;
                }
                if (line.startsWith("gwgr")) {
                    gwgr = Utility.getint("gwgr", line, 0);
                }
                if (line.startsWith("ScaleX")) {
                    nfmm_scale[0] = Utility.getint("ScaleX", line, 0) / 100F;
                }
                if (line.startsWith("ScaleY")) {
                    nfmm_scale[1] = Utility.getint("ScaleY", line, 0) / 100F;
                }
                if (line.startsWith("ScaleZ")) {
                    nfmm_scale[2] = Utility.getint("ScaleZ", line, 0) / 100F;
                }
            }
            if (hasWheels) {
                boolean autoFsFound = false;
                for (int i27 = 0; i27 < npl; i27++) {
                    int i28 = 0;
                    int i29 = p[i27].ox[0]; int i30 = p[i27].ox[0];
                    int i31 = p[i27].oy[0]; int i32 = p[i27].oy[0];
                    int i33 = p[i27].oz[0]; int i34 = p[i27].oz[0];
                    for (int i35 = 0; i35 < p[i27].n; i35++) {
                        if (p[i27].ox[i35] > i29) { i29 = p[i27].ox[i35]; }
                        if (p[i27].ox[i35] < i30) { i30 = p[i27].ox[i35]; }
                        if (p[i27].oy[i35] > i31) { i31 = p[i27].oy[i35]; }
                        if (p[i27].oy[i35] < i32) { i32 = p[i27].oy[i35]; }
                        if (p[i27].oz[i35] > i33) { i33 = p[i27].oz[i35]; }
                        if (p[i27].oz[i35] < i34) { i34 = p[i27].oz[i35]; }
                    }
                    if (Math.abs(i29-i30) <= Math.abs(i31-i32) && Math.abs(i29-i30) <= Math.abs(i33-i34)) { i28 = 1; }
                    if (Math.abs(i31-i32) <= Math.abs(i29-i30) && Math.abs(i31-i32) <= Math.abs(i33-i34)) { i28 = 2; }
                    if (Math.abs(i33-i34) <= Math.abs(i29-i30) && Math.abs(i33-i34) <= Math.abs(i31-i32)) { i28 = 3; }
                    if (i28 == 2 && (!autoFsFound || (i31+i32)/2 < roofat)) {
                        roofat = (i31+i32)/2; autoFsFound = true;
                    }
                    if (fsAuto[i27] == 1) {
                        int i36 = 1000; int i37 = 0;
                        for (int i38 = 0; i38 < p[i27].n; i38++) {
                            int i39 = i38+1; if (i39 >= p[i27].n) { i39 -= p[i27].n; }
                            int i40 = i38+2; if (i40 >= p[i27].n) { i40 -= p[i27].n; }
                            if (i28 == 1) {
                                int i41 = Math.abs((int)(Math.atan((double)(p[i27].oz[i38]-p[i27].oz[i39])/(double)(p[i27].oy[i38]-p[i27].oy[i39]))/0.017453292519943295));
                                int i42 = Math.abs((int)(Math.atan((double)(p[i27].oz[i40]-p[i27].oz[i39])/(double)(p[i27].oy[i40]-p[i27].oy[i39]))/0.017453292519943295));
                                if (i41 > 45) { i41 = 90-i41; } else { i42 = 90-i42; }
                                if (i41+i42 < i36) { i36 = i41+i42; i37 = i38; }
                            }
                            if (i28 == 2) {
                                int i43 = Math.abs((int)(Math.atan((double)(p[i27].oz[i38]-p[i27].oz[i39])/(double)(p[i27].ox[i38]-p[i27].ox[i39]))/0.017453292519943295));
                                int i44 = Math.abs((int)(Math.atan((double)(p[i27].oz[i40]-p[i27].oz[i39])/(double)(p[i27].ox[i40]-p[i27].ox[i39]))/0.017453292519943295));
                                if (i43 > 45) { i43 = 90-i43; } else { i44 = 90-i44; }
                                if (i43+i44 < i36) { i36 = i43+i44; i37 = i38; }
                            }
                            if (i28 == 3) {
                                int i45 = Math.abs((int)(Math.atan((double)(p[i27].oy[i38]-p[i27].oy[i39])/(double)(p[i27].ox[i38]-p[i27].ox[i39]))/0.017453292519943295));
                                int i46 = Math.abs((int)(Math.atan((double)(p[i27].oy[i40]-p[i27].oy[i39])/(double)(p[i27].ox[i40]-p[i27].ox[i39]))/0.017453292519943295));
                                if (i45 > 45) { i45 = 90-i45; } else { i46 = 90-i46; }
                                if (i45+i46 < i36) { i36 = i45+i46; i37 = i38; }
                            }
                        }
                        if (i37 != 0) {
                            int[] tmpX = new int[p[i27].n]; int[] tmpY = new int[p[i27].n]; int[] tmpZ = new int[p[i27].n];
                            for (int i50 = 0; i50 < p[i27].n; i50++) { tmpX[i50]=p[i27].ox[i50]; tmpY[i50]=p[i27].oy[i50]; tmpZ[i50]=p[i27].oz[i50]; }
                            for (int i51 = 0; i51 < p[i27].n; i51++) {
                                int i52 = i51+i37; if (i52 >= p[i27].n) { i52 -= p[i27].n; }
                                p[i27].ox[i51]=tmpX[i52]; p[i27].oy[i51]=tmpY[i52]; p[i27].oz[i51]=tmpZ[i52];
                            }
                        }
                        if (i28 == 1) {
                            if (Math.abs(p[i27].oz[0]-p[i27].oz[1]) > Math.abs(p[i27].oy[0]-p[i27].oy[1])) {
                                p[i27].fs = (p[i27].oz[0] > p[i27].oz[1]) ? ((p[i27].oy[1] > p[i27].oy[2]) ? 1 : -1) : ((p[i27].oy[1] > p[i27].oy[2]) ? -1 : 1);
                            } else {
                                p[i27].fs = (p[i27].oy[0] > p[i27].oy[1]) ? ((p[i27].oz[1] > p[i27].oz[2]) ? -1 : 1) : ((p[i27].oz[1] > p[i27].oz[2]) ? 1 : -1);
                            }
                        }
                        if (i28 == 2) {
                            if (Math.abs(p[i27].oz[0]-p[i27].oz[1]) > Math.abs(p[i27].ox[0]-p[i27].ox[1])) {
                                p[i27].fs = (p[i27].oz[0] > p[i27].oz[1]) ? ((p[i27].ox[1] > p[i27].ox[2]) ? -1 : 1) : ((p[i27].ox[1] > p[i27].ox[2]) ? 1 : -1);
                            } else {
                                p[i27].fs = (p[i27].ox[0] > p[i27].ox[1]) ? ((p[i27].oz[1] > p[i27].oz[2]) ? 1 : -1) : ((p[i27].oz[1] > p[i27].oz[2]) ? -1 : 1);
                            }
                        }
                        if (i28 == 3) {
                            if (Math.abs(p[i27].oy[0]-p[i27].oy[1]) > Math.abs(p[i27].ox[0]-p[i27].ox[1])) {
                                p[i27].fs = (p[i27].oy[0] > p[i27].oy[1]) ? ((p[i27].ox[1] > p[i27].ox[2]) ? 1 : -1) : ((p[i27].ox[1] > p[i27].ox[2]) ? -1 : 1);
                            } else {
                                p[i27].fs = (p[i27].ox[0] > p[i27].ox[1]) ? ((p[i27].oy[1] > p[i27].oy[2]) ? -1 : 1) : ((p[i27].oy[1] > p[i27].oy[2]) ? 1 : -1);
                            }
                        }
                        boolean bool53 = false; boolean bool54 = false;
                        for (int i55 = 0; i55 < npl; i55++) {
                            if (i55 != i27 && fsAuto[i55] != 0) {
                                int i57=p[i55].ox[0],i58=p[i55].ox[0],i59=p[i55].oy[0],i60=p[i55].oy[0],i61=p[i55].oz[0],i62=p[i55].oz[0];
                                for (int i63=0; i63<p[i55].n; i63++) {
                                    if (p[i55].ox[i63]>i57) i57=p[i55].ox[i63]; if (p[i55].ox[i63]<i58) i58=p[i55].ox[i63];
                                    if (p[i55].oy[i63]>i59) i59=p[i55].oy[i63]; if (p[i55].oy[i63]<i60) i60=p[i55].oy[i63];
                                    if (p[i55].oz[i63]>i61) i61=p[i55].oz[i63]; if (p[i55].oz[i63]<i62) i62=p[i55].oz[i63];
                                }
                                int i64=(i57+i58)/2, i65=(i59+i60)/2, i66=(i61+i62)/2;
                                int i67=(i29+i30)/2, i68=(i31+i32)/2, i69=(i33+i34)/2;
                                if (i28==1 && (i65<=i31&&i65>=i32&&i66<=i33&&i66>=i34 || i68<=i59&&i68>=i60&&i69<=i61&&i69>=i62)) {
                                    if (i57<i30) bool53=true; if (i58>i29) bool54=true;
                                }
                                if (i28==2 && (i64<=i29&&i64>=i30&&i66<=i33&&i66>=i34 || i67<=i57&&i67>=i58&&i69<=i61&&i69>=i62)) {
                                    if (i59<i32) bool53=true; if (i60>i31) bool54=true;
                                }
                                if (i28==3 && (i64<=i29&&i64>=i30&&i65<=i31&&i65>=i32 || i67<=i57&&i67>=i58&&i68<=i59&&i68>=i60)) {
                                    if (i61<i34) bool53=true; if (i62>i33) bool54=true;
                                }
                            }
                            if (bool53 && bool54) break;
                        }
                        boolean bool70 = false;
                        if (bool53 && !bool54) { bool70=true; }
                        if (bool54 && !bool53) { p[i27].fs*=-1; bool70=true; }
                        if (bool53 && bool54) { p[i27].fs=0; p[i27].gr=40; bool70=true; }
                        if (!bool70) {
                            int i71=0, i72=0;
                            if (i28==1) { i71=(i29+i30)/2; i72=i71; }
                            if (i28==2) { i71=(i31+i32)/2; i72=i71; }
                            if (i28==3) { i71=(i33+i34)/2; i72=i71; }
                            for (int i73=0; i73<npl; i73++) {
                                if (i73!=i27) {
                                    boolean bool74=false;
                                    boolean[] bools=new boolean[p[i73].n];
                                    for (int i75=0; i75<p[i73].n; i75++) {
                                        bools[i75]=false;
                                        for (int i76=0; i76<p[i27].n; i76++) {
                                            if (p[i27].ox[i76]==p[i73].ox[i75]&&p[i27].oy[i76]==p[i73].oy[i75]&&p[i27].oz[i76]==p[i73].oz[i75]) {
                                                bools[i75]=true; bool74=true;
                                            }
                                        }
                                    }
                                    if (bool74) {
                                        for (int i77=0; i77<p[i73].n; i77++) {
                                            if (!bools[i77]) {
                                                if (i28==1) { if (p[i73].ox[i77]>i71) i71=p[i73].ox[i77]; if (p[i73].ox[i77]<i72) i72=p[i73].ox[i77]; }
                                                if (i28==2) { if (p[i73].oy[i77]>i71) i71=p[i73].oy[i77]; if (p[i73].oy[i77]<i72) i72=p[i73].oy[i77]; }
                                                if (i28==3) { if (p[i73].oz[i77]>i71) i71=p[i73].oz[i77]; if (p[i73].oz[i77]<i72) i72=p[i73].oz[i77]; }
                                            }
                                        }
                                    }
                                }
                            }
                            if (i28==1) { if ((i71+i72)/2>(i29+i30)/2) p[i27].fs*=-1; else if ((i71+i72)/2==(i29+i30)/2&&(i29+i30)/2<0) p[i27].fs*=-1; }
                            if (i28==2) { if ((i71+i72)/2>(i31+i32)/2) p[i27].fs*=-1; else if ((i71+i72)/2==(i31+i32)/2&&(i31+i32)/2<0) p[i27].fs*=-1; }
                            if (i28==3) { if ((i71+i72)/2>(i33+i34)/2) p[i27].fs*=-1; else if ((i71+i72)/2==(i33+i34)/2&&(i33+i34)/2<0) p[i27].fs*=-1; }
                        }
                        p[i27].deltafntyp();
                    }
                }
            }
        } catch (IOException e) {
            
            e.printStackTrace();
        }

        skinMap.put(0, original);
        skinMap.put(1, skin1.isEmpty() ? original : skin1);
        skinMap.put(2, skin2.isEmpty() ? original : skin2);
        skinMap.put(3, skin3.isEmpty() ? original : skin3);

        grat = wheels.ground;


    }

    public void applyWheelEdit(ContO car,
                           int wheelModelID,
                            List<Integer> selectedWheels,
                            int newWidth,
                            int newSize,
                            int newX,
                            int newY,
                            int newZ,
                            int newRotates) {

        List<ContO.TempPoly> model = car.wheelModels.get(wheelModelID);
        if (model == null || model.isEmpty()) return;

        for (int w : selectedWheels) {

            // ---------------------------------------
            // 1. Remove the old polygons for wheel w
            // ---------------------------------------
            WheelAnchor wa = car.anchors[w];
            if (wa == null) continue;

            int oldStart = wa.startIndex;
            int oldCount = wa.polyCount;

            if (oldCount > 0) {
                // Shift p[] down to remove old polys
                for (int i = oldStart; i < car.npl - oldCount; i++) {
                    car.p[i] = car.p[i + oldCount];
                }

                car.npl -= oldCount;

                // Fix startIndex for wheels AFTER this one
                for (int ww = w + 1; ww < car.wheelCount; ww++) {
                    if (car.anchors[ww] != null) {
                        car.anchors[ww].startIndex -= oldCount;
                    }
                }
            }

            // Now polygon block for wheel w is empty

            // ---------------------------------------
            // 2. Insert new wheel polys using makeCustomWheel()
            // ---------------------------------------
            int insertAt = oldStart;  // reuse the old start index

            // Make space in the array BEFORE inserting new polys
            int newCount = model.size();
            for (int i = car.npl - 1; i >= insertAt; i--) {
                car.p[i + newCount] = car.p[i];
            }

            // Now insert the new polys at the correct spot
            car.makeCustomWheel(
                insertAt,
                wa.x, wa.y, wa.z,
                wa.rotates,
                model,
                w,
                newWidth,
                newSize,
                wa.gwgr
            );

            car.npl += newCount;

            // ---------------------------------------
            // 3. Update WheelAnchor metadata
            // ---------------------------------------
            wa.custom   = true;
            wa.modelId  = wheelModelID;
            wa.width    = newWidth;
            wa.size     = newSize;

            wa.startIndex = insertAt;
            wa.polyCount  = newCount;

            // ---------------------------------------
            // 4. Move later wheels forward
            // ---------------------------------------
            for (int ww = w + 1; ww < car.wheelCount; ww++) {
                if (car.anchors[ww] != null) {
                    car.anchors[ww].startIndex += newCount;
                }
            }
        }
    }

    private void removeWheel(ContO car, int wheelIndex) {
        WheelAnchor wa = car.anchors[wheelIndex];
        if (wa == null) return;

        int start = wa.startIndex;
        int count = wa.polyCount;

        if (count <= 0) {
            // No polys to remove (probably never drawn or already overwritten)
            return;
        }

        // ---------------------------------------
        // Shift polygons DOWN to cover removed wheel block
        // ---------------------------------------
        for (int i = start; i < car.npl - count; i++) {
            car.p[i] = car.p[i + count];
        }

        car.npl -= count;

        // ---------------------------------------
        // Fix all later wheels' startIndex
        // ---------------------------------------
        for (int w = wheelIndex + 1; w < car.wheelCount; w++) {
            WheelAnchor next = car.anchors[w];
            if (next != null) {
                next.startIndex -= count;
            }
        }

        // ---------------------------------------
        // Reset metadata for this wheel so it can be rebuilt
        // ---------------------------------------
        wa.polyCount = 0;
        wa.custom = false;  // after removal, no longer custom polys exist
    }

    public void applySkin(int skinIndex) {
        ArrayList<SimpleColor> colors = skinMap.get(skinIndex);
        if (colors == null || colors.isEmpty()) return;

        final int STOCK_WHEEL_POLYS = 15;
        final int RIM_START_INDEX = 1;  
        final int RIM_END_INDEX   = 7; 

        int numWheels = wheelCount;
        boolean hasCustom = false;

        for (int i = 0; i < numWheels; i++) {
            if (isCustomWheel[i]) {
                hasCustom = true;
                break;
            }
        }

        // ---------------- BODY ----------------
        int bodyPolyCount = hasCustom
                ? npl
                : npl - (numWheels * STOCK_WHEEL_POLYS);

        int idx = 0;
        for (int pi = 0; pi < bodyPolyCount; pi++) {
            Plane pl = p[pi];
            if (pl.glass) continue;
            if (idx >= colors.size()) break;

            SimpleColor c = colors.get(idx++);
            if (c == null) continue;
            pl.c[0] = c.r; pl.c[1] = c.g; pl.c[2] = c.b;
            pl.oc[0] = c.r; pl.oc[1] = c.g; pl.oc[2] = c.b;

            float[] h = Color.RGBtoHSB(c.r, c.g, c.b, null);
            pl.hsb[0] = h[0]; pl.hsb[1] = h[1]; pl.hsb[2] = h[2];
        }

        // ---------------- RIMS ----------------
        if (!hasCustom && numWheels > 0) {

            int baseWheelStart = npl - (numWheels * STOCK_WHEEL_POLYS);

            for (int w = 0; w < numWheels; w++) {

                SimpleColor rc;
                switch (skinIndex) {
                    case 1:
                        rc = wheelRimColor1[w] != null
                                ? wheelRimColor1[w]
                                : wheelRimColorOriginal[w];
                        break;
                    case 2:
                        rc = wheelRimColor2[w] != null
                                ? wheelRimColor2[w]
                                : wheelRimColorOriginal[w];
                        break;
                    case 3:
                        rc = wheelRimColor3[w] != null
                                ? wheelRimColor3[w]
                                : wheelRimColorOriginal[w];
                        break;
                    default: // 0 or anything else
                        rc = wheelRimColorOriginal[w];
                }

                if (rc == null) continue;

                int blockStart = baseWheelStart + w * STOCK_WHEEL_POLYS;
                int start      = blockStart + RIM_START_INDEX;
                int end        = blockStart + RIM_END_INDEX;

                for (int pi = start; pi < end; pi++) {
                    Plane pl = p[pi];

                    pl.c[0] = rc.r; pl.c[1] = rc.g; pl.c[2] = rc.b;
                    pl.oc[0] = rc.r; pl.oc[1] = rc.g; pl.oc[2] = rc.b;

                    float[] h = Color.RGBtoHSB(rc.r, rc.g, rc.b, null);
                    pl.hsb[0] = h[0]; pl.hsb[1] = h[1]; pl.hsb[2] = h[2];
                }
            }
        }
    }




    public void d(Graphics2D rd, int hoveredPoly, Set<Integer> selectedPolygons) {

        
        int i = Medium.cx + (int) ((x - Medium.x - Medium.cx) * RadicalMath.cos(Medium.xz) - (z - Medium.z - Medium.cz) * RadicalMath.sin(Medium.xz));
        int j = Medium.cz + (int) ((x - Medium.x - Medium.cx) * RadicalMath.sin(Medium.xz) + (z - Medium.z - Medium.cz) * RadicalMath.cos(Medium.xz));
        int k = Medium.cz + (int) ((y - Medium.y - Medium.cy) * RadicalMath.sin(Medium.zy) + (j - Medium.cz) * RadicalMath.cos(Medium.zy));
        int l = Utility.cXs(i + maxR, k) - Utility.cXs(i - maxR, k);

        

        if (Utility.cXs(i + maxR * 2, k) > 0 && Utility.cXs(i - maxR * 2, k) < Medium.w && k > -maxR
                && (k < Medium.fade[disline] + maxR || Medium.trk)) {
            //SHADOW
            if (shadow) {
                if (!Medium.crs) {
                    // I think this has something to do with rendering objects?
                    if (k < 2000) {
                        boolean flag = false;
                        for (int l1 = - 1; l1 >= 0; l1--) {
                            
                            flag = true;
                            break;
                        }

                        if (flag) {
                            for (int i2 = 0; i2 < npl; i2++) {
                                p[i2].s(rd, x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, 0);
                            }

                        } else {
                            int j2 = Medium.cy + (int) ((Medium.ground - Medium.cy) * RadicalMath.cos(Medium.zy)
                                    - (j - Medium.cz) * RadicalMath.sin(Medium.zy));
                            int k2 = Medium.cz + (int) ((Medium.ground - Medium.cy) * RadicalMath.sin(Medium.zy)
                                    + (j - Medium.cz) * RadicalMath.cos(Medium.zy));
                            if (Utility.cYs(j2 + maxR, k2) > 0 && Utility.cYs(j2 - maxR, k2) < Medium.h) {
                                for (int l2 = 0; l2 < npl; l2++) {
                                    p[l2].s(rd, x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, 1);
                                }

                            }
                        }
                        Medium.addSp(x - Medium.x, z - Medium.z, (int) (maxR * 0.80000000000000004D));

                    } 
                } else {
                    for (int i1 = 0; i1 < npl; i1++) {
                        p[i1].s(rd, x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, 2);
                    }
                }
            }
            // END SHADOW
            //START RENDER MODEL
            int j1 = Medium.cy + (int) ((y - Medium.y - Medium.cy) * RadicalMath.cos(Medium.zy) - (j - Medium.cz) * RadicalMath.sin(Medium.zy));
            if (Utility.cYs(j1 + maxR, k) > 0 && Utility.cYs(j1 - maxR, k) < Medium.h) {
                
                // Pre-pass: compute av for all polygons before sorting
                for (int pi = 0; pi < npl; pi++) {
                    int i13 = p[pi].gr;
                    if (i13 < 0 && i13 >= -17) i13 = 0;
                    if (p[pi].gr == -11) i13 = -90;
                    if (p[pi].gr == -14 || p[pi].gr == -15) i13 = -50;
                    if (p[pi].gr == -16) i13 = 35;
                    p[pi].computeAv(x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, i13);
                }
                
                int ai[] = new int[npl];
                int ai1[] = new int[npl];
                int i3 = 0;
                do {
                    
                } while (++i3 < 4);
                for (int j3 = 0; j3 < npl; j3++) {
                    ai[j3] = 0;
                }

                for (int k3 = 0; k3 < npl; k3++) {
                    for (int i4 = k3 + 1; i4 < npl; i4++) {
                        if (p[k3].av != p[i4].av) {
                            if (p[k3].av < p[i4].av) {
                                ai[k3]++;
                            } else {
                                ai[i4]++;
                            }
                        } else if (k3 > i4) {
                            ai[k3]++;
                        } else {
                            ai[i4]++;
                        }
                    }

                    ai1[ai[k3]] = k3;
                }

                for (int l3 = 0; l3 < npl; l3++) {
                    p[l3].hoverColor    = hoverColor;
                    p[l3].selectedColor = selectedColor;
                }

                for (int l3 = 0; l3 < npl; l3++) {
                    boolean isHovered = wholeHover || (ai1[l3] == hoveredPoly);
                    boolean isSelected = selectedPolygons != null && selectedPolygons.contains(ai1[l3]);  // FIX: use ai1[l3] not i
                    p[ai1[l3]].d(rd, x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, wxz, wzy, noline, l, isHovered, isSelected);
                }

                
            }
            //END RENDER MODEL
        }
    }

        public int[] getAttachPoints() {

        if (cachedATP != null) return cachedATP;
        
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < npl; i++) {
            for (int v = 0; v < p[i].n; v++) {
                if (p[i].ox[v] < minX) minX = p[i].ox[v];
                if (p[i].ox[v] > maxX) maxX = p[i].ox[v];
                if (p[i].oz[v] < minZ) minZ = p[i].oz[v];
                if (p[i].oz[v] > maxZ) maxZ = p[i].oz[v];
            }
        }

        int threshold = 300;

        // For each extreme, collect vertices near it and measure perpendicular spread
        // An open end has perp spread >= ~1000 (road is ~1680 wide)
        // A closed end or side has smaller spread
        int[] extremeVal  = { maxZ,  minZ,  maxX,  minX  };
        boolean[] isXaxis = { false, false, true,  true   };

        java.util.List<int[]> openEnds = new java.util.ArrayList<>();

        for (int e = 0; e < 4; e++) {
            int val = extremeVal[e];
            boolean xAxis = isXaxis[e];
            int count = 0;

            for (int i = 0; i < npl; i++) {
                for (int v = 0; v < p[i].n; v++) {
                    int primary = xAxis ? p[i].ox[v] : p[i].oz[v];
                    if (Math.abs(primary - val) <= threshold) {
                        count++;
                    }
                }
            }

            if (count == 0) continue;
            // Check spread by counting vertices on both sides of perp axis
            int negCount = 0, posCount = 0;
            for (int i = 0; i < npl; i++) {
                for (int v = 0; v < p[i].n; v++) {
                    int primary = xAxis ? p[i].ox[v] : p[i].oz[v];
                    int perp    = xAxis ? p[i].oz[v] : p[i].ox[v];
                    if (Math.abs(primary - val) <= threshold) {
                        if (perp < 0) negCount++;
                        else if (perp > 0) posCount++;
                    }
                }
            }
            // Open end has vertices on both sides of center
            if (negCount > 0 && posCount > 0) {
                int ax = xAxis ? val : 0;
                int az = xAxis ? 0 : val;
                openEnds.add(new int[]{ ax, az });
            }
        }

        if (openEnds.size() >= 2) {
            cachedATP = new int[]{ openEnds.get(0)[0], openEnds.get(0)[1],
                                openEnds.get(1)[0], openEnds.get(1)[1] };
            return cachedATP;
        } else if (openEnds.size() == 1) {
            cachedATP = new int[]{ openEnds.get(0)[0], openEnds.get(0)[1], 0, 0 };
            return cachedATP;
        }
        cachedATP = new int[]{ 0, maxZ, 0, minZ };
        return cachedATP;
    }

}
