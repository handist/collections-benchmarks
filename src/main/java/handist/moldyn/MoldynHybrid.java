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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import apgas.MultipleException;
import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.RangedList;
import handist.collections.RangedListProduct;
import handist.collections.accumulator.Accumulator;
import handist.collections.accumulator.Accumulator.ThreadLocalAccumulator;
import handist.collections.accumulator.AccumulatorCompleteRange;
import handist.collections.dist.CachableChunkedList;
import handist.collections.dist.Reducer;
import handist.collections.dist.TeamedPlaceGroup;
import mpi.MPI;
import mpi.MPIException;

public class MoldynHybrid implements Serializable {

    private static final class LocalStatics {
        double epot, vir;
        int interactions;
    }

    static class MyReducer extends Reducer<MyReducer, Particle> {
        private static final long serialVersionUID = 2449999554090584509L;
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

    public final static class Particle implements Serializable {

        private static final long serialVersionUID = 4019649624174733374L;

        public double xcoord, ycoord, zcoord;

        public double xvelocity, yvelocity, zvelocity;
        public double xforce, yforce, zforce;

        public Particle(double xcoord, double ycoord, double zcoord, double xvelocity, double yvelocity,
                double zvelocity, double xforce, double yforce, double zforce) {
            this.xcoord = xcoord;
            this.ycoord = ycoord;
            this.zcoord = zcoord;
            this.xvelocity = xvelocity;
            this.yvelocity = yvelocity;
            this.zvelocity = zvelocity;
            this.xforce = xforce;
            this.yforce = yforce;
            this.zforce = zforce;

        }

        public void dscal(double sc) {

            xvelocity = xvelocity * sc;
            yvelocity = yvelocity * sc;
            zvelocity = zvelocity * sc;

        }

        public double mkekin(double hsq2) {

            double sumt = 0.0;

            xforce = xforce * hsq2;
            yforce = yforce * hsq2;
            zforce = zforce * hsq2;

            xvelocity = xvelocity + xforce;
            yvelocity = yvelocity + yforce;
            zvelocity = zvelocity + zforce;

            sumt = (xvelocity * xvelocity) + (yvelocity * yvelocity) + (zvelocity * zvelocity);
            return sumt;
        }

        public void move(double side) {

            xcoord = xcoord + xvelocity + xforce; // velocity, force is converted dimension value by deltatime(sc)
            ycoord = ycoord + yvelocity + yforce;
            zcoord = zcoord + zvelocity + zforce;

            if (xcoord < 0) {
                xcoord = xcoord + side;
            }
            if (xcoord > side) {
                xcoord = xcoord - side;
            }
            if (ycoord < 0) {
                ycoord = ycoord + side;
            }
            if (ycoord > side) {
                ycoord = ycoord - side;
            }
            if (zcoord < 0) {
                zcoord = zcoord + side;
            }
            if (zcoord > side) {
                zcoord = zcoord - side;
            }

            xvelocity = xvelocity + xforce;
            yvelocity = yvelocity + yforce;
            zvelocity = zvelocity + zforce;

            xforce = 0.0;
            yforce = 0.0;
            zforce = 0.0;

        }

        @Override
        public String toString() {
            return String.format(
                    "coord(%5.3f, %5.3f, %5.3f) \t velocity(%3.3f, %3.3f, %3.3f) \t force(%2.3f, %2.3f, %2.3f)",
                    xcoord * 10000, ycoord * 10000, zcoord * 10000, xvelocity * 10000, yvelocity * 10000,
                    zvelocity * 10000, xforce * 10000, yforce * 10000, zforce * 10000);
        }

        public void updateForce(Sp sp) {
            this.xforce += sp.x;
            sp.x = 0;
            this.yforce += sp.y;
            sp.y = 0;
            this.zforce += sp.z;
            sp.z = 0;
        }

        public double velavg(double vaverh, double h) {

            double velt;
            double sq;

            sq = Math.sqrt(xvelocity * xvelocity + yvelocity * yvelocity + zvelocity * zvelocity);

            if (sq > vaverh) {
                MoldynHybrid.count = MoldynHybrid.count + 1.0;
            }

            velt = sq;
            return velt;
        }
    }

    public final static class Random implements Serializable {

        private static final long serialVersionUID = 6984646650689608506L;
        public int iseed;
        public double v1, v2;

        public Random(int iseed, double v1, double v2) {
            this.iseed = iseed;
            this.v1 = v1;
            this.v2 = v2;
        }

        public double next() {

            double s, u1, u2, r;
            do {
                u1 = update();
                u2 = update();

                v1 = 2.0 * u1 - 1.0;
                v2 = 2.0 * u2 - 1.0;
                s = v1 * v1 + v2 * v2;

            } while (s >= 1.0);

            r = Math.sqrt(-2.0 * Math.log(s) / s);

            return r;

        }

        public double update() {

            double rand;
            final double scale = 4.656612875e-10;

            int is1, is2, iss2;
            final int imult = 16807;
            final int imod = 2147483647;

            if (iseed <= 0) {
                iseed = 1;
            }

            is2 = iseed % 32768;
            is1 = (iseed - is2) / 32768;
            iss2 = is2 * imult;
            is2 = iss2 % 32768;
            is1 = (is1 * imult + (iss2 - is2) / 32768) % (65536);

            iseed = (is1 * 32768 + is2) % imod;

            rand = scale * iseed;

            return rand;

        }
    }

    static class Sp implements Serializable {
        private static final long serialVersionUID = -7387311209302573067L;

        public static Sp newSp(long l) {
            return new Sp();
        }

        public double x = 0.0;
        public double y = 0.0;

        public double z = 0.0;

    }

    public static int datasizes[] = { 8, 13, 20 };
    public static double refval[] = { 1731.4306625334357, 7397.392307839352, -1 };

    private static final long serialVersionUID = 1364008814489556197L;

    public static double epot = 0.0; // potential energy
    public static double ekin = 0.0; // kinematic energy
    public static double vir = 0.0; // virial
    public static double count = 0.0; // ???
    public static int interactions = 0; // count of interactions between particles

    private static final boolean DEBUG = false;

    private static void force1(Particle p0, RangedList<Particle> pairs, double side, double rcoff,
            ThreadLocalAccumulator<Sp> tla, LocalStatics s) {
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

        final ChunkedList<Sp> fs = tla.acquire(pairs.getRange());
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

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            printUsage();
            return;
        }
        final int problemSize = Integer.parseInt(args[0]);
        final int divides = Integer.parseInt(args[1]);
        final int workers = Integer.parseInt(args[2]);

        try {
            final MoldynHybrid m0 = new MoldynHybrid();
            System.err.println("start warmup for " + m0.warmup + " times");
            m0.Ndivide = divides;
            m0.Nworkers = workers;
            for (int i = 0; i < m0.warmup; i++) {
                System.err.println("##################################################");
                System.err.println("warmup " + (i + 1) + "/" + m0.warmup);
                m0.initialise(datasizes[0]);
                m0.runiters();
                m0.tidyup();
            }
            final MoldynHybrid m = new MoldynHybrid();
            m.Ndivide = divides;
            m.Nworkers = workers;
            System.err.println("start main for " + m.mainLoop + " times");
            for (int i = 0; i < m.mainLoop; i++) {
                System.err.println("##################################################");
                System.err.println("main run");
                m.initialise(datasizes[problemSize]);
                // final long start = System.nanoTime();
                m.runiters();
                // final long end = System.nanoTime();
                m.validate(problemSize);
                System.err.println("############## handist MoldynHybrid time: " + m.total_ns / 1.0e9);
                m.tidyup();
                m.printResult(m.total_ns / 1.0e9);
            }
        } catch (final MultipleException me) {
            me.printStackTrace();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp [...] handist.moldyn.MoldynHybrid "
                + "<data size index(0or1)> <number of divide> <number of workers> ");
    }

    final double den = 0.83134;
    final double tref = 0.722;

    final double h = 0.064;
    public List<LongRange> ranges;
    public LongRange allRange;
    public TeamedPlaceGroup world;
    public RangedList<Particle> prl;

    public CachableChunkedList<Particle> particles;
    int mdsize; // number of particles
    int ijk, i, j, k, lg, move; // iteration variables
    double rcoff, rcoffs, side, sideh, hsq, hsq2;
    double r, tscale, sc, ek;
    double vaver, vaverh;

    double etot, temp, pres, rp; // results(total energy/temperature/pressure/???)
    int irep = 10;
    int istop = 19;
    int iprint = 10;
    int movemx = 50;
    int warmup = 2;

    int mainLoop = 4;
    private int Nworkers;

    private int Ndivide;
    Random randnum;

    transient Accumulator<Sp> myAcc;
    transient double total_ns, domove_ns, reduce_ns, others_ns; // timer

    transient double forceSplit_ns, forceCalc_ns, forceMerge_ns; // timer

    private void debugPrint() {
        System.err.println(prl.get(0));
        System.err.println("ekin:" + ekin + "/epot:" + epot);
    }

    @SuppressWarnings("unused")
    public void initialise(final int mm) {
        /* Parameter determination */
        mdsize = mm * mm * mm * 4;

        side = Math.pow((mdsize / den), 0.3333333);
        sideh = side * 0.5;
        rcoff = mm / 4.0;
        rcoffs = rcoff * rcoff;

        hsq = h * h;
        hsq2 = hsq * 0.5;
        tscale = 16.0 / (1.0 * mdsize - 1.0);
        vaver = 1.13 * Math.sqrt(tref / 24.0);
        vaverh = vaver * h;

        /* Particle Generation */
        world = TeamedPlaceGroup.getWorld();
        allRange = new LongRange(0, mdsize);
        particles = new CachableChunkedList<>(world);
        prl = new Chunk<>(allRange);
        particles.add(prl);

        final double a = side / mm;
        ijk = 0;
        for (lg = 0; lg <= 1; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        prl.set(ijk, new Particle((i * a + lg * a * 0.5), (j * a + lg * a * 0.5), (k * a), 0.0, 0.0,
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
                        prl.set(ijk, new Particle((i * a + (2 - lg) * a * 0.5), (j * a + (lg - 1) * a * 0.5),
                                (k * a + a * 0.5), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
                        ijk++;
                    }
                }
            }
        }

        /* Initialise velocities */

        final int iseed = 0;
        final double v1 = 0.0;
        final double v2 = 0.0;

        randnum = new Random(iseed, v1, v2);

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            prl.get(i).xvelocity = r * randnum.v1;
            prl.get(i + 1).xvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            prl.get(i).yvelocity = r * randnum.v1;
            prl.get(i + 1).yvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            prl.get(i).zvelocity = r * randnum.v1;
            prl.get(i + 1).zvelocity = r * randnum.v2;
        }

        /* velocity scaling */
        final Sp sp = new Sp();
        ekin = 0.0;

        for (final Particle p : prl) {
            sp.x += p.xvelocity;
            sp.y += p.yvelocity;
            sp.z += p.zvelocity;
        }

        sp.x = sp.x / mdsize;
        sp.y = sp.y / mdsize;
        sp.z = sp.z / mdsize;

        for (final Particle p : prl) {
            p.xvelocity -= sp.x;
            p.yvelocity -= sp.y;
            p.zvelocity -= sp.z;
            ekin += p.xvelocity * p.xvelocity;
            ekin += p.yvelocity * p.yvelocity;
            ekin += p.zvelocity * p.zvelocity;
        }

        sc = h * Math.sqrt(tref / (tscale * ekin));

        for (final Particle p : prl) {
            p.xvelocity *= sc;
            p.yvelocity *= sc;
            p.zvelocity *= sc;
        }

        /* share chunks */
        final CachableChunkedList<Particle> oneX0 = particles;
        world.broadcastFlat(() -> { // not transport this (runner) here
            if (world.rank() == 0) {
                oneX0.share(allRange);
            } else {
                oneX0.share();
            }
        });

        if (world.rank() == 0 && DEBUG) {
            debugPrint();
        }
    }

    private void printResult(double total) {
        domove_ns = domove_ns / 1.0e9;
        forceSplit_ns = forceSplit_ns / 1.0e9;
        forceCalc_ns = forceCalc_ns / 1.0e9;
        forceMerge_ns = forceMerge_ns / 1.0e9;
        reduce_ns = reduce_ns / 1.0e9;
        others_ns = others_ns / 1.0e9;
        System.out.println("Iter" + i + ";" + total + ";" + domove_ns + ";" + forceSplit_ns + ";" + forceCalc_ns + ";"
                + forceMerge_ns + ";" + reduce_ns + ";" + others_ns);
    }

    // main routine
    public void runiters() throws MPIException {
        world.broadcastFlat(() -> {
            final double start = System.nanoTime();
            runiters0();
            final double end = System.nanoTime();
            total_ns = (end - start);
        });
    }

    @SuppressWarnings("unused")
    public void runiters0() throws MPIException {
//        particles.forEachChunk((c) -> {
//            prl = c;
//        });

        particles.getChunk(allRange);
        myAcc = new AccumulatorCompleteRange<>(particles.ranges(), Sp::newSp);
        move = 0;

        long start, end;

        // split but sequential exec
        start = System.nanoTime();
        final RangedListProduct<Particle, Particle> prodsX = RangedListProduct.newProductTriangle(prl, prl)
                .teamedSplit(Ndivide, Ndivide, world, 0l);
        end = System.nanoTime();

        forceSplit_ns += (end - start);

        for (move = 0; move < movemx; move++) {
            // ===================================================================================================
            /* move the particles and update velocities, no use global variables */
            start = System.nanoTime();
            particles.parallelForEach(Nworkers, (p) -> {
                p.move(side);
            });
            end = System.nanoTime();
            domove_ns += (end - start);

            if (world.rank() == 0 && DEBUG) {
                debugPrint();
            }

            // ===================================================================================================
            /* compute forces */
            epot = 0.0;
            vir = 0.0;

//            world.barrier();

            final int prevInteractions = interactions;
            interactions = 0;

            start = System.nanoTime();
            final List<LocalStatics> lss = new ArrayList<>();
            for (int i = 0; i < Nworkers; i++) {
                lss.add(new LocalStatics());
            }

            prodsX.parallelForEachRow(myAcc, Nworkers, (p, pairs, tla, ls) -> {
                force1(p, pairs, side, rcoff, tla, ls);
            }, i -> lss.get(i));
//            finish(() -> {
//                int accIndex = 0;
//                for (final List<RangedListProduct<Particle, Particle>> prods : prodsX) {
//                    final LocalStatics ls = new LocalStatics();
//                    final ThreadLocalAccumulator<Sp> tla = accs.get(accIndex++);
//                    lss.add(ls);
//                    async(() -> {
//                        for (final RangedListProduct<Particle, Particle> prod : prods) {
//                            prod.forEachRow((Particle p1, RangedList<Particle> pairs) -> {
//                                force1(p1, pairs, side, rcoff, tla, ls);
//                            });
//                        }
//                    });
//                }
//            });
            end = System.nanoTime();
            forceCalc_ns += (end - start);

            start = System.nanoTime();
            particles.parallelAccept(Nworkers, myAcc, (p, sp) -> p.updateForce(sp));
            for (final LocalStatics ls : lss) {
                epot += ls.epot;
                vir += ls.vir;
                interactions += ls.interactions;
            }
            end = System.nanoTime();
            forceMerge_ns += (end - start);

            if (world.rank() == 0 && DEBUG) {
                debugPrint();
            }

            // ===================================================================================================
            /* allreduce */
            world.barrier();

            start = System.nanoTime();

            particles.allreduce((out, p) -> {
                out.writeDouble(p.xforce);
                out.writeDouble(p.yforce);
                out.writeDouble(p.zforce);
            }, (in, p) -> {
                p.xforce = in.readDouble();
                p.yforce = in.readDouble();
                p.zforce = in.readDouble();
            }, MPI.SUM);

            epot = world.allReduce1(epot, MPI.SUM);
            vir = world.allReduce1(vir, MPI.SUM);
            interactions = world.allReduce1(interactions, MPI.SUM);
            interactions += prevInteractions;

            world.barrier();

            end = System.nanoTime();
            reduce_ns += (end - start);

            if (world.rank() == 0 && DEBUG) {
                debugPrint();
            }

            // ===================================================================================================
            /* scale forces, update velocities */

            start = System.nanoTime();

            final MyReducer ekinReduce = new MyReducer((p) -> p.mkekin(hsq2));
            particles.parallelReduce(ekinReduce);
            ekin = ekinReduce.val / hsq;

            /* average velocity */
            double vel = 0.0;
            count = 0.0;

            final MyReducer veravgReduce = new MyReducer((p) -> p.velavg(vaverh, h));

            particles.parallelReduce(Nworkers, veravgReduce);
            vel = veravgReduce.val / h;

            /* tmeperature scale if required */
            if ((move < istop) && (((move + 1) % irep) == 0)) {
                sc = Math.sqrt(tref / (tscale * ekin));
                particles.parallelForEach(Nworkers, (p) -> {
                    p.dscal(sc);
                });
                ekin = tref / tscale;
            }

            end = System.nanoTime();
            others_ns += (end - start);

            // ===================================================================================================
            /* sum to get full potential energy and virial */
            if (((move + 1) % iprint) == 0) {
                ek = 24.0 * ekin;
                epot = 4.0 * epot;
                etot = ek + epot;
                temp = tscale * ekin;
                pres = den * 16.0 * (ekin - vir) / mdsize;
                vel = vel / mdsize;
                rp = (count / mdsize) * 100.0;
                if (world.rank() == 0) {
                    System.err.println("#Iteration " + move);
                    System.err.println(" interactions : " + interactions);
                    System.err.println(" total energy : " + etot);
                    System.err.println(" average vel : " + vel);
                }
            }
        }

    }

    private void tidyup() {
        prl = null;
        System.gc();
    }

    private void validate(int size) {
        final double dev = Math.abs(ek - refval[size]);
        if (dev > 1.0e-12) {
            System.err.println("Validation failed");
            System.err.println("Kinetic Energy = " + ek + "  " + refval[size] + " diff:" + dev + "  " + size);
        }
    }

}
