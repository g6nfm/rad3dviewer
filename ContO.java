


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
    private int disp = 0;
    private int disline = 7;
    private boolean shadow = false;
    private boolean noline = false;
    public int grat = 0;
    public final int[] keyx = new int[8];
    public final int[] keyz = new int[8];
    boolean[] isCustomWheel = new boolean[8];
    public int wheel0StartIndex = -1;
    public int wheel0PolyCount = 0;
    public final int[] sx = new int[4];
    public final int[] sy = new int[4];
    public final int[] sz = new int[4];
    public final int[] stg = new int[8];
    public final int[] dov = new int[4];
    public final float[] smag = new float[4];
    public final int[] scx = new int[4];
    public final int[] scz = new int[4];
    public final boolean[] fulls = new boolean[4];
    public boolean elec = false;
    public boolean roted = false;
    public boolean fix = false;
    public int fcnt = 0;
    public int checkpoint = 0;

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
    boolean mirror = (wheelIndex % 2 == 1);

    for (TempPoly poly : model) {

        int[] ox = new int[poly.n];
        int[] oy = new int[poly.n];
        int[] oz = new int[poly.n];

        float scale  = size  / 20f;
        float wscale = width / 20f;

        for (int v = 0; v < poly.n; v++) {

            int lx = mirror ? -poly.ox[v] : poly.ox[v];
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
                if (line.startsWith("rims(")) {
                    //original.add(new SimpleColor (Utility.getint("rims", line, 0), Utility.getint("rims", line, 1),
                            //Utility.getint("rims", line, 2)));
                    wheels.setrims(Utility.getint("rims", line, 0), Utility.getint("rims", line, 1),
                            Utility.getint("rims", line, 2), Utility.getint("rims", line, 3),
                            Utility.getint("rims", line, 4));
                }
                if (line.startsWith("rims1(")) {
                    //skin1.add(new SimpleColor (Utility.getint("rims1", line, 0), Utility.getint("rims1", line, 1),
                            //Utility.getint("rims1", line, 2)));
                }
                if (line.startsWith("rims2(")) {
                    //skin2.add(new SimpleColor (Utility.getint("rims2", line, 0), Utility.getint("rims2", line, 1),
                            //Utility.getint("rims2", line, 2)));
                }
                // -------- STEP 6: Wheel anchors + model instancing --------
                if (line.startsWith("w(") && j < 8) {
                    int wxv = (int)(Utility.getint("w", line, 0) * div * nfmm_scale[0]);
                    int wyv = (int)(Utility.getint("w", line, 1) * div * nfmm_scale[1]);
                    int wzv = (int)(Utility.getint("w", line, 2) * div * nfmm_scale[2]);
                    int rotates = Utility.getint("w", line, 3);
                    int width   = (int)(Utility.getint("w", line, 4) * div * wid);
                    int size    = (int)(Utility.getint("w", line, 5) * div);
                    //int modelID = Utility.getint("w", line, 6);
                    // Get all parameters inside the parentheses
                    String inside = line.substring(line.indexOf('(') + 1, line.lastIndexOf(')'));
                    String[] parts = inside.split(",");
                    // CASE A: custom wheel models exist → expect 7 parameters
                   // Default: use normal NFM wheel
                    int modelID = -1;
                    // Parse modelID ONLY if the line actually has it
                    if (parts.length >= 7) {
                        try {
                            modelID = Integer.parseInt(parts[6].trim());
                        } catch (Exception ignored) {
                            modelID = -1;
                        }
                    }
                    keyx[j] = wxv;
                    keyz[j] = wzv;

                    wx[j] = wxv;
                    wy[j] = wyv;
                    wz[j] = wzv;
                    List<TempPoly> model = wheelModels.get(modelID);
                    // ALWAYS run wheels.make() BUT DO NOT USE ITS POLYGONS.
                    int oldNpl = npl;
                    // Run physics setup
                    wheels.make(m, p, npl, wxv, wyv, wzv, rotates, width, size, gwgr, false);

                    npl = oldNpl;
                    if (modelID != -1 && model != null && !model.isEmpty()) {
                        isCustomWheel[j] = true;          
                        makeCustomWheel(npl, wxv, wyv, wzv, rotates, model, j, width, size);
                        npl += model.size();
                    } else {
                        // Non-custom fallback:
                        isCustomWheel[j] = false;
                        wheels.make(m, p, npl, wxv, wyv, wzv, rotates, width, size, gwgr, false);
                        npl += 15;
                    }
                    j++;
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


    public void d(Graphics2D rd) {

        
        int i = Medium.cx + (int) ((x - Medium.x - Medium.cx) * RadicalMath.cos(Medium.xz) - (z - Medium.z - Medium.cz) * RadicalMath.sin(Medium.xz));
        int j = Medium.cz + (int) ((x - Medium.x - Medium.cx) * RadicalMath.sin(Medium.xz) + (z - Medium.z - Medium.cz) * RadicalMath.cos(Medium.xz));
        int k = Medium.cz + (int) ((y - Medium.y - Medium.cy) * RadicalMath.sin(Medium.zy) + (j - Medium.cz) * RadicalMath.cos(Medium.zy));
        int l = Utility.cXs(i + maxR, k) - Utility.cXs(i - maxR, k);


      

        if (Utility.cXs(i + maxR * 2, k) > 0 && Utility.cXs(i - maxR * 2, k) < Medium.w && k > -maxR
                && (k < Medium.fade[disline] + maxR || Medium.trk) && (l > disp || Medium.trk)) {
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
                
                if (checkpoint != 0 && checkpoint - 1 == Medium.checkpoint) {
                    l = -1;
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
                    p[ai1[l3]].d(rd, x - Medium.x, y - Medium.y, z - Medium.z, xz, xy, zy, wxz, wzy, noline, l);
                    
                }

                
            }
            //END RENDER MODEL
        }
    }

}
