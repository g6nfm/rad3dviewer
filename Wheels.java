
public class Wheels {
    int ground;
    int mast;
    int rc[] = {70, 70, 70};
    float size;
    float depth;
 
    public Wheels() {
        size = 2.0F;
        depth = 3F;
        ground = 0;
        mast = 0;
    }


    public void setrims(int i, int j, int k, int l, int i1) {
        rc[0] = i;
        rc[1] = j;
        rc[2] = k;
        size = l / 10F;
        depth = i1 / 10F;
    }

    
 
    public void make(Medium medium, Plane aplane[], int indx, int wx, int wy, int wz, int wrot, int rmsize,
                     int rmdepth, int gwgr, boolean setupOnly) {


        int ai[] = new int[16];
        int ai1[] = new int[16];
        int ai2[] = new int[16];
        int ai3[] = {45, 45, 45};
        int pwx = 0;
        float f = rmsize / 10F;
        float f1 = rmdepth / 10F;

        if (wrot == 11) {
            //pwx = (int) (wx + 4F * f);
            pwx = wx;
        }

        byte byte0 = -1;
        if (wx < 0) {
            byte0 = 1;
        }
        int j2 = 0;
        do {
            ai[j2] = (int) (wx - 4F * f);
        } while (++j2 < 16);



        ai1[0] = (int) (wy - 12F * f1);
        ai2[0] = (int) (wz + 5F * f1);
        ai1[1] = (int) (wy - 12F * f1);
        ai2[1] = (int) (wz - 5F * f1);
        ai1[2] = (int) (wy - 5F * f1);
        ai2[2] = (int) (wz - 12F * f1);
        ai1[3] = (int) (wy + 5F * f1);
        ai2[3] = (int) (wz - 12F * f1);
        ai1[4] = (int) (wy + 12F * f1);
        ai2[4] = (int) (wz - 5F * f1);
        ai1[5] = (int) (wy + 12F * f1);
        ai2[5] = (int) (wz + 5F * f1);
        ai1[6] = (int) (wy + 5F * f1);
        ai2[6] = (int) (wz + 12F * f1);
        ai1[7] = (int) (wy - 5F * f1);
        ai2[7] = (int) (wz + 12F * f1);
        ai1[8] = wy;
        ai2[8] = (int) (wz + 10F * size);
        ai1[9] = (int) (wy + 8.6600000000000001D * size);
        ai2[9] = (int) (wz + 5F * size);
        ai1[10] = (int) (wy + 8.6600000000000001D * size);
        ai2[10] = (int) (wz - 5F * size);
        ai1[11] = wy;
        ai2[11] = (int) (wz - 10F * size);
        ai1[12] = (int) (wy - 8.6600000000000001D * size);
        ai2[12] = (int) (wz - 5F * size);
        ai1[13] = (int) (wy - 8.6600000000000001D * size);
        ai2[13] = (int) (wz + 5F * size);
        ai1[14] = wy;
        ai2[14] = (int) (wz + 10F * size);
        ai1[15] = (int) (wy - 5F * f1);
        ai2[15] = (int) (wz + 12F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 16, ai3, false, gwgr, 0, pwx, wy, wz, 7, 0, false, 0, false);
        mast++;
        aplane[indx].master = mast;
        indx++;
        ai[2] = (int) (wx - depth * f);
        ai1[2] = wy;
        ai2[2] = wz;
        j2 = -16;
        if (gwgr == 21) {
            j2 = -17;
        }
        ai1[0] = wy;
        ai2[0] = (int) (wz + 10F * size);
        ai1[1] = (int) (wy + 8.6600000000000001D * size);
        ai2[1] = (int) (wz + 5F * size);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 3, rc, false, j2, 0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai1[0] = (int) (wy + 8.6600000000000001D * size);
        ai2[0] = (int) (wz + 5F * size);
        ai1[1] = (int) (wy + 8.6600000000000001D * size);
        ai2[1] = (int) (wz - 5F * size);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 3, rc, false, j2, 0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai1[0] = (int) (wy + 8.6600000000000001D * size);
        ai2[0] = (int) (wz - 5F * size);
        ai1[1] = wy;
        ai2[1] = (int) (wz - 10F * size);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 3, rc, false, j2, 0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai1[0] = wy;
        ai2[0] = (int) (wz - 10F * size);
        ai1[1] = (int) (wy - 8.6600000000000001D * size);
        ai2[1] = (int) (wz - 5F * size);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 3, rc, false, j2, 0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai1[0] = (int) (wy - 8.6600000000000001D * size);
        ai2[0] = (int) (wz - 5F * size);
        ai1[1] = (int) (wy - 8.6600000000000001D * size);
        ai2[1] = (int) (wz + 5F * size);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 3, rc, false, j2, 0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai1[0] = (int) (wy - 8.6600000000000001D * size);
        ai2[0] = (int) (wz + 5F * size);
        ai1[1] = wy;
        ai2[1] = (int) (wz + 10F * size);




        aplane[indx] = new Plane(medium, ai, ai2, ai1, 3, rc, false, j2, 0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy - 12F * f1);
        ai2[0] = (int) (wz + 5F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy - 12F * f1);
        ai2[1] = (int) (wz - 5F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy - 12F * f1);
        ai2[2] = (int) (wz - 5F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy - 12F * f1);
        ai2[3] = (int) (wz + 5F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, -1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy - 5F * f1);
        ai2[0] = (int) (wz - 12F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy - 12F * f1);
        ai2[1] = (int) (wz - 5F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy - 12F * f1);
        ai2[2] = (int) (wz - 5F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy - 5F * f1);
        ai2[3] = (int) (wz - 12F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, 1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy - 5F * f1);
        ai2[0] = (int) (wz - 12F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy + 5F * f1);
        ai2[1] = (int) (wz - 12F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy + 5F * f1);
        ai2[2] = (int) (wz - 12F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy - 5F * f1);
        ai2[3] = (int) (wz - 12F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, -1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy + 12F * f1);
        ai2[0] = (int) (wz - 5F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy + 5F * f1);
        ai2[1] = (int) (wz - 12F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy + 5F * f1);
        ai2[2] = (int) (wz - 12F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy + 12F * f1);
        ai2[3] = (int) (wz - 5F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, 1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy + 12F * f1);
        ai2[0] = (int) (wz - 5F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy + 12F * f1);
        ai2[1] = (int) (wz + 5F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy + 12F * f1);
        ai2[2] = (int) (wz + 5F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy + 12F * f1);
        ai2[3] = (int) (wz - 5F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, -1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        if (ground < (int) (wy + 12F * f1 + 1.0F)) {
            ground = (int) (wy + 12F * f1 + 1.0F);
        }
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy + 5F * f1);
        ai2[0] = (int) (wz + 12F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy + 12F * f1);
        ai2[1] = (int) (wz + 5F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy + 12F * f1);
        ai2[2] = (int) (wz + 5F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy + 5F * f1);
        ai2[3] = (int) (wz + 12F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, 1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy + 5F * f1);
        ai2[0] = (int) (wz + 12F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy - 5F * f1);
        ai2[1] = (int) (wz + 12F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy - 5F * f1);
        ai2[2] = (int) (wz + 12F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy + 5F * f1);
        ai2[3] = (int) (wz + 12F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, -1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;
        ai[0] = (int) (wx - 4F * f);
        ai1[0] = (int) (wy - 12F * f1);
        ai2[0] = (int) (wz + 5F * f1);
        ai[1] = (int) (wx - 4F * f);
        ai1[1] = (int) (wy - 5F * f1);
        ai2[1] = (int) (wz + 12F * f1);
        ai[2] = (int) (wx + 4F * f);
        ai1[2] = (int) (wy - 5F * f1);
        ai2[2] = (int) (wz + 12F * f1);
        ai[3] = (int) (wx + 4F * f);
        ai1[3] = (int) (wy - 12F * f1);
        ai2[3] = (int) (wz + 5F * f1);
        aplane[indx] = new Plane(medium, ai, ai2, ai1, 4, ai3, false, gwgr, 1 * byte0, pwx, wy, wz, 7, 0, false, 0, false);
        indx++;



    }

}
