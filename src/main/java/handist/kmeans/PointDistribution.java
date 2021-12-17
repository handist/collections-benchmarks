package handist.kmeans;

import handist.collections.LongRange;
import handist.collections.dist.CollectiveMoveManager;
import handist.collections.dist.DistChunkedList;
import handist.collections.dist.TeamedPlaceGroup;

/**
 * Utility class used to create an initial distribution of points across hosts
 * for the various KMeans programs
 *
 * @author Patrick Finnerty
 *
 */
public class PointDistribution {

    /**
     * Makes an uniform "flat" distribution. It is assumed that all the entries of
     * the collection are contained in the local branch
     *
     * @param distCol the distributed collection on which the uniform distribution
     *                needs to be made
     */
    public static void makeFlatDistribution(DistChunkedList<?> distCol) {
        final TeamedPlaceGroup world = distCol.placeGroup();
        world.broadcastFlat(() -> {
            final CollectiveMoveManager mm = new CollectiveMoveManager(world);
            final int hostCount = world.size();
            int destinationHost = 0;
            for (final LongRange lr : distCol.ranges()) {
                distCol.moveRangeAtSync(lr, world.get((destinationHost++) % hostCount), mm);
            }
            mm.sync();
        });
    }

    /**
     * Makes a triangular distribution. It is assumed that all the entries of the
     * collection are contained in the local branch
     *
     * @param distCol the distributed collection whose distribution needs to be
     *                adjusted
     */
    public static void makeTriangleDistribution(DistChunkedList<?> distCol) {
        final TeamedPlaceGroup world = distCol.placeGroup();
        world.broadcastFlat(() -> {
            final CollectiveMoveManager mm = new CollectiveMoveManager(world);
            final int hostCount = world.size();
            int destinationHost = 0;
            int tBound = hostCount;
            for (final LongRange lr : distCol.ranges()) {
                distCol.moveRangeAtSync(lr, world.places().get(destinationHost++), mm);
                // The following if's are here to make a triangular distribution
                if (destinationHost == tBound) {
                    destinationHost = 0;
                    tBound--;
                    if (tBound == 0) {
                        tBound = hostCount;
                    }
                }
            }
            mm.sync();
        });
    }

    public static void strongScalingTriangleDistribution(DistChunkedList<?> distCol) throws Exception {
        final TeamedPlaceGroup world = distCol.placeGroup();
        final CollectiveMoveManager mm = new CollectiveMoveManager(world);
        final int hostCount = world.size();
        int destinationHost = 0;
        int tBound = hostCount;
        for (final LongRange lr : distCol.ranges()) {
            distCol.moveRangeAtSync(lr, world.places().get(destinationHost++), mm);
            // The following if's are here to make a triangular distribution
            if (destinationHost == tBound) {
                destinationHost = 0;
                tBound--;
                if (tBound == 0) {
                    tBound = hostCount;
                }
            }
        }
        mm.sync();
    }
}
