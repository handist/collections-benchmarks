package handist.noglb.fuzzykmeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.CollectiveMoveManager;
import handist.collections.dist.DistChunkedList;
import handist.collections.dist.Reducer;
import handist.collections.dist.TeamedPlaceGroup;
import handist.collections.glb.Config;

public class FuzzyKMeans {

    static class AveragePosition extends Reducer<AveragePosition, Point> {

        /** Serial Version UID */
        private static final long serialVersionUID = 29050701329215796L;

        final double m;

        /** Weighted sum of the point coordinates */
        double[][] numerator;
        /** Sum of weights for each cluster */
        double[] denominator;

        /**
         * Constructor
         *
         * @param k number of clusters considered for computation
         */
        public AveragePosition(int k, int dimension, double fuzziness) {
            numerator = new double[k][dimension];
            denominator = new double[k];
            m = fuzziness;
        }

        @Override
        public void merge(AveragePosition reducer) {
            for (int c = 0; c < numerator.length; c++) {
                denominator[c] += reducer.denominator[c];
                for (int dim = 0; dim < numerator[c].length; dim++) {
                    numerator[c][dim] += reducer.numerator[c][dim];
                }
            }
        }

        /**
         * After all the reduction has been done, computes and returns the new centroid
         * positions
         *
         * @return the new centroid positions
         */
        public double[][] newCentroids() {
            final double[][] centroids = new double[numerator.length][numerator[0].length];
            // For each cluster, divide numerator by weight to obtain the new coordinate
            for (int j = 0; j < numerator.length; j++) {
                final double[] weightedSum = numerator[j]; // j'th cluster
                for (int dim = 0; dim < weightedSum.length; dim++) {
                    centroids[j][dim] = weightedSum[dim] / denominator[j];
                }
            }
            return centroids;
        }

        @Override
        public AveragePosition newReducer() {
            return new AveragePosition(numerator.length, numerator[0].length, m);
        }

        @Override
        public void reduce(Point input) {
            // For each cluster membership
            for (int j = 0; j < input.clusterAssignment.length; j++) {
                final double weight = Math.pow(input.clusterAssignment[j], m);
                // Add the weight to the denominator
                denominator[j] += weight;
                // Add the weighted coordinates to the numerator
                for (int dim = 0; dim < input.position.length; dim++) {
                    numerator[j][dim] += input.position[dim] * weight;
                }

            }
        }
    }

    static class Point implements Serializable {
        /** Generated Serial Version UID */
        private static final long serialVersionUID = 903981107365981546L;

        /** Cluster id this point belongs to */
        double clusterAssignment[];

        /** Coordinates array */
        final double[] position;

        /**
         * Constructor
         *
         * @param initialPosition array of {@link Double} containing the point
         *                        coordinates of this point
         * @param clusterCount    the number of clusters considered in this algorithm
         */
        Point(Double[] initialPosition, int clusterCount) {
            position = new double[initialPosition.length];
            for (int i = 0; i < position.length; i++) {
                position[i] = initialPosition[i];
            }
            clusterAssignment = new double[clusterCount];
        }

        /**
         * Update the value of {@link #clusterAssignment} based on the position of the
         * given centroids
         *
         * @param clusterCentroids the position of the centroid
         * @param m                fuzziness factor (>1.0)
         */
        public void assignCluster(double[][] clusterCentroids, float m) {
            // Compute the membership of the point to each cluster
            for (int j = 0; j < clusterCentroids.length; j++) {
                float D = 0f;
                for (int c = 0; c < clusterCentroids.length; c++) {
                    D += Math.pow((distance(position, clusterCentroids[c])), (2 / (m - 1)));
                }

                final float u = 1 / D;
                clusterAssignment[j] = u;
            }
        }
    }

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

        int dimension, k, repetitions, chunkSize, chunkCount, dataSize;
        long seed;
        float m;
        try {
            dimension = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            m = Float.parseFloat(args[2]);
            repetitions = Integer.parseInt(args[3]);
            chunkSize = Integer.parseInt(args[4]);
            chunkCount = Integer.parseInt(args[5]);
            dataSize = chunkSize * chunkCount;

            if (args.length > 6) {
                seed = Long.parseLong(args[6]);
            } else {
                seed = System.nanoTime();
            }
        } catch (final Exception e) {
            printUsage();
            System.err.println("Arguments received were " + Arrays.toString(args));
            return;
        }

        System.err.println("Arguments received were " + Arrays.toString(args));
        Config.printConfiguration(System.err);

        // initialize final constants for easier lambda serialization later
        final int K = k;
        final int DIMENSION = dimension;
        final int REPETITIONS = repetitions;
        final float M = m;

        System.err.println("Starting Initialization");

        // INITIALIZATION
        final long initStart = System.nanoTime();
        final Random r = new Random(seed);

        final DistChunkedList<Point> points = new DistChunkedList<>();
        final List<Double[]> initialPoints = generateData(dataSize, dimension, k);
        final List<Double[]> initialCentroids = randomSample(k, initialPoints, r);

        // Additional step for our implementation, we need to place the points in our
        // DistCol collection
        for (int chunkNumber = 0; chunkNumber < chunkCount; chunkNumber++) {
            final LongRange chunkRange = new LongRange(chunkNumber * chunkSize, (chunkNumber + 1) * chunkSize);
            final Chunk<Point> c = new Chunk<>(chunkRange, l -> {
                return new Point(initialPoints.get(l.intValue()), K);
            });
            points.add(c);
        }

        System.err.println("Chunked Initialized");

        // Second additional step: we distribute the chunks evenly across hosts
        final TeamedPlaceGroup world = TeamedPlaceGroup.getWorld();
        world.broadcastFlat(() -> {
            final CollectiveMoveManager mm = new CollectiveMoveManager(world);
            final int hostCount = world.size();
            int destinationHost = 0;
            for (final LongRange lr : points.ranges()) {
                points.moveRangeAtSync(lr, world.get((destinationHost++) % hostCount), mm);
            }
            mm.sync();
        });

        System.err.println("Chunks distributed");

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

        // ITERATIONS OF THE FUZZY C-MEANS ALGORITHM
        world.broadcastFlat(() -> {
            double[][] clusterCentroids = initialClusterCenter;
            for (int iter = 0; iter < REPETITIONS; iter++) {
                final long iterStart = System.nanoTime();

                // 1 ------------------------------------------------------------------
                // Calculate the average position of each cluster
                final AveragePosition avgClusterPosition = points.team()
                        .parallelReduce(new AveragePosition(K, DIMENSION, M));
                final long centroidAverageFinished = System.nanoTime();
                clusterCentroids = avgClusterPosition.newCentroids();

                // 2 ------------------------------------------------------------------
                // Compute the fuzzy membership of each point to the clusters
                final double[][] clusters = clusterCentroids; // need a final value variable to use in lambda
                                                              // expression
                points.parallelForEach(p -> p.assignCluster(clusters, M));

                final long iterEnd = System.nanoTime();
                if (world.rank() == 0) {
                    System.out.println("Iter " + iter + "; " + (iterEnd - iterStart) / 1e6 + "; "
                            + (centroidAverageFinished - iterStart) / 1e6 + "; "
                            + (iterEnd - centroidAverageFinished) / 1e6 + "; ");
                }
            }
        });
    }

    /**
     * Prints usage onto standard error output
     */
    private static void printUsage() {
        System.err.println("Usage: java -cp [...] handist.noglb.fuzzykmeans.FuzzyKMeans "
                + "<point dimension> <nb of clusters \"k\"> <fuziness m> <repetitions> <chunk size> <number of chunks> [<seed>]");
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