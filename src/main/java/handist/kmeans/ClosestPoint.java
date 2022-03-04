package handist.kmeans;

import handist.collections.dist.Reducer;

final public class ClosestPoint extends Reducer<ClosestPoint, Point> {

    /** Serial Version UID */
    private static final long serialVersionUID = -5053187857859985586L;

    public final double[][] closestPointCoordinates;
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
                // The reducer has a point closer than the prl held by this instance
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
        final double distance = KMeansGlb.distance(input.position, clusterAverage[input.clusterAssignment]);
        if (distance < distanceToAverage[input.clusterAssignment]) {
            distanceToAverage[input.clusterAssignment] = distance;
            for (int n = 0; n < input.position.length; n++) {
                closestPointCoordinates[input.clusterAssignment][n] = input.position[n];
            }
        }
    }

}