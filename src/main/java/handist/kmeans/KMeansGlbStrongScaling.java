package handist.kmeans;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.DistChunkedList;
import handist.collections.dist.DistLog;
import handist.collections.dist.TeamedPlaceGroup;
import handist.collections.glb.GlobalLoadBalancer;
import handist.collections.util.SavedLog;

public class KMeansGlbStrongScaling {

    private static final String GLB_LOG_FILE = "kmeans.logfile";
    private static final String RESET_PERIOD = "kmeans.reset";

    private static final int resetPeriod = Integer.parseInt(System.getProperty(RESET_PERIOD, "-1"));

    /**
     * Euclidean distance calculation between n-dimensional coordinates. The square
     * root is not applied to the sum of the squares as it preserves the order.
     *
     * @param a first point
     * @param b second point
     * @return the distance between the two points.
     */
    static double distance(double[] a, double[] b) {
        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            final double diff = a[i] - b[i];
            result += diff * diff;
        }
        return result;
    }

    /**
     * Method taken from the Renaissance JavaKMeans benchmark
     *
     * @param count        number of data points desired
     * @param dimension    number of dimensions of data points
     * @param clusterCount "K" in K-Means
     * @return a list of Double arrays, each array representing a data point
     */
    static List<Double[]> generateData(final int count, final int dimension, final int clusterCount) {
        // Create random generators for individual dimensions.
        final Random[] randoms = IntStream.range(0, dimension).mapToObj(d -> new Random(1 + 2 * d))
                .toArray(Random[]::new);

        // Generate random data for all dimensions.
        return IntStream.range(0, count).mapToObj(i -> {
            return IntStream.range(0, dimension).mapToObj(
                    d -> (((i + (1 + 2 * d)) % clusterCount) * 1.0 / clusterCount) + randoms[d].nextDouble() * 0.5)
                    .toArray(Double[]::new);
        }).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        Arguments arguments = null;
        try {
            arguments = new Arguments(args);
        } catch (final Exception e) {
            Arguments.printHelp(KMeansNoGlb.class.getCanonicalName());
            System.err.println("Arguments received were " + Arrays.toString(args));
            System.exit(1);
        }

        // initialize final constants for easier lambda serialization later
        final int k = arguments.k;
        final int dimension = arguments.dim;
        final int repetitions = arguments.iterations;
        final int chunkSize = arguments.ptsPerChunk;
        final int chunkCount = arguments.nbChunks;
        final int dataSize = chunkSize * chunkCount;
        final long seed = arguments.seed;
        final String distribution = arguments.distribution;

        System.err.println("Main class " + KMeansGlbStrongScaling.class.getCanonicalName());
        System.err.println("Arguments received were " + Arrays.toString(args));

        // INITIALIZATION
        final long initStart = System.nanoTime();
        final Random r = new Random(seed);

        final DistChunkedList<Point> points = new DistChunkedList<>();

        final List<Double[]> initialPoints = generateData(dataSize, dimension, k);
        final List<Double[]> initialCentroids = randomSample(k, initialPoints, r);

        final TeamedPlaceGroup world = TeamedPlaceGroup.getWorld();
        world.broadcastFlat(() -> {
            final long offset = world.rank() * chunkCount * chunkSize;
            final List<Double[]> pts = generateData(dataSize, dimension, k);
            for (int chunkNumber = 0; chunkNumber < chunkCount; chunkNumber++) {
                final LongRange chunkRange = new LongRange(chunkNumber * chunkSize + offset,
                        (chunkNumber + 1) * chunkSize + offset);
                final Chunk<Point> c = new Chunk<>(chunkRange, l -> {
                    return new Point(pts.get((int) (l - offset)));
                });
                points.add(c);
            }
            // Second additional step: we distribute the chunks evenly across hosts
            switch (distribution) {
            case "triangle":
                PointDistribution.strongScalingTriangleDistribution(points);
                break;
            case "flat":
            default:
                // Is already a flat distribution
            }
            for (final LongRange lr : points.ranges()) {
                System.err.println(world.rank() + " " + lr);
            }
        });
        // Third additional step, we convert the list of initial centroids to a 2Darray
        // of double
        final double[][] initialClusterCenter = new double[k][dimension];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < dimension; j++) {
                initialClusterCenter[i][j] = initialCentroids.get(i)[j];
            }
        }

        final long initEnd = System.nanoTime();

        System.out.println("Init; " + (initEnd - initStart) / 1e6 + " ms");

        // ITERATIONS OF THE K-MEANS ALGORITHM
        GlobalLoadBalancer.underGLB(() -> {
            double[][] clusterCentroids = initialClusterCenter;
            int resetCounter = 0;
            for (int iter = 0; iter < repetitions; iter++) {
                resetCounter++;
                final long iterStart = System.nanoTime();
                final double[][] centroids = clusterCentroids;
                // Assign each point to a cluster
                points.GLB.forEach(p -> p.assignCluster(centroids)).waitGlobalTermination();

                final long assignFinished = System.nanoTime();
                // Calculate the average position of each cluster
                final AveragePosition avgClusterPosition = points.GLB.reduce(new AveragePosition(k, dimension))
                        .result();

                final long avgFinished = System.nanoTime();
                // Calculate the new centroid of each cluster
                final ClosestPoint closestPoint = points.GLB
                        .reduce(new ClosestPoint(k, dimension, avgClusterPosition.clusterCenters)).result();
                clusterCentroids = closestPoint.closestPointCoordinates;

                final long iterEnd = System.nanoTime();
                System.out.println(
                        "Iter " + iter + "; " + (iterEnd - iterStart) / 1e6 + "; " + (assignFinished - iterStart) / 1e6
                                + "; " + (avgFinished - assignFinished) / 1e6 + "; " + (iterEnd - avgFinished) / 1e6);

                if (resetCounter == resetPeriod) {
                    GlobalLoadBalancer.reset(); // EXPERIMENTAL FEATURE, FOR EXPERIMENT PURPOSES
                    resetCounter = 0;
                }
            }
        });

        if (System.getProperties().contains(GLB_LOG_FILE)) {
            final String fileName = System.getProperty(GLB_LOG_FILE);
            final DistLog log = GlobalLoadBalancer.getPreviousLog();
            final SavedLog sLog = new SavedLog(log);
            try {
                sLog.saveToFile(new File(fileName));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Selects some random points to be the initial centroids
     *
     * @param sampleCount number of centroids desired
     * @param data        the data from which to sample the initial centroids
     * @param random      random generator
     * @return a list of initial centroids
     */
    static List<Double[]> randomSample(final int sampleCount, final List<Double[]> data, final Random random) {
        return random.ints(sampleCount, 0, data.size()).mapToObj(data::get).collect(Collectors.toList());
    }

    static double weightedAverage(double a, long includedPoints, double b, long includedPoints2) {
        return ((a * includedPoints) + (b * includedPoints2)) / (includedPoints + includedPoints2);
    }
}
