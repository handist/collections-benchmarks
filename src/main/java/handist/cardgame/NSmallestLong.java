package handist.cardgame;

import java.util.ArrayList;
import java.util.List;

import handist.collections.dist.DistBag;
import handist.collections.dist.Reducer;
import handist.collections.glb.DistColGlb;

/**
 * This class is in charge of identifying the N smallest Long present in the {@link DistBag} produced by
 * the {@link DistColGlb#toBag(handist.collections.function.SerializableFunction)} operation.
 */
public class NSmallestLong extends Reducer<NSmallestLong, List<Long>> {

    /** Serial Version UID */
    private static final long serialVersionUID = -3133631916758540843L;

    /**
     * List of the n smallest elements found so far
     */
    List<Long> nSmallest;
    /**
     * Number of elements to keep
     */
    final int n;

    final int computationWeight;

    /**
     * Constructor
     *
     * @param nb number of smallest elements to keep
     */
    public NSmallestLong(int nb, int weight) {
        nSmallest = new ArrayList<>();
        n = nb;
        computationWeight = weight;
    }

    @Override
    public void merge(NSmallestLong reducer) {
        reduce(reducer.nSmallest);
    }

    @Override
    public NSmallestLong newReducer() {
        return new NSmallestLong(n, computationWeight);
    }

    @Override
    public void reduce(List<Long> input) {
        nSmallest.addAll(input);
        nSmallest.sort(null);
        //        final int size = nSmallest.size();
        // It is possible that the combines lists contain fewer than than n Longs, in which case we keep them all.
        // As method sublist throws an error when there are fewer elements than the desired number in method sublist,
        // we use the ternary operator to avoid it
        //        nSmallest = nSmallest.subList(0, size < n ? size : n);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Long l : nSmallest) {
            builder.append(l + " ");
        }

        return builder.toString();
    }
}