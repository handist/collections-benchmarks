package handist.noglb.kmeans;

import handist.collections.dist.Reducer;

class AveragePosition extends Reducer<AveragePosition, Point> {

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
                clusterCenters[k][d] = KMeans.weightedAverage(clusterCenters[k][d], includedPoints[k],
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
            clusterCenters[k][d] = KMeans.weightedAverage(clusterCenters[k][d], includedPoints[k], input.position[d],
                    1l);
        }
        includedPoints[k]++;
    }
}