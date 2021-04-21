package handist.ufs;

import java.util.List;

import handist.collections.dist.Reducer;

/**
 * Class used to track the number of trees and the size of the trees explored on
 * each host
 *
 * @author Patrick Finnerty
 *
 */
public class LoadTracker extends Reducer<LoadTracker, List<Long>> {

    /** Serial Version UID */
    private static final long serialVersionUID = -8461100640237232990L;

    /** Number of trees processed on a host */
    public long treeCount = 0;
    /**
     * Cumulative sum of the number of nodes contained in the trees explored on a
     * host
     */
    double nodeCount = 0;

    @Override
    public void merge(LoadTracker reducer) {
        treeCount += reducer.treeCount;
        nodeCount += reducer.nodeCount;
    }

    @Override
    public LoadTracker newReducer() {
        return new LoadTracker();
    }

    @Override
    public void reduce(List<Long> input) {
        treeCount += input.size();
        for (final Long l : input) {
            nodeCount += l;
        }
    }

}
