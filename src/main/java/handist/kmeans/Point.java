package handist.kmeans;

import java.io.Serializable;

public final class Point implements Serializable {
    /** Generated Serial Version UID */
    private static final long serialVersionUID = 903981107365981546L;

    /** Cluster id this point belongs to */
    int clusterAssignment;

    /** Coordinates array */
    public final double[] position;

    /**
     * Constructor
     *
     * @param initialPosition array of {@link Double} containing the point
     *                        coordinates of this point
     */
    public Point(Double[] initialPosition) {
        position = new double[initialPosition.length];
        for (int i = 0; i < position.length; i++) {
            position[i] = initialPosition[i];
        }
    }

    public void assignCluster(double[][] clusterCentroids) {
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < clusterCentroids.length; i++) {
            final double distance = KMeansGlb.distance(position, clusterCentroids[i]);
            if (distance < closestDistance) {
                closestDistance = distance;
                clusterAssignment = i;
            }
        }
    }
}