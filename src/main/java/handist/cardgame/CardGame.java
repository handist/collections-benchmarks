package handist.cardgame;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

import apgas.Constructs;
import apgas.Place;
import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.DistBag;
import handist.collections.dist.DistCol;
import handist.collections.dist.TeamedPlaceGroup;
import handist.collections.glb.DistFuture;
import handist.collections.glb.GlobalLoadBalancer;
import handist.collections.launcher.Launcher;

/**
 *
 */
public class CardGame implements Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = -5071847931670661294L;

    /**
     * Main method. Arguments are:
     * <ul>
     * <li>number of players (long)
     * <li>number of rounds (int)
     * <li>computation weight of players (int)
     * <li>computation weight of processing the cards played by the player (int)
     * <li>seed (long - optional)
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        // ARGUMENT PARSING
        long nbPlayers = 0;
        int rounds;
        int playerWeight;
        int processWeight;
        int increasedPlayerWeight;
        int nbOfWinners;
        long seed;
        try {
            nbPlayers = Long.parseLong(args[0]);
            rounds = Integer.parseInt(args[1]);
            playerWeight = Integer.parseInt(args[2]);
            processWeight = Integer.parseInt(args[3]);
            increasedPlayerWeight = Integer.parseInt(args[4]);
            nbOfWinners = Integer.parseInt(args[5]);
            if (args.length > 6) {
                seed = Long.parseLong(args[6]);
            } else {
                seed = System.nanoTime();
            }
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            printUsage();
            return;
        }

        // INITIALIZATION
        // Some variables need to be final for proper serialization in lambdas
        final Random rnd = new Random(seed);
        final int PLAYERWEIGHT = playerWeight;
        final int INCREASED_PLAYERWEIGHT = increasedPlayerWeight;
        final int PROCESSWEIGHT = processWeight;
        final int WINNER_COUNT = nbOfWinners;
        final int ROUNDS = rounds;
        final Place HERE = Constructs.here();
        final TeamedPlaceGroup WORLD = TeamedPlaceGroup.getWorld();

        // Initialization of players
        final DistCol<Player> players = new DistCol<>();
        final int chunkCount = players.placeGroup().size();
        final int playersPerChunk = (int) (nbPlayers / chunkCount);

        for (int cid = 0; cid < chunkCount; cid++) {
            final long startIndex = cid * playersPerChunk;
            final long endIndex = startIndex + playersPerChunk;
            final Chunk<Player> c = new Chunk<>(new LongRange(startIndex, endIndex), l -> {
                return new Player(rnd.nextLong(), PLAYERWEIGHT, INCREASED_PLAYERWEIGHT);
            });
            players.add(c);
        }

        // Uniform distribution of players accross hosts
        // TODO

        // ITERATIONS
        GlobalLoadBalancer.underGLB(() -> {
            long winner = Long.MIN_VALUE; // At first, no winners
            final DistFuture<DistBag<Long>> futureCards = players.GLB.toBag(p -> p.chooseCardToPlay(Long.MIN_VALUE));
            final List<Throwable> errors = futureCards.getErrors();
            for (final Throwable t : errors) {
                t.printStackTrace();
            }
            DistBag<Long> cardsPlayed = futureCards.result();
            long roundEnd = System.nanoTime();

            for (int round = 1; round < ROUNDS; round++) {

                final long WINNER = winner;
                final DistBag<Long> previousRoundCards = cardsPlayed;
                final NSmallestLong reducer = new NSmallestLong(WINNER_COUNT, PROCESSWEIGHT);
                cardsPlayed = finish(() -> {
                    // Two things in parallel: the gathering and reduction of the cards from the
                    // previous round ...
                    async(() -> WORLD.broadcastFlat(() -> {
                        final Place here = here();
                        previousRoundCards.TEAM.gather(HERE);
                        if (here != HERE) {
                            return;
                        }
                        previousRoundCards.parallelReduceList(reducer);
                    }));

                    // ... and the card computation for the next round
                    return players.GLB.toBag(p -> p.chooseCardToPlay(WINNER)).result();
                });
                // cardsPlayed contains the result fo the GLB.toBag(..) operation above

                winner = reducer.nSmallest.get(WINNER_COUNT - 1); // The new winner has been decided
                final long totalWinners = reducer.nSmallest.lastIndexOf(winner);
                final long total1Tree = reducer.nSmallest.lastIndexOf(1l);

                final long stamp = System.nanoTime();

                System.out.println("Round " + round + "; " + (stamp - roundEnd) / 1e6 + "ms");

                roundEnd = stamp;

                // Syserr output for the threshold value
                System.err.println("Round " + round + " " + reducer);
                System.err.println("Round " + round + " winner threshold;  " + winner);
                System.err.println("Round " + round + " nb of winners;     " + totalWinners);
                System.err.println("Round " + round + " nb of tree 1 size; " + total1Tree);
            }
        });

    }

    /**
     * Prints the arguments this program expects
     */
    public static void printUsage() {
        System.err.println("Udage: mpirun -np <N> java -cp <classpath> " + Launcher.class.getCanonicalName() + " "
                + CardGame.class.getCanonicalName() + " ARGUMENTS");
        System.err.println("Arguments are:");
        System.err.println("\t<players>               the number of players participating in the game");
        System.err.println("\t<rounds>                the number of rounds in the game");
        System.err.println("\t<player weight>         base computation weight of the players");
        System.err
                .println("\t<process wight>         base computation weight of processing the cards played by workers");
        System.err.println("\t<player increased load> computation weight of players that have won a round");
        System.err.println(
                "\t<nb of winners>         minimum number of players that win each round and suffer the increased computation weight in the next round");
        System.err.println("\t<seed>                  the random seed used to initialize the workers");
    }
}
