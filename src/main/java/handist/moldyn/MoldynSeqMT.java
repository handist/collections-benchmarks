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

import java.io.File;
import java.io.FileWriter;
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
import handist.collections.dist.Reducer;
import mpi.MPIException;

public class MoldynSeqMT implements Serializable {

    static class DoubleBox {
        double val;
    }

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

        public void domove(double side) {

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

        @Override
        public String toString() {
            return String.format(
                    "coord(%5.3f, %5.3f, %5.3f) \t velocity(%3.3f, %3.3f, %3.3f) \t force(%2.3f, %2.3f, %2.3f)",
                    xcoord * 1000, ycoord * 1000, zcoord * 1000, xvelocity * 10000, yvelocity * 10000,
                    zvelocity * 10000, xforce * 10000, yforce * 10000, zforce * 10000);
        }

        public double velavg(double vaverh, double h) {

            double velt;
            double sq;

            sq = Math.sqrt(xvelocity * xvelocity + yvelocity * yvelocity + zvelocity * zvelocity);

            if (sq > vaverh) {
                MoldynSeqMT.count = MoldynSeqMT.count + 1.0;
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

    static class Sp {
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;
    }

    public static int datasizes[] = { 8, 13 };
    public static double refval[] = { 1731.4306625334357, 7397.392307839352 };
    private static final long serialVersionUID = 1364008814489556197L;
    public static double epot = 0.0; // potential energy
    public static double ekin = 0.0; // kinematic energy

    public static double vir = 0.0; // virial
    public static double count = 0.0; // ???
    public static int interactions = 0; // count of interactions between particles
    static int Nworkers; // num workers
    static int Ndivide;

    private static final boolean DEBUG = false;

    private static void printUsage() {
        System.err.println("Usage: java -cp [...] handist.moldyn.MoldynSeqMT "
                + "<data size index(0or1)> <number of workers> <number of divide> <result filename");
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            printUsage();
            return;
        }
        final int problemSize = Integer.parseInt(args[0]);
        Nworkers = Integer.parseInt(args[1]);
        Ndivide = Integer.parseInt(args[2]);
        final String fileName = args[3];

        final MoldynSeqMT m0 = new MoldynSeqMT();
        
        try {
            System.out.println("start warmup for " + m0.warmup + " times");
            for (int i = 0; i < m0.warmup; i++) {
                System.out.println("##################################################");
                System.out.println("warmup " + (i + 1) + "/" + m0.warmup);
                m0.initialise(datasizes[0]);
                m0.runiters(true);
                // m.tidyup();
            }

            System.out.println("##################################################");
            System.out.println("main run");
            final MoldynSeqMT m = new MoldynSeqMT();
            m.initialise(datasizes[problemSize]);
            final long start = System.nanoTime();
            m.runiters(false);
            final long end = System.nanoTime();
            m.validate(problemSize);
            System.out.println("############## handist 2CProdMT time: " + (end - start) / 1.0e9);
            m.tidyup();
            
            File file = new File(fileName);
            file.createNewFile();
            FileWriter fw = new FileWriter(file, true);
            fw.write((end-start)/1.0e9 + "\n");
            fw.close();
        } catch (final MultipleException me) {
            me.printStackTrace();
        }
    }

    final double den = 0.83134; // density
    final double tref = 0.722;

    final double h = 0.064;
    public List<LongRange> ranges;
    public LongRange allRange;
    public Chunk<Particle> one;
    public ChunkedList<Particle> oneX;

    public int nprocess;
    public int nchunks;

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

    int warmup = 5;

    Random randnum;
    transient MyAccumM myAccM;

    private final void force1(Particle p0, RangedList<Particle> pairs, double side, double rcoff, LocalStatics s) {
        double sideh;
        double rcoffs;

        double xi, yi, zi, fxi, fyi, fzi;

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
            double xx = xi - p1.xcoord;
            double yy = yi - p1.ycoord;
            double zz = zi - p1.zcoord;

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

            final double rd = xx * xx + yy * yy + zz * zz;
            double rrd, rrd2, rrd3, rrd4, rrd6, rrd7, r148;
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
                final double forcex = xx * r148;
                fxi += forcex;
                p1f.x -= forcex;
                final double forcey = yy * r148;
                fyi += forcey;
                p1f.y -= forcey;
                final double forcez = zz * r148;
                fzi += forcez;
                p1f.z -= forcez;
                inters0++;
            }
        }
        synchronized (this) {
            s.epot += epot0;
            s.vir += vir0;
            s.interactions += inters0;
            p0.xforce = p0.xforce + fxi;
            p0.yforce = p0.yforce + fyi;
            p0.zforce = p0.zforce + fzi;
        }

    }

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
        allRange = new LongRange(0, mdsize);
        oneX = new ChunkedList<>();
        one = new Chunk<>(allRange);
        oneX.add(one);

        final double a = side / mm;
        ijk = 0;
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

        final int iseed = 0;
        final double v1 = 0.0;
        final double v2 = 0.0;

        randnum = new Random(iseed, v1, v2);

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

        for (final Particle p : one) {// .forEach((p) -> {
            sp.x += p.xvelocity;
            sp.y += p.yvelocity;
            sp.z += p.zvelocity;
        } // );

        sp.x = sp.x / mdsize;
        sp.y = sp.y / mdsize;
        sp.z = sp.z / mdsize;

        for (final Particle p : one) {// one.forEach((p) -> {
            p.xvelocity -= sp.x;
            p.yvelocity -= sp.y;
            p.zvelocity -= sp.z;
            ekin += p.xvelocity * p.xvelocity;
            ekin += p.yvelocity * p.yvelocity;
            ekin += p.zvelocity * p.zvelocity;
        } // );

        sc = h * Math.sqrt(tref / (tscale * ekin));

        for (final Particle p : one) {// one.forEach((p) -> {
            p.xvelocity *= sc;
            p.yvelocity *= sc;
            p.zvelocity *= sc;
        } // );

        /* share chunks */
        if (DEBUG) {
            System.out.println("#Initialized");
            System.out.println(" " + one.get(0));
        }
    }

    // main routine
    public void runiters(boolean isWarmup) throws MPIException {
        final ChunkedList<Particle> oneX = new ChunkedList<>();
        oneX.add(one);
        myAccM = new MyAccumM(oneX);
        move = 0;
        for (move = 0; move < movemx; move++) {
            // ===================================================================================================
            /* move the particles and update velocities, no use global variables */
            oneX.parallelForEach(Nworkers, (p) -> {
                p.domove(side);
            });

            if (DEBUG) {
                System.out.println(" #after domove");
                System.out.println(" " + one.get(0));
                System.out.println(" ekin:" + ekin + "/epot:" + epot);
            }

            // ===================================================================================================
            /* compute forces */
            epot = 0.0;
            vir = 0.0;

            // split but sequential exec

            final List<List<RangedListProduct<Particle, Particle>>> prodsX = new RangedListProduct<>(one, one, true)
                    .splitN(Ndivide, Ndivide, Nworkers, false);
            final List<LocalStatics> lss = new ArrayList<>();
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

            if (DEBUG) {
                System.out.println(" #after force");
                System.out.println(" " + one.get(0));
                System.out.println(" ekin:" + ekin + "/epot:" + epot);
            }

            // ===================================================================================================
            /* scale forces, update velocities */
            
            // TODO reduce
            final MyReducer ekinReduce = new MyReducer((p) -> {
                return p.mkekin(hsq2);
            });
            oneX.parallelReduce(ekinReduce);
            ekin = ekinReduce.val / hsq;

            if (DEBUG) {
                System.out.println(" #after mkekin");
                System.out.println(" " + one.get(0));
                System.out.println(" ekin:" + ekin + "/epot:" + epot);
            }

            // ===================================================================================================
            /* average velocity */
            double vel = 0.0;
            count = 0.0;

            // TODO reduce
            final MyReducer veravgReduce = new MyReducer((p) -> {
                return p.velavg(vaverh, h);
            });
            oneX.parallelReduce(Nworkers, veravgReduce);
            vel = veravgReduce.val / h;

            if (DEBUG) {
                System.out.println(" #after velavg");
                System.out.println(" " + one.get(0));
                System.out.println(" ekin:" + ekin + "/epot:" + epot);
            }

            // ===================================================================================================
            /* tmeperature scale if required */
            if ((move < istop) && (((move + 1) % irep) == 0)) {
                sc = Math.sqrt(tref / (tscale * ekin));
                oneX.parallelForEach(Nworkers, (p) -> {
                    p.dscal(sc);
                });
                ekin = tref / tscale;
            }

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

                System.out.println("#Iteration " + move);
                System.out.println(" interactions : " + interactions);
                System.out.println(" total energy : " + etot);
                // System.out.println(" average vel : " + vel);
            }
        }
    }

    private void tidyup() {
        one = null;
        System.gc();
    }

    private void validate(int size) {
        final double dev = Math.abs(ek - refval[size]);
        if (dev > 1.0e-12) {
            System.out.println("Validation failed");
            System.out.println("Kinetic Energy = " + ek + "  " + refval[size] + " diff:" + dev + "  " + size);
        }
    }
}
