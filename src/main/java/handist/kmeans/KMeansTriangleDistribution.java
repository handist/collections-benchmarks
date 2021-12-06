package handist.kmeans;

import static handist.kmeans.KMeansGlb.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import apgas.Constructs;
import apgas.Place;
import handist.collections.Chunk;
import handist.collections.LongRange;
import handist.collections.dist.DistChunkedList;
import handist.collections.dist.DistLog;
import handist.collections.dist.TeamedPlaceGroup;
import handist.collections.glb.Config;
import handist.collections.glb.GlobalLoadBalancer;
import handist.collections.util.SavedLog;

public class KMeansTriangleDistribution {

    public static void main(String[] args) {
        // ARGUMENT PARSING
        if (args.length < 6) {
            printUsage();
            return;
        }

        int dimension, k, repetitions, chunkSize, dataSize, chunkCount;
        long seed;
        String logFile = null;
        try {
            dimension = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            repetitions = Integer.parseInt(args[2]);
            chunkSize = Integer.parseInt(args[3]);
            dataSize = Integer.parseInt(args[4]);
            chunkCount = dataSize / chunkSize;
            logFile = args[5];
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

        Config.printConfiguration(System.err);
        System.err.println("Arguments received were " + Arrays.toString(args));

        // initialize final constants for easier lambda serialization later
        final int K = k;
        final int DIMENSION = dimension;
        final int REPETITIONS = repetitions;
        final TeamedPlaceGroup WORLD = TeamedPlaceGroup.getWorld();

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
                return new Point(initialPoints.get(l.intValue()));
            });
            points.add(c);
        }

        // We distribute the points in a triangular pattern
        PointDistribution.makeTriangleDistribution(points);

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
        final DistLog log = new DistLog();
        final double[][] clusterCentroids = initialClusterCenter;
        for (int iter = 0; iter < REPETITIONS; iter++) {
            final int iterF = iter; // Final for use inside closure
            GlobalLoadBalancer.underGLB(() -> {
                double[][] centroids = clusterCentroids; // Carry variable inside closure (cannot use clusterCentroids)

                final long iterStart = System.nanoTime();
                final double[][] centroidsF = centroids; // Final for use in closure in next line
                points.GLB.forEach(p -> p.assignCluster(centroidsF)).waitGlobalTermination();
                final long assignFinished = System.nanoTime(); // Time tracking

                // Compute the average position for each cluster
                final AveragePosition avgClusterPosition = new AveragePosition(K, DIMENSION);
                points.GLB.reduce(avgClusterPosition);
                final long avgFinished = System.nanoTime(); // Time tracking

                // Find the closest point to each centroid
                final ClosestPoint closestPoint = new ClosestPoint(K, DIMENSION, avgClusterPosition.clusterCenters);
                points.GLB.reduce(closestPoint).result();

                final long iterEnd = System.nanoTime(); // Time tracking

                centroids = closestPoint.closestPointCoordinates;

                // Part of the code used to track the distribution:
                final long[] size = new long[WORLD.size()];
                for (final Place p : WORLD.places()) {
                    size[p.id] = Constructs.at(p, () -> {
                        return points.size();
                    });
                }

                // Print the elapsed time, the number of trees, and the number of nodes explored
                // by each host on System.out
                final StringBuilder sb = new StringBuilder(
                        "Iter " + iterF + "; " + (iterEnd - iterStart) / 1e6 + "; " + (assignFinished - iterStart) / 1e6
                                + "; " + (avgFinished - assignFinished) / 1e6 + "; " + (iterEnd - avgFinished) / 1e6);
                for (final long l : size) {
                    sb.append("; " + l);
                }
                System.out.println(sb.toString());
            });
        }

        try {
            new SavedLog(log).saveToFile(new File(logFile));
        } catch (final IOException e) {
            System.err.println("Program encountered when saving GLB log to file");
            e.printStackTrace();
        }
    }

    /**
     * Prints usage onto standard error output
     */
    private static void printUsage() {
        System.err.println("Usage: java -cp [...] " + KMeansTriangleDistribution.class.getCanonicalName()
                + " <point dimension> <nb of clusters \"k\"> <repetitions> <chunk size> <number of points> [seed] [prefix for log file]");
        System.err.println(
                "This version of the KMeans benchmark purposely makes a triangular distribution instead of a flat one.");
        System.err.println(
                "This is used to test the capabilities of our GLB system to relocated entries to balance the load.");
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
