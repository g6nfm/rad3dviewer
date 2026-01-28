


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

    public final int[] keyx = new int[8];
    public final int[] keyz = new int[8];

    boolean[] isCustomWheel = new boolean[8];

    SimpleColor pendingRim0, pendingRim1, pendingRim2;

    public SimpleColor[] wheelRimColorOriginal = new SimpleColor[8];
    public SimpleColor[] wheelRimColor1 = new SimpleColor[8];
    public SimpleColor[] wheelRimColor2 = new SimpleColor[8];
    
    public int wheelCount = 0;

    public static class WheelAnchor {
        public int x, y, z;
        public int rotates;
        public int width;
        public int size;
        public int modelId;
        public boolean custom;

        public int startIndex;   // first poly index for this wheel
        public int polyCount;    // how many polys this wheel uses
    }

    public final WheelAnchor[] anchors = new WheelAnchor[8];

    

    Wheels wheels = new Wheels();


    //Skins

    ArrayList<SimpleColor> original = new ArrayList<>();
    ArrayList<SimpleColor> skin1 = new ArrayList<>();
    ArrayList<SimpleColor> skin2 = new ArrayList<>();

    HashMap<Integer, ArrayList<SimpleColor>> skinMap = new HashMap<>();

    
    

    class TempPoly {
            int[] ox, oy, oz;
            int n;
            SimpleColor c, c1, c2;
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
        int size
) {
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

        int[] col = { c.r, c.g, c.b };

        // --- HANDLE ROTATION: ONLY FRONT WHEELS TURN ---
        int px = (wheelIndex <= 1) ? wxv : 0;   // <= front wheels
        int pr = (wheelIndex <= 1) ? rotates : 0;

        p[nplStart] = new Plane(
            m,
            ox, oz, oy,
            poly.n,
            col,
            false,
            poly.gr,
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
        SimpleColor tmpC = null, tmpC1 = null, tmpC2 = null;
        boolean tmpNoOutline = false;
        int tmpGr = 1, tmpFs = 0;

        

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
                    tmpC = tmpC1 = tmpC2 = null;
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
                    poly.noOutline = tmpNoOutline;
                    poly.gr = tmpGr;
                    poly.fs = tmpFs;

                    original.add(tmpC);
                    skin1.add(tmpC1);
                    skin2.add(tmpC2);

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
                    // just a normal body polygon
                    p[npl++] = new Plane(m, ox, oz, oy, nverts, pcolor,
                            glass, gr, fs, 0, 0, 0, disline, 0, road, light, noOutline);

                    if (!glass) {
                        original.add(pendingOrigColor);
                        skin1.add(pendingSkin1Color);
                        skin2.add(pendingSkin2Color);
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
                        }
                    }

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

                        makeCustomWheel(npl, wxv, wyv, wzv, rotates, model, j, width, size);
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
        } catch (IOException e) {
            
            e.printStackTrace();
        }

        skinMap.put(0, original);
        skinMap.put(1, skin1);

        if(!skin2.isEmpty()){
            skinMap.put(2, skin2);
        }

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
                newSize
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




    public void d(Graphics2D rd) {

        
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
                    p[ai1[l3]].d(rd, x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, wxz, wzy, noline, l);
                    
                }

                
            }
            //END RENDER MODEL
        }
    }

}
