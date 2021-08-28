/**************************************************************************
 *                                                                         *
 *             Java Grande Forum Benchmark Suite - MPJ Version 1.0         *
 *                                                                         *
 *                            produced by                                  *
 *                                                                         *
 *                  Java Grande Benchmarking Project                       *
 *                                                                         *
 *                                at                                       *
 *                                                                         *
 *                Edinburgh Parallel Computing Centre                      *
 *                                                                         *
 *                email: epcc-javagrande@epcc.ed.ac.uk                     *
 *                                                                         *
 *                  Original version of this code by                       *
 *                         Dieter Heermann                                 *
 *                       converted to Java by                              *
 *                Lorna Smith  (l.smith@epcc.ed.ac.uk)                     *
 *                   (see copyright notice below)                          *
 *                                                                         *
 *      This version copyright (c) The University of Edinburgh, 2001.      *
 *                         All rights reserved.                            *
 *                                                                         *
 **************************************************************************/
package handist.moldyn;

import java.io.IOException;
import java.io.Serializable;

public class MoldynSimpleSeq extends Md implements Serializable {

    private static final long serialVersionUID = -3718955330792217659L;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java -cp [...] handist.moldyn.MoldynSimpleSeq " + "<data size index(0or1or2)>");
            return;
        }
        final int sizeIndex = Integer.parseInt(args[0]);

        final MoldynSimpleSeq m = new MoldynSimpleSeq();
        m.runBenchmarks(sizeIndex);
    }

    private Particle[] one;

    double[] tmp_epot;
    double[] tmp_vir;
    int[] tmp_interactions;

    @Override
    protected void domove() {
        for (i = 0; i < mdsize; i++) {
            one[i].domove(side);
        }
    }

    @Override
    protected void force() {
        epot = 0.0;
        vir = 0.0;

        for (int i = 0; i < mdsize; i++) {
            force1(one[i], side, rcoff, mdsize, i);
        }
    }

    public void force1(Particle p0, double side, double rcoff, int mdsize, int x) {

        double sideh;
        double rcoffs;

        double xx, yy, zz, xi, yi, zi, fxi, fyi, fzi;
        double rd, rrd, rrd2, rrd3, rrd4, rrd6, rrd7, r148;
        double forcex, forcey, forcez;

        int i;

        sideh = 0.5 * side;
        rcoffs = rcoff * rcoff;

        xi = p0.xcoord;
        yi = p0.ycoord;
        zi = p0.zcoord;
        fxi = 0.0;
        fyi = 0.0;
        fzi = 0.0;

        for (i = x + 1; i < mdsize; i++) {
            xx = xi - one[i].xcoord;
            yy = yi - one[i].ycoord;
            zz = zi - one[i].zcoord;

            if (xx < (-sideh)) {
                xx = xx + side;
            }
            if (xx > (sideh)) {
                xx = xx - side;
            }
            if (yy < (-sideh)) {
                yy = yy + side;
            }
            if (yy > (sideh)) {
                yy = yy - side;
            }
            if (zz < (-sideh)) {
                zz = zz + side;
            }
            if (zz > (sideh)) {
                zz = zz - side;
            }

            rd = xx * xx + yy * yy + zz * zz;

            if (rd <= rcoffs) {
                rrd = 1.0 / rd;
                rrd2 = rrd * rrd;
                rrd3 = rrd2 * rrd;
                rrd4 = rrd2 * rrd2;
                rrd6 = rrd2 * rrd4;
                rrd7 = rrd6 * rrd;

                MoldynSimpleSeq.epot = MoldynSimpleSeq.epot + (rrd6 - rrd3);
                r148 = rrd7 - 0.5 * rrd4;

                MoldynSimpleSeq.vir = MoldynSimpleSeq.vir - rd * r148;

                forcex = xx * r148;
                fxi = fxi + forcex;
                one[i].xforce = one[i].xforce - forcex;

                forcey = yy * r148;
                fyi = fyi + forcey;
                one[i].yforce = one[i].yforce - forcey;

                forcez = zz * r148;
                fzi = fzi + forcez;
                one[i].zforce = one[i].zforce - forcez;

                MoldynSimpleSeq.interactions++;
            }
        }
        p0.xforce = p0.xforce + fxi;
        p0.yforce = p0.yforce + fyi;
        p0.zforce = p0.zforce + fzi;
    }

    @Override
    protected void initialize(int mm) {
        /* Parameter determination */
        one = new Particle[mdsize];

        final double a = side / mm;
        int ijk = 0;
        for (lg = 0; lg <= 1; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        one[ijk] = new Particle((i * a + lg * a * 0.5), (j * a + lg * a * 0.5), (k * a), 0.0, 0.0, 0.0,
                                0.0, 0.0, 0.0);
                        ijk = ijk + 1;
                    }
                }
            }
        }
        for (lg = 1; lg <= 2; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        one[ijk] = new Particle((i * a + (2 - lg) * a * 0.5), (j * a + (lg - 1) * a * 0.5),
                                (k * a + a * 0.5), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
                        ijk = ijk + 1;
                    }
                }
            }
        }

        /* Initialise velocities */
        randnum = new Random(0, 0.0, 0.0);

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one[i].xvelocity = r * randnum.v1;
            one[i + 1].xvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one[i].yvelocity = r * randnum.v1;
            one[i + 1].yvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one[i].zvelocity = r * randnum.v1;
            one[i + 1].zvelocity = r * randnum.v2;
        }

        /* velocity scaling */
        ekin = 0.0;
        double sp = 0.0;

        for (i = 0; i < mdsize; i++) {
            sp = sp + one[i].xvelocity;
        }
        sp = sp / mdsize;

        for (i = 0; i < mdsize; i++) {
            one[i].xvelocity = one[i].xvelocity - sp;
            ekin = ekin + one[i].xvelocity * one[i].xvelocity;
        }

        sp = 0.0;
        for (i = 0; i < mdsize; i++) {
            sp = sp + one[i].yvelocity;
        }
        sp = sp / mdsize;

        for (i = 0; i < mdsize; i++) {
            one[i].yvelocity = one[i].yvelocity - sp;
            ekin = ekin + one[i].yvelocity * one[i].yvelocity;
        }

        sp = 0.0;
        for (i = 0; i < mdsize; i++) {
            sp = sp + one[i].zvelocity;
        }
        sp = sp / mdsize;

        for (i = 0; i < mdsize; i++) {
            one[i].zvelocity = one[i].zvelocity - sp;
            ekin = ekin + one[i].zvelocity * one[i].zvelocity;
        }

        final double ts = tscale * ekin;
        sc = h * Math.sqrt(tref / ts);

        for (i = 0; i < mdsize; i++) {
            one[i].xvelocity = one[i].xvelocity * sc;
            one[i].yvelocity = one[i].yvelocity * sc;
            one[i].zvelocity = one[i].zvelocity * sc;
        }
    }

    @Override
    protected void reduce() {

    }

    @Override
    protected void tidyup() {
        one = null;
        System.gc();
    }

    @Override
    protected void updateParams() {
        /* scale forces, update velocities */
        double sum = 0.0;
        for (int i = 0; i < mdsize; i++) {
            sum += one[i].mkekin(hsq2);
        }
        ekin = sum / hsq;

        /* average velocity */
        double vel = 0.0;
        count = 0.0;

        for (int i = 0; i < mdsize; i++) {
            vel += one[i].velavg(vaverh, h);
        }

        vel = vel / h;

        /* tmeperature scale if required */
        if ((move < istop) && (((move + 1) % irep) == 0)) {
            sc = Math.sqrt(tref / (tscale * ekin));
            for (int i = 0; i < mdsize; i++) {
                one[i].dscal(sc);
            }
            ekin = tref / tscale;
        }
    }

}
