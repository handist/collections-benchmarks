package handist.ufs;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.Random;

import apgas.Place;
import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.CollectiveMoveManager;
import handist.collections.dist.DistBag;
import handist.collections.dist.DistCol;
import handist.collections.dist.TeamedPlaceGroup;
import handist.collections.glb.Config;
import handist.collections.glb.DistFuture;
import handist.collections.glb.GlobalLoadBalancer;
import handist.collections.glb.lifeline.NoLifeline;
import handist.collections.launcher.Launcher;
import mpi.MPI;

/**
 * Unbalanced Forest Search (UFS).
 *
 * This benchmark for distributed computations consists in computing a large
 * number of sequential {@link UTS} over a certain number of successive rounds.
 * Unbalance is created thorugh the use of an unrelocatable collection's
 * concurrent computation on Place0
 *
 * @author Patrick Finnerty
 *
 */
public class UFS4 implements Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = -5071847931670661294L;

    /**
     * Main method. Arguments are:
     * <ul>
     * <li>number of trees in the forest (long)
     * <li>number of trees in the host0 local forest (long)
     * <li>number of rounds (int)
     * <li>maximum depth of individual trees (int)
     * <li>seed (long - optional)
     * <li>number of trees in each Chunk (long - optional, specific to this
     * implementation)
     *
     * @param args program arguments
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        // ARGUMENT PARSING
        long nbTrees = 0;
        long host0Trees = 0;
        int rounds;
        int treeDepth;
        int host0TreeDepth;
        long seed;
        long treesPerChunk;
        try {
            nbTrees = Long.parseLong(args[0]);
            host0Trees = Long.parseLong(args[1]);
            rounds = Integer.parseInt(args[2]);
            treeDepth = Integer.parseInt(args[3]);
            host0TreeDepth = Integer.parseInt(args[4]);
            if (args.length > 5) {
                seed = Long.parseLong(args[5]);
            } else {
                seed = System.nanoTime();
            }
            if (args.length > 6) {
                treesPerChunk = Long.parseLong(args[6]);
            } else {
                treesPerChunk = nbTrees / TeamedPlaceGroup.getWorld().size();
            }
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            printUsage();
            return;
        }

        Config.printConfiguration(System.err);

        // INITIALIZATION
        // Some variables need to be final for proper serialization in lambdas
        final Random rnd = new Random(seed);
        final int TREE_DEPTH = treeDepth;
        final int HOST0_TREE_DEPTH = host0TreeDepth;
        final int ROUNDS = rounds;
        final TeamedPlaceGroup WORLD = TeamedPlaceGroup.getWorld();

        // Initialization of the forest
        final DistCol<RandomTree> trees = new DistCol<>();

        final int chunkCount = (int) ((int) nbTrees / treesPerChunk);
        for (int cid = 0; cid < chunkCount; cid++) {
            final long startIndex = cid * treesPerChunk;
            final long endIndex = startIndex + treesPerChunk;
            final Chunk<RandomTree> c = new Chunk<>(new LongRange(startIndex, endIndex), l -> {
                return new RandomTree(rnd.nextLong());
            });
            trees.add(c);
        }

        // Uniform distribution of trees across hosts
        WORLD.broadcastFlat(() -> {
            int destination = 0;
            final CollectiveMoveManager mm = new CollectiveMoveManager(WORLD);
            for (final LongRange lr : trees.getAllRanges()) {
                trees.moveRangeAtSync(lr, place(destination % WORLD.size()), mm);
                destination++;
            }
            mm.sync();
        });

        // Initialization of the host0-local
        final DistCol<RandomTree> host0localTrees = new DistCol<>();
        final int localChunkCount = (int) ((int) host0Trees / treesPerChunk);
        for (int cid = 0; cid < localChunkCount; cid++) {
            final long startIndex = cid * treesPerChunk;
            final long endIndex = startIndex + treesPerChunk;
            final Chunk<RandomTree> c = new Chunk<>(new LongRange(startIndex, endIndex), l -> {
                return new RandomTree(rnd.nextLong());
            });
            host0localTrees.add(c);
        }
        host0localTrees.GLB.setLifeline(NoLifeline.class);

        // ITERATIONS
        // Insert a line for the table outputs on Syserr and Sysout
        final StringBuilder sb1 = new StringBuilder("Round ; Elapsed Time (ms); h0local trees; ");
        for (int h = 0; h < WORLD.size(); h++) {
            sb1.append("h" + h + " trees; ");
        }
        sb1.append("h0local nodes; ");
        for (int h = 0; h < WORLD.size(); h++) {
            sb1.append("h" + h + " nodes; ");
        }
        System.out.println(sb1.toString());

        for (int round = 0; round < ROUNDS; round++) {
            final int ROUND = round;

            GlobalLoadBalancer.underGLB(() -> {
                final long startStamp = System.nanoTime();
                // Compute every tree in the forest
                // All the computation is done in the next line
                final int localTreeDepth = rnd.nextInt(HOST0_TREE_DEPTH);
                // By experience, we know seed "19" is a good seed
                final DistFuture<DistBag<Long>> future = host0localTrees.GLB.toBag(t -> t.growTree(19, localTreeDepth));
                final DistBag<Long> cards = trees.GLB.toBag(t -> t.growTree(TREE_DEPTH)).result();
                final long endStamp = System.nanoTime();

                final LoadTracker localLoad = future.result().reduceList(new LoadTracker());

                // Collect information to make relevant outputs
                // Sum the number of trees / nodes explored on each host
                // This will be used to track the evolution of the distribution
                WORLD.broadcastFlat(() -> {
                    final Place here = here();
                    final LoadTracker localTracker = new LoadTracker();
                    cards.parallelReduceList(localTracker);

                    // Prepare an array to gather everything on host 0
                    final Object[] trackers = new Object[WORLD.size()];
                    trackers[here.id] = localTracker;

                    // Quick MPI call to gather everything on place 0
                    WORLD.comm.Gather(trackers, here.id, 1, MPI.OBJECT, trackers, 0, 1, MPI.OBJECT, 0);

                    if (here.id == 0) {
                        // Print the elapsed time, the number of trees, and the number of nodes explored
                        // by each host on System.out
                        final StringBuilder sb = new StringBuilder("Round " + ROUND + "; "
                                + (endStamp - startStamp) / 1e6 + "; " + localLoad.treeCount + "; ");
                        for (final Object o : trackers) {
                            final LoadTracker tracker = (LoadTracker) o;
                            sb.append(tracker.treeCount + "; ");
                        }
                        sb.append(localLoad.nodeCount + "; ");
                        for (final Object o : trackers) {
                            final LoadTracker tracker = (LoadTracker) o;
                            sb.append(tracker.nodeCount + "; ");
                        }
                        System.out.println(sb.toString());
                    }
                });
            });
        }

    }

    /**
     * Prints the arguments this program expects
     */
    public static void printUsage() {
        System.err.println("Udage: mpirun -np <N> java -cp <classpath> " + Launcher.class.getCanonicalName() + " "
                + UFS4.class.getCanonicalName() + " ARGUMENTS");
        System.err.println("Arguments are:");
        System.err.println("\t<forest size>                  the number of trees in this Unbalanced Forest Search");
        System.err.println("\t<host 0 forest size>           the number of trees in immovable host 0 forest");
        System.err.println("\t<rounds>                       the number of rounds of the UFS");
        System.err.println("\t<tree depth>                   maximum depth of each tree in the forest");
        System.err.println("\t<host 0 tree depth>            maximum depth of the trees in the host 0 forest");
        System.err.println("\t<seed>            (optional)   the random seed used to initialize the workers");
        System.err.println(
                "\t<tree per chunk>  (optional)   number of trees per chunk in the underlying collection used to contain the forest");
    }
}
