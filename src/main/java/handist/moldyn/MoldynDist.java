package handist.moldyn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

import apgas.MultipleException;
import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.RangedList;
import handist.collections.dist.CachableChunkedList;
import handist.collections.dist.Reducer;
import handist.collections.dist.TeamedPlaceGroup;
import mpi.MPI;
import mpi.MPIException;

public class MoldynDist implements Serializable {
    public static int datasizes[] = {8,13};
    public static double refval[] = {1731.4306625334357,7397.392307839352};

    private static final long serialVersionUID = 1364008814489556197L;

    final double den  = 0.83134;    // density
    final double tref = 0.722;          
    final double h    = 0.064;
    
    public List<LongRange> ranges;
    public LongRange allRange;
    public TeamedPlaceGroup placeGroup;
    public RangedList<Particle> one;
    public CachableChunkedList<Particle> oneX;

    public static double epot  = 0.0;   // potential energy
    public static double ekin  = 0.0;   // kinematic energy
    public static double vir   = 0.0;   // virial
    public static double count = 0.0;   // ???
    public static int interactions = 0; // count of interactions between particles
    
    int mdsize;                         // number of particles

    int ijk, i, j, k, lg, move;         // iteration variables

    double rcoff, rcoffs, side, sideh, hsq, hsq2;
    double r, tscale, sc, ek;
    double vaver, vaverh;
    double etot, temp, pres, rp;    // results(total energy/temperature/pressure/???)

    int irep    = 10;
    int istop   = 19;
    int iprint  = 10;
    int movemx  = 50;
    int warmup  = 5;

    Random randnum;
    
    private static final boolean DEBUG = false;
    
    private static void printUsage() {
        System.err.println("Usage: java -cp [...] handist.moldyn.MoldynDist "
                + "<data size index(0or1)> <number of workers> <number of divide> <result file name>");
    }
    
    public static void main(String[] args) throws IOException {
        if(args.length != 2) {
            printUsage();
            return;
        }
        final int problemSize = Integer.parseInt(args[0]);
        final String fileName = args[1];
        
        try {
            MoldynDist m0 = new MoldynDist();
            System.out.println("start warmup for " + m0.warmup + " times");
            for(int i = 0; i < m0.warmup; i++) {
                System.out.println("##################################################");
                System.out.println("warmup " + (i+1) + "/" + m0.warmup);
                m0.initialise(datasizes[0]);
                m0.runiters(true);
                //m.tidyup();
            }
            System.out.println("##################################################");
            System.out.println("main run");
            MoldynDist m = new MoldynDist();
            m.initialise(datasizes[problemSize]);
            long start = System.nanoTime();
            m.runiters(false);
            long end = System.nanoTime();
            m.validate(problemSize);
            System.out.println("############## handist 2CProdDist time: "+ (end-start)/1.0e9);
            m.tidyup();
            
            File file = new File(fileName);
            file.createNewFile();
            FileWriter fw = new FileWriter(file, true);
            fw.write((end-start)/1.0e9 + "\n");
            fw.close();
        } catch (MultipleException me) {
            me.printStackTrace();
        }
    }

    private void validate(int size) {
        double dev = Math.abs(ek - refval[size]);
        if (dev > 1.0e-12 ){
            System.out.println("Validation failed");
            System.out.println("Kinetic Energy = " + ek + "  " + refval[size] + " diff:" + dev + "  " + size);
        }
    }
    
    private void tidyup() {
        one = null;
        System.gc();
    }
    
    static class Sp implements Serializable {
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;
        Sp(double x, double y, double z) { this.x = x; this.y=y; this.z = z;}
        Sp() {}
    }

    public void initialise(final int mm) {
        /* Parameter determination */
        mdsize  = mm * mm * mm * 4;

        side    = Math.pow((mdsize / den), 0.3333333);
        sideh   = side * 0.5;
        rcoff   = mm / 4.0;
        rcoffs  = rcoff * rcoff;

        hsq     = h * h;
        hsq2    = hsq * 0.5;
        tscale  = 16.0 / (1.0 * mdsize - 1.0);
        vaver   = 1.13 * Math.sqrt(tref / 24.0);
        vaverh  = vaver * h;

        /* Particle Generation */
        placeGroup = TeamedPlaceGroup.getWorld();
        allRange    = new LongRange(0, mdsize);
        ranges      = allRange.split(placeGroup.size() * 2);
        oneX        = new CachableChunkedList<>(placeGroup);
        one = new Chunk<>(allRange);
        oneX.add(one);

        double a = side / mm;
        ijk = 0;
        for (lg = 0; lg <= 1; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        one.set(ijk, new Particle(
                                (i * a + lg * a * 0.5), (j * a + lg * a * 0.5), (k * a), 
                                0.0, 0.0,0.0, 
                                0.0, 0.0, 0.0));
                        ijk++;
                    }
                }
            }
        }
        for (lg = 1; lg <= 2; lg++) {
            for (i = 0; i < mm; i++) {
                for (j = 0; j < mm; j++) {
                    for (k = 0; k < mm; k++) {
                        one.set(ijk, new Particle(
                                (i * a + (2 - lg) * a * 0.5), (j * a + (lg - 1) * a * 0.5), (k * a + a * 0.5), 
                                0.0, 0.0, 0.0, 
                                0.0, 0.0, 0.0));
                        ijk++;
                    }
                }
            }
        }

        /* Initialise velocities */

        int iseed = 0;
        double v1 = 0.0;
        double v2 = 0.0;

        randnum = new Random(iseed, v1, v2);

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one.get(i).xvelocity     = r * randnum.v1;
            one.get(i + 1).xvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one.get(i).yvelocity     = r * randnum.v1;
            one.get(i + 1).yvelocity = r * randnum.v2;
        }

        for (i = 0; i < mdsize; i += 2) {
            r = randnum.next();
            one.get(i).zvelocity     = r * randnum.v1;
            one.get(i + 1).zvelocity = r * randnum.v2;
        }

        /* velocity scaling */
        Sp sp = new Sp();
        ekin = 0.0;
        
        for(Particle p: one) {//.forEach((p) -> {
            sp.x += p.xvelocity;
            sp.y += p.yvelocity;
            sp.z += p.zvelocity;
        }//);
        
        sp.x = sp.x / mdsize;
        sp.y = sp.y / mdsize;
        sp.z = sp.z / mdsize;
        
        for(Particle p: one) {//one.forEach((p) -> {
            p.xvelocity -= sp.x;
            p.yvelocity -= sp.y;
            p.zvelocity -= sp.z;
            ekin += p.xvelocity * p.xvelocity;
            ekin += p.yvelocity * p.yvelocity;
            ekin += p.zvelocity * p.zvelocity;
        }//);

        sc = h * Math.sqrt(tref / (tscale * ekin));
        
        for(Particle p: one) {//one.forEach((p) -> {
            p.xvelocity *= sc;
            p.yvelocity *= sc;
            p.zvelocity *= sc;
        }//);

        /* share chunks */
        final CachableChunkedList<Particle> oneX0 = oneX;
        placeGroup.broadcastFlat(()->{ // not transport this (runner) here
            if(placeGroup.rank()==0) {
                oneX0.share(allRange);
            } else {
                oneX0.share(new LongRange(0,0));
            }
        });

        if(placeGroup.rank() == 0 && DEBUG) {
            System.out.println("#Initialized");
            System.out.println(" " + one.get(0));
        }
    }

    static class MyReducer extends Reducer<MyReducer, Particle> {
        double val;
        final Function<Particle,Double> func;
        MyReducer(Function<Particle,Double> func) {
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

    // main routine
    public void runiters(boolean isWarmup) throws MPIException {
        placeGroup.broadcastFlat(()->{
            runiters0(isWarmup);
        });
    }

    public void runiters0(boolean isWarmup) throws MPIException {
        oneX.forEachChunk((c)->{
            one = c;
        });
            move = 0;
            for (move = 0; move < movemx; move++) {
                // ===================================================================================================
                /* move the particles and update velocities, no use global variables */
                for(Particle p : oneX) {
                    p.domove(side);
                }
                
                if(placeGroup.rank() == 0 && DEBUG) {
                    System.out.println(" #after domove");
                    System.out.println(" " + one.get(0));
                    System.out.println(" ekin:" + ekin + "/epot:" + epot);
                }

                // ===================================================================================================
                /* compute forces */
                epot = 0.0;
                vir = 0.0;

                placeGroup.barrier();

                int prevInteractions = interactions;
                interactions = 0;
                // split but sequential exec
                oneX.subList(ranges.get(placeGroup.rank())).forEach((i1, p1) -> {
                    p1.force(oneX, side, rcoff, mdsize, (int)i1);
                });
                oneX.subList(ranges.get(ranges.size() - placeGroup.rank() - 1)).forEach((i1, p1) -> {
                    p1.force(oneX, side, rcoff, mdsize, (int)i1);
                });
             
                epot = placeGroup.allReduce1(epot, MPI.SUM);
                vir = placeGroup.allReduce1(vir, MPI.SUM);
                interactions = placeGroup.allReduce1(interactions, MPI.SUM);
                interactions += prevInteractions;

                placeGroup.barrier();

                if(placeGroup.rank()==0 && DEBUG) {
                    System.out.println(" #after force");
                    System.out.println(" " + one.get(0));
                    System.out.println(" ekin:" + ekin + "/epot:" + epot);
                }
                
                placeGroup.barrier(); // TODO: Not need? written in original code.
                // ===================================================================================================
                /* scale forces, update velocities */
                MyReducer ekinReduce = new MyReducer((p)->p.mkekin(hsq2));
                oneX.parallelReduce(ekinReduce);
                ekin = ekinReduce.val / hsq;
                
                if(placeGroup.rank() == 0 && DEBUG) {
                    System.out.println(" #after mkekin");
                    System.out.println(" " + one.get(0));
                    System.out.println(" ekin:" + ekin + "/epot:" + epot);
                }

                // ===================================================================================================
                /* average velocity */
                double vel = 0.0;
                count = 0.0;

                MyReducer veravgReduce = new MyReducer((p)->p.velavg(vaverh, h));

                oneX.reduce(veravgReduce);
                vel = veravgReduce.val / h;
                
                if(placeGroup.rank() == 0 && DEBUG) {
                    System.out.println(" #after velavg");
                    System.out.println(" " + one.get(0));
                    System.out.println(" ekin:" + ekin + "/epot:" + epot);
                }

                // ===================================================================================================
                /* tmeperature scale if required */
                if ((move < istop) && (((move + 1) % irep) == 0)) {
                    sc = Math.sqrt(tref / (tscale * ekin));
                    oneX.forEach((p) -> {
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
                    if(placeGroup.rank() == 0) {
                        System.out.println("#Iteration " + move);
                        System.out.println(" interactions : " + interactions);
                        System.out.println(" total energy : " + etot);
                        // System.out.println(" average vel : " + vel);
                    }
                }
            }
    }
    

    public final static class Particle implements Serializable {

        private static final long serialVersionUID = 4019649624174733374L;
        public double xcoord, ycoord, zcoord;
        public double xvelocity, yvelocity, zvelocity;
        public double xforce, yforce, zforce;
        
        public Particle(double xcoord, double ycoord, double zcoord,
                double xvelocity, double yvelocity, double zvelocity, double xforce, double yforce, double zforce) {
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
        
        public void force(ChunkedList<Particle> oneX, double side, double rcoff, int mdsize, int x) {

            double sideh;
            double rcoffs;

            double xx, yy, zz, xi, yi, zi, fxi, fyi, fzi;
            double rd, rrd, rrd2, rrd3, rrd4, rrd6, rrd7, r148;
            double forcex, forcey, forcez;

            int i;

            sideh = 0.5 * side;
            rcoffs = rcoff * rcoff;

            xi = xcoord;
            yi = ycoord;
            zi = zcoord;
            fxi = 0.0;
            fyi = 0.0;
            fzi = 0.0;

            for (Particle p2 : oneX.subList(new LongRange(x+1, mdsize))) {
                xx = xi - p2.xcoord;
                yy = yi - p2.ycoord;
                zz = zi - p2.zcoord;

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
                    
                    MoldynDist.epot = MoldynDist.epot + (rrd6 - rrd3);
                    r148 = rrd7 - 0.5 * rrd4;
                    
                    MoldynDist.vir = MoldynDist.vir - rd * r148;
                    
                    forcex = xx * r148;
                    fxi = fxi + forcex;
                    p2.xforce = p2.xforce - forcex;
                    
                    forcey = yy * r148;
                    fyi = fyi + forcey;
                    p2.yforce = p2.yforce - forcey;
                    
                    forcez = zz * r148;
                    fzi = fzi + forcez;
                    p2.zforce = p2.zforce - forcez;
                    
                    MoldynDist.interactions++;
                }

            }

            xforce = xforce + fxi;
            yforce = yforce + fyi;
            zforce = zforce + fzi;

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

        public double velavg(double vaverh, double h) {

            double velt;
            double sq;

            sq = Math.sqrt(xvelocity * xvelocity + yvelocity * yvelocity + zvelocity * zvelocity);

            if (sq > vaverh) {
                MoldynDist.count = MoldynDist.count + 1.0;
            }

            velt = sq;
            return velt;
        }

        public void dscal(double sc) {

            xvelocity = xvelocity * sc;
            yvelocity = yvelocity * sc;
            zvelocity = zvelocity * sc;

        }

        @Override
        public String toString() {
            return String.format("coord(%5.3f, %5.3f, %5.3f) \t velocity(%3.3f, %3.3f, %3.3f) \t force(%2.3f, %2.3f, %2.3f)",
                        xcoord * 1000, ycoord * 1000, zcoord* 1000, xvelocity * 10000, yvelocity * 10000, zvelocity * 10000, xforce * 10000, yforce * 10000, zforce * 10000);
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

        public double update() {

            double rand;
            double scale = 4.656612875e-10;

            int is1, is2, iss2;
            int imult = 16807;
            int imod = 2147483647;

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
    }
}
