
import java.awt.*;

/**
 * Medium manages most of the graphics you see during games, such as clouds, ground, and dust.
 * This class was edited and re-edited again by Chaotic for (mostly) full NFMM Graphics (excluding sparks and extra dust).
 *
 * @author Kaffeinated, Chaotic, Omar Wally
 */
public class Medium {

    public static int mgen;
    public static int focus_point;
    public static int ground;
    private static int skyline;

    public static final int[] fade = {
            3000, 4500, 6000, 7500, 9000, 10500, 12000, 13500, 15000, 16500, 18000, 19500, 21000, 22500, 24000, 25500
    };
    private static final int[] cldd = {
            210, 210, 210, 1, -1000
    };
    private static final int[] clds = {
            210, 210, 210
    };
    private static final int[] osky = {
            170, 220, 255
    };
    public static final int[] csky = {
            170, 220, 255
    };

    private static final int[] ogrnd = {
            205, 200, 200
    };
    public static final int[] cgrnd = {
            205, 200, 200
    };
    private static final int[] texture = {
            0, 0, 0, 50
    };
    private static final int[] cpol = {
            215, 210, 210
    };
    private static final int[] crgrnd = {
            205, 200, 200
    };
    public static final int[] cfade = {
            255, 220, 220
    };
    public static int snap[] = {
            0, 0, 0
    };
    public static int origfade;
    public static int fogd;
    public static boolean lightson;

    public static int lightn;
    private static boolean lton;
    private static int lilo;
    public static int flex;
    public static boolean trk;
    public static boolean crs;
    public static int cx;
    public static int cy;
    public static int cz;
    public static float xz;
    public static float zy;
    public static int x;
    public static int y;
    public static int z;
    public static int scroll;
    public static int w;
    public static int h;
    public static int nsp;
    public static int spx[];
    public static int spz[];
    public static int sprad[];

    public static float vxz;
    public static int adv;
    public static boolean vert;

    public static int lastmaf;
    public static int checkpoint;
    public static boolean lastcheck;
    public static float elecr;
    public static boolean cpflik;
    public static boolean nochekflk;
    private static int cntrn;
    private static boolean diup[];
    private static int rand[];
    private static int trn;
    public static int hit;
    public static int ptr;
    public static int ptcnt;
    public static int nrnd;
    public static long trx;
    public static long trz;

    public static int fallen;

    private static int resdown;

    private static int iw;


    private static int ih;

    public static int focusCar;

    public static int focusPoint = 400;

    public static void setViewport(int x0, int y0, int width, int height) {
        // Left / top corner of the drawable area
        iw = x0;
        ih = y0;

        // Overall size
        w = width;
        h = height;

        // Screen centre for projection
        cx = x0 + width  / 2;
        cy = y0 + height / 2;
    }


    public static float random() {
        if (cntrn == 0) {
            int i = 0;
            do {
                rand[i] = (int) (10D * Math.random());
                if (Math.random() > Math.random()) {
                    diup[i] = false;
                } else {
                    diup[i] = true;
                }
            } while (++i < 3);
            cntrn = 20;
        } else {
            cntrn--;
        }
        int j = 0;
        do {
            if (diup[j]) {
                rand[j]++;
                if (rand[j] == 10) {
                    rand[j] = 0;
                }
            } else {
                rand[j]--;
                if (rand[j] == -1) {
                    rand[j] = 9;
                }
            }
        } while (++j < 3);
        trn++;
        if (trn == 3) {
            trn = 0;
        }
        return rand[trn] / 10F;

    }

    

    

    public Medium() {
        focus_point = 400;
        ground = 250;
        skyline = -300;
        fogd = 7;
        mgen = (int) (Math.random() * 100000D);
        snap = new int[3];
        origfade = 3000;
        fogd = 3;
        lightson = false;
        lightn = -1;
        lilo = 217;
        flex = 0;
        trk = false;
        crs = false;
        cx = 640;
        cy = 360;
        cz = 50;
        xz = 0;
        zy = 0;
        x = 0;
        y = 0;
        z = 0;
        w = 1280;
        h = 720;
        nsp = 0;
        spx = new int[7];
        spz = new int[7];
        sprad = new int[7];
        vxz = 180;
        adv = 720;
        vert = false;
        lastmaf = 0;
        checkpoint = -1;
        lastcheck = false;
        elecr = 0.0F;
        cpflik = false;
        nochekflk = false;
        cntrn = 0;
        diup = new boolean[3];
        rand = new int[3];
        trn = 0;
        hit = 45000;
        ptr = 0;
        ptcnt = -10;
        nrnd = 0;
        trx = 0L;
        trz = 0L;
        fallen = 0;
    }

    

    public static void d(Graphics2D graphics2d) {
        nsp = 0;
        if (zy > 90) {
            zy = 90;
        }
        if (zy < -90) {
            zy = -90;
        }
        if (xz > 360) {
            xz -= 360;
        }
        if (xz < 0) {
            xz += 360;
        }
        if (y > 0) {
            y = 0;
        }

        float t_cos = RadicalMath.cos(zy);
        float t_sin = RadicalMath.sin(zy);

        ground = 250 - y;
        int ai[] = new int[4];
        int ai1[] = new int[4];
        int i = cgrnd[0];
        int j = cgrnd[1];
        int k = cgrnd[2];
        int l = crgrnd[0];
        int i1 = crgrnd[1];
        int j1 = crgrnd[2];
        int k1 = h;
        for (int l1 = 0; l1 < 16; l1++) {
            int j2 = fade[l1];
            int l2 = ground;
            if (zy != 0) {
                l2 = cy + (int) ((ground - cy) * t_cos - (fade[l1] - cz) * t_sin);
                j2 = cz + (int) ((ground - cy) * t_sin + (fade[l1] - cz) * t_cos);
            }
            ai[0] = iw;
            ai1[0] = Utility.ys(l2, j2, 1);
            if (ai1[0] < ih) {
                ai1[0] = ih;
            }
            if (ai1[0] > h) {
                ai1[0] = h;
            }
            ai[1] = iw;
            ai1[1] = k1;
            ai[2] = w;
            ai1[2] = k1;
            ai[3] = w;
            ai1[3] = ai1[0];
            k1 = ai1[0];
            if (l1 > 0) {
                l = (l * 7 + cfade[0]) / 8;
                i1 = (i1 * 7 + cfade[1]) / 8;
                j1 = (j1 * 7 + cfade[2]) / 8;
                if (l1 < 3) {
                    i = (i * 7 + cfade[0]) / 8;
                    j = (j * 7 + cfade[1]) / 8;
                    k = (k * 7 + cfade[2]) / 8;
                } else {
                    i = l;
                    j = i1;
                    k = j1;
                }
            }
            if (ai1[0] < h && ai1[1] > ih) {
                graphics2d.setColor(new Color(i, j, k));
                graphics2d.fillPolygon(ai, ai1, 4);
            }
        }

        if (lightn != -1 && lton) {
            if (lightn < 16) {
                if (lilo > lightn + 217) {
                    lilo -= 3;
                } else {
                    lightn = (int) (16F + 16F * random());
                }
            } else if (lilo < lightn + 217) {
                lilo += 7;
            } else {
                lightn = (int) (16F * random());
            }
            csky[0] = (int) (lilo + lilo * (snap[0] / 100F));
            if (csky[0] > 255) {
                csky[0] = 255;
            }
            if (csky[0] < 0) {
                csky[0] = 0;
            }
            csky[1] = (int) (lilo + lilo * (snap[1] / 100F));
            if (csky[1] > 255) {
                csky[1] = 255;
            }
            if (csky[1] < 0) {
                csky[1] = 0;
            }
            csky[2] = (int) (lilo + lilo * (snap[2] / 100F));
            if (csky[2] > 255) {
                csky[2] = 255;
            }
            if (csky[2] < 0) {
                csky[2] = 0;
            }
        }

        i = csky[0];
        j = csky[1];
        k = csky[2];

        int i2 = i;
        int k2 = j;
        int i3 = k;
        int j3 = cy + (int) ((skyline - 700 - cy) * t_cos - (7000 - cz) * t_sin);
        int k3 = cz + (int) ((skyline - 700 - cy) * t_sin + (7000 - cz) * t_cos);

        j3 = Utility.ys(j3, k3, 1);
        int l3 = ih;
        for (int i4 = 0; i4 < 16; i4++) {
            int k4 = fade[i4];
            int i5 = skyline;
            if (zy != 0) {
                i5 = cy + (int) ((skyline - cy) * t_cos - (fade[i4] - cz) * t_sin);
                k4 = cz + (int) ((skyline - cy) * t_sin + (fade[i4] - cz) * t_cos);
            }
            ai[0] = iw;
            ai1[0] = Utility.ys(i5, k4, 1);
            if (ai1[0] > h) {
                ai1[0] = h;
            }
            if (ai1[0] < ih) {
                ai1[0] = ih;
            }
            ai[1] = iw;
            ai1[1] = l3;
            ai[2] = w;
            ai1[2] = l3;
            ai[3] = w;
            ai1[3] = ai1[0];
            l3 = ai1[0];
            if (i4 > 0) {
                i = (i * 7 + cfade[0]) / 8;
                j = (j * 7 + cfade[1]) / 8;
                k = (k * 7 + cfade[2]) / 8;
            }
            if (ai1[1] < j3) {
                i2 = i;
                k2 = j;
                i3 = k;
            }
            if (ai1[0] > ih && ai1[1] < h) {
                graphics2d.setColor(new Color(i, j, k));
                graphics2d.fillPolygon(ai, ai1, 4);
            }
        }

        ai[0] = iw;
        ai1[0] = l3;
        ai[1] = iw;
        ai1[1] = k1;
        ai[2] = w;
        ai1[2] = k1;
        ai[3] = w;
        ai1[3] = l3;
        if (ai1[0] < h && ai1[1] > ih) {
            float f = (Math.abs(y) - 250F) / (fade[0] * 2);
            if (f < 0.0F) {
                f = 0.0F;
            }
            if (f > 1.0F) {
                f = 1.0F;
            }
            i = (int) ((i * (1.0F - f) + l * (1.0F + f)) / 2.0F);
            j = (int) ((j * (1.0F - f) + i1 * (1.0F + f)) / 2.0F);
            k = (int) ((k * (1.0F - f) + j1 * (1.0F + f)) / 2.0F);
            graphics2d.setColor(new Color(i, j, k));
            graphics2d.fillPolygon(ai, ai1, 4);
        }
        if (resdown != 2) {
            for (int j4 = 1; j4 < 20; j4++) {
                int l4 = 7000;
                int j5 = skyline - 700 - j4 * 70;
                if (zy != 0 && j4 != 19) {
                    j5 = cy + (int) ((skyline - 700 - j4 * 70 - cy) * t_cos - (7000 - cz) * t_sin);
                    l4 = cz + (int) ((skyline - 700 - j4 * 70 - cy) * t_sin + (7000 - cz) * t_cos);
                }
                ai[0] = iw;
                if (j4 != 19) {
                    ai1[0] = Utility.ys(j5, l4, 1);
                    if (ai1[0] > h) {
                        ai1[0] = h;
                    }
                    if (ai1[0] < ih) {
                        ai1[0] = ih;
                    }
                } else {
                    ai1[0] = ih;
                }
                ai[1] = iw;
                ai1[1] = j3;
                ai[2] = w;
                ai1[2] = j3;
                ai[3] = w;
                ai1[3] = ai1[0];
                j3 = ai1[0];
                i2 = (int) (i2 * 0.99099999999999999D);
                k2 = (int) (k2 * 0.99099999999999999D);
                i3 = (int) (i3 * 0.998D);
                if (ai1[1] > ih && ai1[0] < h) {
                    graphics2d.setColor(new Color(i2, k2, i3));
                    graphics2d.fillPolygon(ai, ai1, 4);
                }
            }

            
        }
        
    }

    public static void addSp(int i, int j, int k) {
        if (nsp != 7) {
            spx[nsp] = i;
            spz[nsp] = j;
            sprad[nsp] = k;
            nsp++;
        }
    }

    public static void setSnap(int i, int j, int k) {
        snap[0] = i;
        snap[1] = j;
        snap[2] = k;
    }

    public static void adjustFade(float f) {
        if (f < 15F) {
            fade[0] = (int) (origfade - 1000F * (15F - f));
            if (fade[0] < 3000) {
                fade[0] = 3000;
            }
            fadeFrom(fade[0]);
        } else if (fade[0] != origfade) {
            fade[0] += 500;
            if (fade[0] > origfade) {
                fade[0] = origfade;
            }
            fadeFrom(fade[0]);
        }
    }

    public static void setFade(int i, int j, int k) {
        cfade[0] = (int) (i + i * (snap[0] / 100F));
        if (cfade[0] > 255) {
            cfade[0] = 255;
        }
        if (cfade[0] < 0) {
            cfade[0] = 0;
        }
        cfade[1] = (int) (j + j * (snap[1] / 100F));
        if (cfade[1] > 255) {
            cfade[1] = 255;
        }
        if (cfade[1] < 0) {
            cfade[1] = 0;
        }
        cfade[2] = (int) (k + k * (snap[2] / 100F));
        if (cfade[2] > 255) {
            cfade[2] = 255;
        }
        if (cfade[2] < 0) {
            cfade[2] = 0;
        }
    }

    public static void setSky(int i, int j, int k) {
        osky[0] = i;
        osky[1] = j;
        osky[2] = k;
        for (int l = 0; l < 3; l++) {
            clds[l] = (osky[l] * cldd[3] + cldd[l]) / (cldd[3] + 1);
            clds[l] = (int) (clds[l] + clds[l] * (snap[l] / 100F));
            if (clds[l] > 255) {
                clds[l] = 255;
            }
            if (clds[l] < 0) {
                clds[l] = 0;
            }
        }

        csky[0] = (int) (i + i * (snap[0] / 100F));
        if (csky[0] > 255) {
            csky[0] = 255;
        }
        if (csky[0] < 0) {
            csky[0] = 0;
        }
        csky[1] = (int) (j + j * (snap[1] / 100F));
        if (csky[1] > 255) {
            csky[1] = 255;
        }
        if (csky[1] < 0) {
            csky[1] = 0;
        }
        csky[2] = (int) (k + k * (snap[2] / 100F));
        if (csky[2] > 255) {
            csky[2] = 255;
        }
        if (csky[2] < 0) {
            csky[2] = 0;
        }
        float af[] = new float[3];
        Color.RGBtoHSB(csky[0], csky[1], csky[2], af);
    }

    
    public static void setGround(int i, int j, int k) {
        ogrnd[0] = i;
        ogrnd[1] = j;
        ogrnd[2] = k;
        for (int l = 0; l < 3; l++) {
            cpol[l] = (ogrnd[l] * texture[3] + texture[l]) / (1 + texture[3]);
            cpol[l] = (int) (cpol[l] + cpol[l] * (snap[l] / 100F));
            if (cpol[l] > 255) {
                cpol[l] = 255;
            }
            if (cpol[l] < 0) {
                cpol[l] = 0;
            }
        }

        cgrnd[0] = (int) (i + i * (snap[0] / 100F));
        if (cgrnd[0] > 255) {
            cgrnd[0] = 255;
        }
        if (cgrnd[0] < 0) {
            cgrnd[0] = 0;
        }
        cgrnd[1] = (int) (j + j * (snap[1] / 100F));
        if (cgrnd[1] > 255) {
            cgrnd[1] = 255;
        }
        if (cgrnd[1] < 0) {
            cgrnd[1] = 0;
        }
        cgrnd[2] = (int) (k + k * (snap[2] / 100F));
        if (cgrnd[2] > 255) {
            cgrnd[2] = 255;
        }
        if (cgrnd[2] < 0) {
            cgrnd[2] = 0;
        }
        for (int i1 = 0; i1 < 3; i1++) {
            crgrnd[i1] = (int) ((cpol[i1] * 0.98999999999999999D + cgrnd[i1]) / 2D);
        }

    }

    public static void fadeFrom(int i) {
        if (i > 8000) {
            i = 8000;
        }
        for (int j = 1; j < 17; j++) {
            fade[j - 1] = (i / 2) * (j + 1);
        }
    }
    
}



