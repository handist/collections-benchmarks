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

import handist.collections.Chunk;
import handist.collections.LongRange;

public class MoldynSeq extends Md implements Serializable {

    static class Sp {
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;
    }

    private static final long serialVersionUID = 1364008814489556197L;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java -cp [...] handist.moldyn.MoldynSeq " + "<data size index(0or1)>");
            return;
        }
        final int problemSize = Integer.parseInt(args[0]);

        final MoldynSeq m = new MoldynSeq();
        m.runBenchmarks(problemSize);
    }

    public LongRange allRange;
    public Chunk<Particle> one;
    public int nprocess;
    public int nchunks;

    Random randnum;

    @Override
    protected void domove() {
        for (final Particle p : one) {
            p.domove(side);
        }
    }

    @Override
    protected void force() {
        epot = 0.0;
        vir = 0.0;

        one.forEach(allRange, (i1, p1) -> {
            force1(p1, side, rcoff, i1);
        });
    }

    private final void force1(Particle p0, double side, double rcoff, long x) {
        double sideh;
        double rcoffs;

        double xx, yy, zz, xi, yi, zi, fxi, fyi, fzi;
        double rd, rrd, rrd2, rrd3, rrd4, rrd6, rrd7, r148;
        double forcex, forcey, forcez;

        sideh = 0.5 * side;
        rcoffs = rcoff * rcoff;

        xi = p0.xcoord;
        yi = p0.ycoord;
        zi = p0.zcoord;
        fxi = 0.0;
        fyi = 0.0;
        fzi = 0.0;

        for (final Particle p1 : one.subList(0, x)) {
            xx = xi - p1.xcoord;
            yy = yi - p1.ycoord;
            zz = zi - p1.zcoord;

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

                epot = epot + (rrd6 - rrd3);
                r148 = rrd7 - 0.5 * rrd4;
                vir = vir - rd * r148;

                forcex = xx * r148;
                fxi += forcex;
                p1.xforce = p1.xforce - forcex;

                forcey = yy * r148;
                fyi += forcey;
                p1.yforce = p1.yforce - forcey;

                forcez = zz * r148;
                fzi += forcez;
                p1.zforce = p1.zforce - forcez;

                interactions++;
            }
        }
        p0.xforce = p0.xforce + fxi;
        p0.yforce = p0.yforce + fyi;
        p0.zforce = p0.zforce + fzi;
    }

    @Override
    public void initialize(final int mm) {
        /* Particle Generation */
        allRange = new LongRange(0, mdsize);
        // one = new ChunkedList<>();
        one = new Chunk<>(allRange);

        final double a = side / mm;
        int ijk = 0;
        for (lg = 0; lg <= 1; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        one.set(ijk, new Particle((i * a + lg * a * 0.5), (j * a + lg * a * 0.5), (k * a), 0.0, 0.0,
                                0.0, 0.0, 0.0, 0.0));
                        ijk++;
                    }
                }
            }
        }
        for (lg = 1; lg <= 2; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        one.set(ijk, new Particle((i * a + (2 - lg) * a * 0.5), (j * a + (lg - 1) * a * 0.5),
                                (k * a + a * 0.5), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
                        ijk++;
                    }
                }
            }
        }

        /* Initialise velocities */
        randnum = new Random(0, 0.0, 0.0);

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one.get(i).xvelocity = r * randnum.v1;
            one.get(i + 1).xvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one.get(i).yvelocity = r * randnum.v1;
            one.get(i + 1).yvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one.get(i).zvelocity = r * randnum.v1;
            one.get(i + 1).zvelocity = r * randnum.v2;
        }

        /* velocity scaling */
        final Sp sp = new Sp();
        ekin = 0.0;

        for (final Particle p : one) {
            sp.x += p.xvelocity;
            sp.y += p.yvelocity;
            sp.z += p.zvelocity;
        }

        sp.x = sp.x / mdsize;
        sp.y = sp.y / mdsize;
        sp.z = sp.z / mdsize;

        for (final Particle p : one) {
            p.xvelocity -= sp.x;
            p.yvelocity -= sp.y;
            p.zvelocity -= sp.z;
            ekin += p.xvelocity * p.xvelocity;
            ekin += p.yvelocity * p.yvelocity;
            ekin += p.zvelocity * p.zvelocity;
        }

        sc = h * Math.sqrt(tref / (tscale * ekin));

        for (final Particle p : one) {
            p.xvelocity *= sc;
            p.yvelocity *= sc;
            p.zvelocity *= sc;
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
        double sum = 0.0;

        for (final Particle p : one) {
            sum += p.mkekin(hsq2);
        }
        ekin = sum / hsq;

        /* average velocity */
        vel = 0.0;
        count = 0.0;

        for (final Particle p : one) {
            vel += p.velavg(vaverh, h);
        }
        vel = vel / h;

        /* tmeperature scale if required */
        if ((move < istop) && (((move + 1) % irep) == 0)) {
            sc = Math.sqrt(tref / (tscale * ekin));
            for (final Particle p : one) {
                p.dscal(sc);
            }
            ekin = tref / tscale;
        }
    }
}
