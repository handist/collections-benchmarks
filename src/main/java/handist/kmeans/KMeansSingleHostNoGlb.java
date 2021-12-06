package handist.kmeans;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;

/**
 * Single host implementation of a K-Means computation.
 *
 *
 * @author Patrick Finnerty
 *
 */
public class KMeansSingleHostNoGlb {

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
        // ARGUMENT PARSING
        if (args.length < 5) {
            printUsage();
            return;
        }

        int dimension, k, repetitions, dataSize, chunkSize, chunkCount;
        long seed;
        try {
            dimension = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            repetitions = Integer.parseInt(args[2]);
            dataSize = Integer.parseInt(args[3]);
            chunkSize = Integer.parseInt(args[4]);
            chunkCount = dataSize / chunkSize;

            if (chunkCount * chunkSize != dataSize) {
                System.err.println("Chunk size not uniform!");
                return;
            }

            if (args.length > 5) {
                seed = Long.parseLong(args[5]);
            } else {
                seed = System.nanoTime();
            }
        } catch (final Exception e) {
            printUsage();
            System.err.println("Arguments received were " + Arrays.toString(args));
            return;
        }

        System.err.println(
                KMeansSingleHostNoGlb.class.getCanonicalName() + "; arguments received were " + Arrays.toString(args));

        // initialize final constants for easier lambda serialization later
        final int K = k;
        final int DIMENSION = dimension;
        final int REPETITIONS = repetitions;

        // INITIALIZATION
        final long initStart = System.nanoTime();
        final Random r = new Random(seed);

        final ChunkedList<Point> points = new ChunkedList<>();
        final List<Double[]> initialPoints = generateData(dataSize, dimension, k);
        final List<Double[]> initialCentroids = randomSample(k, initialPoints, r);

        // Additional step for our implementation, we need to place the points in our
        // ChunkedList collection
        for (int chunkNumber = 0; chunkNumber < chunkCount; chunkNumber++) {
            final LongRange chunkRange = new LongRange(chunkNumber * chunkSize, (chunkNumber + 1) * chunkSize);
            final Chunk<Point> c = new Chunk<>(chunkRange, l -> {
                return new Point(initialPoints.get(l.intValue()));
            });
            points.add(c);
        }

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
        double[][] clusterCentroids = initialClusterCenter;
        for (int iter = 0; iter < REPETITIONS; iter++) {
            final long iterStart = System.nanoTime();
            final double[][] centroids = clusterCentroids;
            // Assign each point to a cluster
            points.parallelForEach(p -> p.assignCluster(centroids));

            final long assignFinished = System.nanoTime();
            // Calculate the average position of each cluster
            final AveragePosition avgClusterPosition = points.parallelReduce(new AveragePosition(K, DIMENSION));

            final long avgFinished = System.nanoTime();
            // Calculate the new centroid of each cluster
            final ClosestPoint closestPoint = points
                    .parallelReduce(new ClosestPoint(K, DIMENSION, avgClusterPosition.clusterCenters));
            clusterCentroids = closestPoint.closestPointCoordinates;

            final long iterEnd = System.nanoTime();
            System.out.println("Iter " + iter + "; " + (iterEnd - iterStart) / 1e6 + "; " + "; "
                    + (assignFinished - iterStart) / 1e6 + "; " + (avgFinished - assignFinished) / 1e6 + "; "
                    + (iterEnd - avgFinished) / 1e6);
        }
    }

    /**
     * Prints usage onto standard error output
     */
    private static void printUsage() {
        System.err.println("Usage: java -cp [...] " + KMeansSingleHostNoGlb.class.getCanonicalName()
                + " <point dimension> <nb of clusters \"k\"> <repetitions> <nb points> <chunk size> [<seed>]");
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