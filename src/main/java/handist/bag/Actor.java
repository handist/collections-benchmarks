package handist.bag;

import java.util.Random;

public class Actor {

    Random r;

    public Actor(long seed) {
        r = new Random(seed);
    }

    /**
     * Get Order method with adjustable amount of computation weight.
     * <p>
     * The computation weight is
     *
     * @param computationWeight number of times {@link Random#nextFloat()} is
     *                          called, positive or nil integer
     * @return a new {@link Order} object instance
     */
    public Order getOrder(int computationWeight) {
        for (; computationWeight > 0; computationWeight--) {
            r.nextFloat();
        }
        return new Order(r.nextLong());
    }
}
