package handist.moldyn;

import java.io.Serializable;

public abstract class Md {

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
                    xcoord * 10000, ycoord * 10000, zcoord * 10000, xvelocity * 10000, yvelocity * 10000,
                    zvelocity * 10000, xforce * 10000, yforce * 10000, zforce * 10000);
        }

        public double velavg(double vaverh, double h) {
            double velt;
            double sq;

            sq = Math.sqrt(xvelocity * xvelocity + yvelocity * yvelocity + zvelocity * zvelocity);
            if (sq > vaverh) {
                count = count + 1.0;
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
            final int imult = 16807;
            final int imod = 2147483647;
            int is1, is2, iss2;

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

    protected static boolean DEBUG = false;

    public static int datasizes[] = { 8, 13, 26 };
    public static double refval[] = { 1731.4306625334357, 7397.392307839352, -1 };
    final static double den = 0.83134;
    final static double tref = 0.722;
    final static double h = 0.064;

    public static double epot = 0.0; // potential energy
    public static double ekin = 0.0; // kinematic energy
    public static double vir = 0.0; // virial
    public static double count = 0.0; // ???
    public static int interactions = 0; // count of interactions between particles

    int mdsize; // number of particles
    transient int i, j, k, lg, move; // iteration variables
    double rcoff, rcoffs, side, sideh, hsq, hsq2, vel;
    double r, tscale, sc, ek;
    double vaver, vaverh;
    double etot, temp, pres, rp; // results(total energy/temperature/pressure/???)

    transient final int irep = 10;
    transient final int istop = 19;
    transient final int iprint = 10;
    transient final int movemx = 50;
    transient final int warmup = 2;
    transient final int mainLoop = 10;

    transient double domove_ns, force_ns, reduce_ns, others_ns; // timer
    transient double forceSplit_ns, forceCalc_ns, forceMerge_ns; // timer

    Random randnum;

    protected void debugPrint(String head) {
        System.err.println(head);
        System.err.println(" ekin:" + ekin + "/epot:" + epot);
    }

    protected abstract void domove();

    protected abstract void force();

    protected abstract void initialize(int mm);

    protected void initialize0(int mm) {
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

        initialize(mm);

        if (DEBUG) {
            debugPrint("#Initialized");
        }
    }

    protected abstract void reduce();

    protected void runBenchmarks(int sizeIndex) {
        for (int i = 0; i < warmup; i++) {
            System.err.println("##################################################");
            System.err.println("warmup " + (i + 1) + "/" + warmup);
            initialize0(datasizes[0]);
            runIters();
            tidyup();
        }
        for (int i = 0; i < mainLoop; i++) {
            System.err.println("##################################################");
            System.err.println("main run " + (i + 1) + "/" + mainLoop);
            initialize0(datasizes[sizeIndex]);
            final long start = System.nanoTime();
            runIters();
            final long end = System.nanoTime();
            validate(sizeIndex);
            System.err.println("########################################## time: " + (end - start) / 1.0e9);
            tidyup();

            final double total = (end - start) / 1.0e9;
            domove_ns = domove_ns / 1.0e9;
            force_ns = force_ns / 1.0e9;
            forceSplit_ns = forceSplit_ns / 1.0e9;
            forceCalc_ns = forceCalc_ns / 1.0e9;
            forceMerge_ns = forceMerge_ns / 1.0e9;
            reduce_ns = reduce_ns / 1.0e9;
            others_ns = others_ns / 1.0e9;
            System.out.println("Iter" + i + ";" + total + ";" + domove_ns + ";" + force_ns + ";" + forceSplit_ns + ";"
                    + forceCalc_ns + ";" + forceMerge_ns + ";" + reduce_ns + ";" + others_ns);
        }
    }

    protected void runIters() {
        double start, end; // timer
        move = 0;
        domove_ns = 0;
        force_ns = 0;
        forceSplit_ns = 0;
        forceCalc_ns = 0;
        forceMerge_ns = 0;
        reduce_ns = 0;
        others_ns = 0;

        for (move = 0; move < movemx; move++) {
            // ===================================================================================================
            /* move the particles and update velocities, no use global variables */
            start = System.nanoTime();
            domove();
            end = System.nanoTime();
            domove_ns += (end - start);
            if (DEBUG) {
                debugPrint(" #after domove");
            }

            // ===================================================================================================
            /* compute forces */

            start = System.nanoTime();
            force();
            end = System.nanoTime();
            force_ns += (end - start);
            if (DEBUG) {
                debugPrint(" #after force");
            }

            // ===================================================================================================
            /* update parameters */
            start = System.nanoTime();
            updateParams();
            end = System.nanoTime();
            others_ns += (end - start);
            if (DEBUG) {
                debugPrint(" #params update");
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

                System.err.println("#Iteration " + move + ", interactions " + interactions + ", total energy " + etot);
            }
        }
    }

    protected abstract void tidyup();

    protected abstract void updateParams();

    protected void validate(int size) {
        final double dev = Math.abs(ek - refval[size]);
        if (dev > 1.0e-12) {
            System.err.println("Validation failed");
            System.err.println("Kinetic Energy = " + ek + "  " + refval[size] + " diff:" + dev + "  " + size);
        } else {
            System.err.println("Validation successed");
            System.err.println("Kinetic Energy = " + ek + "  " + refval[size] + " diff:" + dev + "  " + size);
        }
    }

}
