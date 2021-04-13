package handist.kmeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.CollectiveMoveManager;
import handist.collections.dist.DistCol;
import handist.collections.dist.Reducer;
import handist.collections.dist.TeamedPlaceGroup;
import handist.collections.glb.Config;
import handist.collections.glb.GlobalLoadBalancer;

public class KMeans {

    static class AveragePosition extends Reducer<AveragePosition, Point> {

        /** Serial Version UID */
        private static final long serialVersionUID = 29050701329215796L;

        /**
         * First index: cluster id Second index: position in the 'nth' dimension of the
         * point
         */
        final double[][] clusterCenters;

        /**
         * Number of points contained in the average position contained in
         * {@link #clusterCenters}
         */
        final long[] includedPoints;

        /**
         * Constructor
         * 
         * @param k number of clusters considered for computation
         */
        public AveragePosition(int k, int dimension) {
            clusterCenters = new double[k][dimension];
            includedPoints = new long[k];
        }

        @Override
        public void merge(AveragePosition reducer) {
            // Compute the average of this and reducer

            // For each 'k' centroid
            for (int k = 0; k < clusterCenters.length; k++) {
                // For each dimension 'd' of the centroid
                for (int d = 0; d < clusterCenters[k].length; d++) {
                    clusterCenters[k][d] = weightedAverage(clusterCenters[k][d], includedPoints[k],
                            reducer.clusterCenters[k][d], reducer.includedPoints[k]);
                }
                includedPoints[k] += reducer.includedPoints[k];
            }
        }

        @Override
        public AveragePosition newReducer() {
            return new AveragePosition(clusterCenters.length, clusterCenters[0].length);
        }

        @Override
        public void reduce(Point input) {
            final int k = input.clusterAssignment;
            for (int d = 0; d < clusterCenters[k].length; d++) {
                clusterCenters[k][d] = weightedAverage(clusterCenters[k][d], includedPoints[k], input.position[d], 1l);
            }
            includedPoints[k]++;
        }
    }

    static final class ClosestPoint extends Reducer<ClosestPoint, Point> {

        /** Serial Version UID */
        private static final long serialVersionUID = -5053187857859985586L;

        final double[][] closestPointCoordinates;
        final double[][] clusterAverage;
        final double[] distanceToAverage;

        /**
         * Constructor
         * 
         * @param k         number of clusters considered for computation
         * @param dimension dimension of the points used
         */
        public ClosestPoint(int k, int dimension, double[][] clusterAveragePositions) {
            closestPointCoordinates = new double[k][dimension];
            distanceToAverage = new double[k];
            for (int l = 0; l < k; l++) {
                distanceToAverage[l] = Double.MAX_VALUE;
            }
            this.clusterAverage = clusterAveragePositions;
        }

        @Override
        public void merge(ClosestPoint reducer) {
            // For each cluster ...
            for (int k = 0; k < distanceToAverage.length; k++) {
                // Check if "reducer" found a closer point than this instance
                if (reducer.distanceToAverage[k] < distanceToAverage[k]) {
                    // The reducer has a point closer than the one held by this instance
                    closestPointCoordinates[k] = reducer.closestPointCoordinates[k];
                    distanceToAverage[k] = reducer.distanceToAverage[k];
                }
            }
        }

        @Override
        public ClosestPoint newReducer() {
            return new ClosestPoint(distanceToAverage.length, clusterAverage[0].length, clusterAverage);
        }

        @Override
        public void reduce(Point input) {
            double distance = distance(input.position, clusterAverage[input.clusterAssignment]);
            if (distance < distanceToAverage[input.clusterAssignment]) {
                distanceToAverage[input.clusterAssignment] = distance;
                for (int n = 0; n < input.position.length; n++) {
                    closestPointCoordinates[input.clusterAssignment][n] = input.position[n];
                }
            }
        }

    }

    static class Point implements Serializable {
        /** Generated Serial Version UID */
        private static final long serialVersionUID = 903981107365981546L;

        /** Cluster id this point belongs to */
        int clusterAssignment;

        /** Coordinates array */
        final double[] position;

        /**
         * Constructor
         * 
         * @param initialPosition array of {@link Double} containing the point
         *                        coordinates of this point
         */
        Point(Double[] initialPosition) {
            position = new double[initialPosition.length];
            for (int i = 0; i < position.length; i++) {
                position[i] = initialPosition[i];
            }
        }

        public void assignCluster(double[][] clusterCentroids) {
            double closestDistance = Double.MAX_VALUE;
            for (int i = 0; i < clusterCentroids.length; i++) {
                double distance = distance(position, clusterCentroids[i]);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    clusterAssignment = i;
                }
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
        try {
            dimension = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            repetitions = Integer.parseInt(args[2]);
            chunkSize = Integer.parseInt(args[3]);
            chunkCount = Integer.parseInt(args[4]);
            dataSize = chunkSize * chunkCount;

            if (args.length > 5) {
                seed = Long.parseLong(args[5]);
            } else {
                seed = System.nanoTime();
            }
        } catch (Exception e) {
            printUsage();
            System.err.println("Arguments received were " + Arrays.toString(args));
            return;
        }
        
        Config.printConfiguration(System.err);
        
        // initialize final constants for easier lambda serialization later
        final int K = k;
        final int DIMENSION = dimension;
        final int REPETITIONS = repetitions;

        // INITIALIZATION
        long initStart = System.nanoTime();
        Random r = new Random(seed);

        DistCol<Point> points = new DistCol<>();
        List<Double[]> initialPoints = generateData(dataSize, dimension, k);
        List<Double[]> initialCentroids = randomSample(k, initialPoints, r);

        // Additional step for our implementation, we need to place the points in our
        // DistCol collection
        for (int chunkNumber = 0; chunkNumber < chunkCount; chunkNumber++) {
            LongRange chunkRange = new LongRange(chunkNumber * chunkSize, (chunkNumber + 1) * chunkSize);
            Chunk<Point> c = new Chunk<>(chunkRange, l -> {
                return new Point(initialPoints.get(l.intValue()));
            });
            points.add(c);
        }

        // Second additional step: we distribute the chunks evenly across hosts
        final TeamedPlaceGroup world = TeamedPlaceGroup.getWorld();
        world.broadcastFlat(() -> {
            CollectiveMoveManager mm = new CollectiveMoveManager(world);
            final int hostCount = world.size();
            int destinationHost = 0;
            for (LongRange lr : points.ranges()) {
                points.moveRangeAtSync(lr, world.get((destinationHost++) % hostCount), mm);
            }
            mm.sync();
        });

        // Third additional step, we convert the list of initial centroids to a 2Darray
        // of double
        double[][] initialClusterCenter = new double[k][dimension];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < dimension; j++) {
                initialClusterCenter[i][j] = initialCentroids.get(i)[j];
            }
        }
        
        long initEnd = System.nanoTime();
        
        System.out.println("Init; " + (initEnd - initStart)/ 1e6 + " ms");

        // ITERATIONS OF THE K-MEANS ALGORITHM
        GlobalLoadBalancer.underGLB(() -> {
            double[][] clusterCentroids = initialClusterCenter;
            for (int iter = 0; iter < REPETITIONS; iter++) {
                long iterStart = System.nanoTime();
                final double[][] centroids = clusterCentroids;
                // Assign each point to a cluster
                points.GLB.forEach(p -> p.assignCluster(centroids)).waitGlobalTermination();

                // Calculate the average position of each cluster
                AveragePosition avgClusterPosition = points.GLB.reduce(new AveragePosition(K, DIMENSION)).result();

                // Calculate the new centroid of each cluster
                ClosestPoint closestPoint = points.GLB
                        .reduce(new ClosestPoint(K, DIMENSION, avgClusterPosition.clusterCenters)).result();
                clusterCentroids = closestPoint.closestPointCoordinates;
                
                long iterEnd = System.nanoTime();
                System.out.println("Iter " + iter + "; " + (iterEnd - iterStart)/1e6 + " ms");
            }
        });

    }

    /**
     * Prints usage onto standard error output
     */
    private static void printUsage() {
        System.err.println("Usage: java -cp [...] handist.kmenas.KMeans "
                + "<point dimension> <nb of clusters \"k\"> <repetitions> <chunk size> <number of chunks> [<seed>]");
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
