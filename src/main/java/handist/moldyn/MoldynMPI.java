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

import mpi.MPI;
import mpi.MPIException;

public class MoldynMPI extends Md {

    public static int interactions = 0;
    public static int nprocess;
    public static int rank;

    public static void main(String argv[]) throws MPIException, IOException {
        if (argv.length != 4) {
            System.err.println("Usage: java -cp [...] mpjexpress.moldyn.MoldynMPI " + "<data size index(0or1or2)>");
            return;
        }
        final int problemSize = Integer.parseInt(argv[3]);

        /* Initialise MPI */
        MPI.Init(argv);
        rank = MPI.COMM_WORLD.Rank();
        nprocess = MPI.COMM_WORLD.Size();

        final MoldynMPI m = new MoldynMPI();
        m.runBenchmarks(problemSize);

        /* Finalise MPI */
        MPI.Finalize();
    }

    public Particle one[] = null;

    double[] tmp_xforce;
    double[] tmp_yforce;
    double[] tmp_zforce;
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

        for (i = 0 + rank; i < mdsize; i += nprocess) {
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
    public void initialize(int mm) {
        /* Parameter determination */
        mdsize = mm * mm * mm * 4;
        one = new Particle[mdsize];

        side = Math.pow((mdsize / den), 0.3333333);
        rcoff = mm / 4.0;

        final double a = side / mm;
        sideh = side * 0.5;
        hsq = h * h;
        hsq2 = hsq * 0.5;
        rcoffs = rcoff * rcoff;
        tscale = 16.0 / (1.0 * mdsize - 1.0);
        vaver = 1.13 * Math.sqrt(tref / 24.0);
        vaverh = vaver * h;

        /* temporary arrays for MPI operations */

        tmp_xforce = new double[mdsize];
        tmp_yforce = new double[mdsize];
        tmp_zforce = new double[mdsize];

        tmp_epot = new double[1];
        tmp_vir = new double[1];
        tmp_interactions = new int[1];

        /* Particle Generation */

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

        /* MD simulation */
    }

    @Override
    protected void reduce() {
        double start, end;

        for (i = 0; i < mdsize; i++) {
            tmp_xforce[i] = one[i].xforce;
            tmp_yforce[i] = one[i].yforce;
            tmp_zforce[i] = one[i].zforce;
        }

        start = System.nanoTime();
        MPI.COMM_WORLD.Barrier();
        end = System.nanoTime();
        barrier_ns += (end - start);

        start = System.nanoTime();

        MPI.COMM_WORLD.Allreduce(tmp_xforce, 0, tmp_xforce, 0, mdsize, MPI.DOUBLE, MPI.SUM);
        MPI.COMM_WORLD.Allreduce(tmp_yforce, 0, tmp_yforce, 0, mdsize, MPI.DOUBLE, MPI.SUM);
        MPI.COMM_WORLD.Allreduce(tmp_zforce, 0, tmp_zforce, 0, mdsize, MPI.DOUBLE, MPI.SUM);

        for (i = 0; i < mdsize; i++) {
            one[i].xforce = tmp_xforce[i];
            one[i].yforce = tmp_yforce[i];
            one[i].zforce = tmp_zforce[i];
        }

        tmp_epot[0] = epot;
        tmp_vir[0] = vir;
        tmp_interactions[0] = interactions;

        MPI.COMM_WORLD.Allreduce(tmp_epot, 0, tmp_epot, 0, 1, MPI.DOUBLE, MPI.SUM);
        MPI.COMM_WORLD.Allreduce(tmp_vir, 0, tmp_vir, 0, 1, MPI.DOUBLE, MPI.SUM);
        MPI.COMM_WORLD.Allreduce(tmp_interactions, 0, tmp_interactions, 0, 1, MPI.INT, MPI.SUM);

        epot = tmp_epot[0];
        vir = tmp_vir[0];
        interactions = tmp_interactions[0];

        end = System.nanoTime();
        reduce_ns += (end - start);
    }

    @Override
    protected void tidyup() {
        one = null;
        System.gc();
    }

    @Override
    protected void updateParams() {
        double sum = 0.0;

        for (i = 0; i < mdsize; i++) {
            sum = sum + one[i].mkekin(hsq2); /* scale forces, update velocities */
        }

        ekin = sum / hsq;

        vel = 0.0;
        count = 0.0;

        for (i = 0; i < mdsize; i++) {
            vel = vel + one[i].velavg(vaverh, h); /* average velocity */
        }

        vel = vel / h;

        /* tmeperature scale if required */
        if ((move < istop) && (((move + 1) % irep) == 0)) {
            sc = Math.sqrt(tref / (tscale * ekin));
            for (i = 0; i < mdsize; i++) {
                one[i].dscal(sc);
            }
            ekin = tref / tscale;
        }
    }
}
