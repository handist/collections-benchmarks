package handist.kmeans;

import static apgas.Constructs.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import apgas.Place;
import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.DistChunkedList;
import handist.collections.dist.TeamedPlaceGroup;

/**
 * Teamed implementation of a distributed K-Means computation.
 *
 * @author Patrick Finnerty
 *
 */
public class KMeansNoGlbStrongScaling implements Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = -216740021880516992L;

    /**
     * Constant defining a property of this program. If this property is set, the
     * detailed computation time of each host will be printed to the file specified
     * as parameter.
     */
    public static final String DETAILED_PER_HOST_OUTPUT = "kmeans.detailed_output";

    /**
     * Array into which local information about computation time is recorded.
     */
    static transient ArrayList<Long> localStamps = new ArrayList<>();

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
            Arguments.printHelp(KMeansNoGlbStrongScaling.class.getCanonicalName());
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

        System.err.println("Main class " + KMeansNoGlbStrongScaling.class.getCanonicalName());
        System.err.println("Arguments received were " + Arrays.toString(args));

        // INITIALIZATION
        final long initStart = System.nanoTime();
        final TeamedPlaceGroup world = TeamedPlaceGroup.getWorld();
        final Random r = new Random(seed);
        final DistChunkedList<Point> points = new DistChunkedList<>();
        final List<Double[]> initialPoints = generateData(dataSize, dimension, k);
        final List<Double[]> initialCentroids = randomSample(k, initialPoints, r);

        // Additional step for our implementation, we need to place the points in our
        // DistCol collection
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
        });

        // We convert the list of initial centroids to a 2Darray of double
        final double[][] initialClusterCenter = new double[k][dimension];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < dimension; j++) {
                initialClusterCenter[i][j] = initialCentroids.get(i)[j];
            }
        }

        final long initEnd = System.nanoTime();

        System.err.println("Init; " + (initEnd - initStart) / 1e6 + " ms");

        // ITERATIONS OF THE K-MEANS ALGORITHM
        world.broadcastFlat(() -> {
            double[][] clusterCentroids = initialClusterCenter;
            for (int iter = 0; iter < repetitions; iter++) {
                final long iterStart = System.nanoTime();
                localStamps.add(iterStart);
                final double[][] centroids = clusterCentroids;
                // Assign each point to a cluster
                points.parallelForEach(p -> p.assignCluster(centroids));

                final long assignFinished = System.nanoTime();
                // Calculate the average position of each cluster
//                final AveragePosition avgClusterPosition = points.team()
//                        .parallelReduce(new AveragePosition(K, DIMENSION));
                // The above is explicitly split into two to enable us to distinguish the
                // computation time and the communication/merging time
                final AveragePosition localAvg = points.parallelReduce(new AveragePosition(k, dimension));
                localStamps.add(System.nanoTime());
                final AveragePosition avgClusterPosition = localAvg.teamReduction(world);

                final long avgFinished = System.nanoTime();
                localStamps.add(System.nanoTime());
                // Calculate the new centroid of each cluster
//                final ClosestPoint closestPoint = points.team()
//                        .parallelReduce(new ClosestPoint(K, DIMENSION, avgClusterPosition.clusterCenters));
                final ClosestPoint localClosestPoint = points
                        .parallelReduce(new ClosestPoint(k, dimension, avgClusterPosition.clusterCenters));
                localStamps.add(System.nanoTime());
                final ClosestPoint closestPoint = localClosestPoint.teamReduction(world);
                clusterCentroids = closestPoint.closestPointCoordinates;

                final long iterEnd = System.nanoTime();
                if (world.rank() == 0) {
                    System.out.println("Iter " + iter + "; " + (iterEnd - iterStart) / 1e6 + "; "
                            + (assignFinished - iterStart) / 1e6 + "; " + (avgFinished - assignFinished) / 1e6 + "; "
                            + (iterEnd - avgFinished) / 1e6);
                }
            }
        });
        final long totalComputationTime = System.nanoTime() - initEnd;
        System.err.println("ComputationTime(s); " + totalComputationTime / 1e9);

        // The program has completed.
        // If required, we dump some more complete output on a specified file
        if (System.getProperties().containsKey(DETAILED_PER_HOST_OUTPUT)) {
            final String detailedOutputFileName = System.getProperty(DETAILED_PER_HOST_OUTPUT);
            final File outputFile = new File(detailedOutputFileName);
            PrintStream out;
            try {
                out = new PrintStream(outputFile);
            } catch (final FileNotFoundException e) {
                System.err.println("Issue in creating file for detailed output");
                e.printStackTrace();
                System.err.println("Aborting ...");
                return;
            }

            // First, collect all the remote stamps
            final List<ArrayList<Long>> hostStamps = new ArrayList<>(world.size());
            for (final Place p : world.places()) {
                hostStamps.add(at(p, () -> {
                    return localStamps;
                }));
            }

            @SuppressWarnings("unchecked")
            final Iterator<Long>[] stampIterator = new Iterator[world.size()];
            for (int p = 0; p < world.size(); p++) {
                stampIterator[p] = hostStamps.get(p).iterator();
            }
            // Second parse the whole lot and print line by line the computation time.
            for (int iter = 0; iter < repetitions; iter++) {
                // For each iteration, there are 3 operations being measured:
                // 1. assignment of cluster based on closest centroid
                // 2. compute new cluster average
                // 3. find closest point to average to be next centroid
                //
                // As 1. and 2. are measured together, this represents 4 stamps per iteration on
                // each host
                final long[] startStamp = new long[world.size()];
                final long[] endStamp = new long[world.size()];
                for (int p = 0; p < world.size(); p++) {
                    startStamp[p] = stampIterator[p].next();
                    endStamp[p] = stampIterator[p].next();
                }
                printStampDiff(out, "assign+avg", startStamp, endStamp);
                for (int p = 0; p < world.size(); p++) {
                    startStamp[p] = stampIterator[p].next();
                    endStamp[p] = stampIterator[p].next();
                }
                printStampDiff(out, "closestpnt", startStamp, endStamp);
            }
            out.close();
        }
    }

    /**
     * Subroutine used to print on one line the elapsed time between the stamps
     * contained in the arrays passed as parameters. Both arrays should be of the
     * same size so that pairs can be computed without reaching an
     * {@link ArrayIndexOutOfBoundsException}
     *
     * @param out        outputStream into which the information should be printed
     * @param msg        the string to print in the first column
     * @param startStamp start stamps
     * @param endStamp   end stamps
     */
    private static void printStampDiff(PrintStream out, String msg, long[] startStamp, long[] endStamp) {
        out.print(msg);
        for (int i = 0; i < startStamp.length; i++) {
            out.print(";" + (endStamp[i] - startStamp[i]) / 1e6);
        }
        out.println();
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