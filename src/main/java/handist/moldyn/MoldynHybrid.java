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

import static apgas.Constructs.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.RangedList;
import handist.collections.RangedListProduct;
import handist.collections.dist.CachableChunkedList;
import handist.collections.dist.Reducer;
import handist.collections.dist.TeamedPlaceGroup;
import mpi.MPI;
import mpi.MPIException;

public class MoldynHybrid extends Md implements Serializable {

    private static final class LocalStatics {
        double epot, vir;
        int interactions;
    }

    static class MyAccumM extends AccumulatorManager<Particle, Sp> {
        public MyAccumM(ChunkedList<Particle> target) {
            super(target);
        }

        @Override
        protected Sp init() {
            return new Sp();
        }

        @Override
        protected void reduce(Particle target, Sp copy) {
            target.xforce += copy.x;
            copy.x = 0;
            target.yforce += copy.y;
            copy.y = 0;
            target.zforce += copy.z;
            copy.z = 0;
        }
    }

    static class MyReducer extends Reducer<MyReducer, Particle> {
        double val;
        final Function<Particle, Double> func;

        MyReducer(Function<Particle, Double> func) {
            this.func = func;
        }

        @Override
        public void merge(MyReducer reducer) {
            this.val += reducer.val;
        }

        @Override
        public MyReducer newReducer() {
            return new MyReducer(func);
        }

        @Override
        public void reduce(Particle input) {
            val += func.apply(input);
        }
    }

    static class Sp implements Serializable {
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;

        Sp() {
        }

        Sp(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final long serialVersionUID = 1364008814489556197L;

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java -cp [...] handist.moldyn.MoldynHybrid "
                    + "<data size index(0or1or2)> <number of workers> <number of divide>");
            return;
        }
        final int problemSize = Integer.parseInt(args[0]);
        final int workers = Integer.parseInt(args[1]);
        final int divides = Integer.parseInt(args[2]);

        final MoldynHybrid m0 = new MoldynHybrid();
        m0.Nworkers = workers;
        m0.Ndivide = divides;

        m0.runBenchmarks(problemSize);
    }

    private LongRange allRange;
    private TeamedPlaceGroup placeGroup;
    private RangedList<Particle> one;
    private CachableChunkedList<Particle> oneX;

    private int Nworkers; // NOTE: if static variable, remote Nworkers cannot be set in main()
    private int Ndivide;

    private transient int prevInteractions;

    transient MyAccumM myAccM;

    @Override
    protected void domove() {
        oneX.parallelForEach(Nworkers, (p) -> {
            p.domove(side);
        });
    }

    @Override
    protected void force() {
        epot = 0.0;
        vir = 0.0;
        prevInteractions = interactions;
        interactions = 0;

        // split but sequential exec
        final List<List<RangedListProduct<Particle, Particle>>> prodsX = new RangedListProduct<>(one, one, true)
                .splitNM(Ndivide, Ndivide, placeGroup.rank(), placeGroup.size(), Nworkers, new java.util.Random(0));
        final List<LocalStatics> lss = new ArrayList<>(Nworkers);
        finish(() -> {
            for (final List<RangedListProduct<Particle, Particle>> prods : prodsX) {
                final LocalStatics ls = new LocalStatics();
                lss.add(ls);
                async(() -> {
                    for (final RangedListProduct<Particle, Particle> prod : prods) {
                        prod.forEachRow((Particle p1, RangedList<Particle> pairs) -> {
                            force1(p1, pairs, side, rcoff, ls);
                        });
                    }
                });
            }
        });

        myAccM.parallelMerge(Nworkers);
        for (final LocalStatics ls : lss) {
            epot += ls.epot;
            vir += ls.vir;
            interactions += ls.interactions;
        }
    }

    private void force1(Particle p0, RangedList<Particle> pairs, double side, double rcoff, LocalStatics s) {
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

        final RangedList<Sp> fs = myAccM.getCopy(pairs.getRange());
        final Iterator<Sp> fiterator = fs != null ? fs.iterator() : null;
        double vir0 = 0.0, epot0 = 0.0;
        int inters0 = 0;
        for (final Particle p1 : pairs) {
            final Sp p1f = fiterator.next();
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

                epot0 = epot0 + (rrd6 - rrd3);
                r148 = rrd7 - 0.5 * rrd4;
                vir0 = vir0 - rd * r148;

                forcex = xx * r148;
                fxi += forcex;
                p1f.x -= forcex;

                forcey = yy * r148;
                fyi += forcey;
                p1f.y -= forcey;

                forcez = zz * r148;
                fzi += forcez;
                p1f.z -= forcez;

                inters0++;
            }
        }
        s.epot += epot0;
        s.vir += vir0;
        s.interactions += inters0;
        synchronized (p0) {
            p0.xforce = p0.xforce + fxi;
            p0.yforce = p0.yforce + fyi;
            p0.zforce = p0.zforce + fzi;
        }

    }

    @Override
    @SuppressWarnings("unused")
    public void initialize(final int mm) {
        /* Particle Generation */
        placeGroup = TeamedPlaceGroup.getWorld();
        allRange = new LongRange(0, mdsize);
        oneX = new CachableChunkedList<>(placeGroup);
        one = new Chunk<>(allRange);
        oneX.add(one);

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

        /* share chunks */
        final CachableChunkedList<Particle> oneX0 = oneX;
        placeGroup.broadcastFlat(() -> { // not transport this (runner) here
            if (placeGroup.rank() == 0) {
                oneX0.share(allRange);
            } else {
                oneX0.share(new LongRange(0, 0));
            }
        });

        if (placeGroup.rank() == 0 && DEBUG) {
            debugPrint("#Initialized");
        }
    }

    @Override
    protected void reduce() {
        oneX.allreduce((p) -> new Sp(p.xforce, p.yforce, p.zforce), (p, sp) -> {
            p.xforce += sp.x;
            p.yforce += sp.y;
            p.zforce += sp.z;
        });

        epot = placeGroup.allReduce1(epot, MPI.SUM);
        vir = placeGroup.allReduce1(vir, MPI.SUM);
        interactions = placeGroup.allReduce1(interactions, MPI.SUM);
        interactions += prevInteractions;
    }

    // main routine
    @Override
    public void runIters() throws MPIException {
        placeGroup.broadcastFlat(() -> {
            super.runIters();
        });
    }

    @Override
    protected void tidyup() {
        oneX.remove(allRange);
        System.gc();
    }

    @Override
    protected void updateParams() {
        /* scale forces, update velocities */
        final MyReducer ekinReduce = new MyReducer((p) -> p.mkekin(hsq2));
        oneX.parallelReduce(ekinReduce);
        ekin = ekinReduce.val / hsq;

        /* average velocity */
        vel = 0.0;
        count = 0.0;
        final MyReducer veravgReduce = new MyReducer((p) -> p.velavg(vaverh, h));
        oneX.parallelReduce(Nworkers, veravgReduce);
        vel = veravgReduce.val / h;

        /* tmeperature scale if required */
        if ((move < istop) && (((move + 1) % irep) == 0)) {
            sc = Math.sqrt(tref / (tscale * ekin));
            oneX.parallelForEach(Nworkers, (p) -> {
                p.dscal(sc);
            });
            ekin = tref / tscale;
        }
    }
}
