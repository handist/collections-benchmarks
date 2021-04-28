package handist.kmeans;

import static handist.kmeans.KMeans.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.glb.Config;
import handist.kmeans.KMeans.AveragePosition;
import handist.kmeans.KMeans.ClosestPoint;
import handist.kmeans.KMeans.Point;

public class KMeansSequential {

    public static void main(String[] args) {
        // ARGUMENT PARSING
        if (args.length < 5) {
            printUsage();
            return;
        }

        int dimension, k, repetitions, chunkSize, dataSize, chunkCount;
        long seed;
        try {
            dimension = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            repetitions = Integer.parseInt(args[2]);
            chunkSize = Integer.parseInt(args[3]);
            dataSize = Integer.parseInt(args[4]);
            chunkCount = dataSize / chunkSize;
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

        Config.printConfiguration(System.err);
        System.err.println("Arguments received were " + Arrays.toString(args));

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
        // DistCol collection
        for (int chunkNumber = 0; chunkNumber < chunkCount; chunkNumber++) {
            final LongRange chunkRange = new LongRange(chunkNumber * chunkSize, (chunkNumber + 1) * chunkSize);
            final Chunk<Point> c = new Chunk<>(chunkRange, l -> {
                return new Point(initialPoints.get(l.intValue()));
            });
            points.add(c);
        }

        // We convert the list of initial centroids to a 2Darray
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

            // Assign each point to a cluster
            final double[][] centroids = clusterCentroids;
            points.forEach(p -> p.assignCluster(centroids));

            final long assignFinished = System.nanoTime(); // Time tracking

            // Compute the average position for each cluster
            final AveragePosition avgClusterPosition = new AveragePosition(K, DIMENSION);
            // Calculate the average position of each cluster
            points.reduce(avgClusterPosition);

            final long avgFinished = System.nanoTime(); // Time tracking

            // Find the closest point to each centroid
            final ClosestPoint closestPoint = new ClosestPoint(K, DIMENSION, avgClusterPosition.clusterCenters);
            // Calculate the new centroid of each cluster
            points.reduce(closestPoint);

            final long iterEnd = System.nanoTime(); // Time tracking

            clusterCentroids = closestPoint.closestPointCoordinates;

            // Part of the code used to track the distribution:
            final long size = points.size();

            // Print the elapsed time, the number of trees, and the number of nodes explored
            // by each host on System.out
            final StringBuilder sb = new StringBuilder(
                    "Iter " + iter + "; " + (iterEnd - iterStart) / 1e6 + "; " + (assignFinished - iterStart) / 1e6
                            + "; " + (avgFinished - assignFinished) / 1e6 + "; " + (iterEnd - avgFinished) / 1e6);
            sb.append("; " + size);
            System.out.println(sb.toString());

//            System.out.println("Iter " + iter + "; " + (iterEnd - iterStart) / 1e6 + "; " + "; "
//                    + (assignFinished - iterStart) / 1e6 + "; " + (avgFinished - assignFinished) / 1e6 + "; "
//                    + (iterEnd - avgFinished) / 1e6);
        }

    }

    /**
     * Prints usage onto standard error output
     */
    private static void printUsage() {
        System.err.println("Usage: java -cp [...] " + KMeansSequential.class.getCanonicalName()
                + " <point dimension> <nb of clusters \"k\"> <repetitions> <chunk size> <number of points> [seed]");
        System.err.println("This version of the KMeans benchmark is sequential.");
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
