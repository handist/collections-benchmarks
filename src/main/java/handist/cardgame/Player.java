package handist.cardgame;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Random;

/**
 * Player in our simulated card game
 */
public class Player implements Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = -5647140014720772065L;
    
    /** Weight of the computation of {@link Player} in method {@link #chooseCardToPlay(long)} */
    final int weight;
    /** Previous long result returned by method {@link #chooseCardToPlay(long)} */
    long previousCardChoice;
    /** Random number generator for this player */
    Random r;
    /** Increased player weight after winning a round */
    private int increasedWeight;

    /**
     * Method called on a player to obtain the next card which it is going to 
     * @return a card instance symbolizing the choice made by the player
     */
    public Long chooseCardToPlay(long winnerCard) {
        // If this player had the winning card, make its computation slightly longer
        int utsDepth = previousCardChoice <= winnerCard ? increasedWeight : weight;  

        //System.err.println("Depth; " + utsDepth);
        final MessageDigest md = UTS.encoder();
        UTS myProblem = new UTS(utsDepth);
        myProblem.seed(md, r.nextInt(), utsDepth);
        myProblem.run(md);

        previousCardChoice = myProblem.count;
        return myProblem.count;
    }

    /**
     * Constructor
     * <p>
     * The two "weight" parameters of this constructor represent the depth of the UTS tree performed by the player
     * @param seed random seed for this player
     * @param w computation weight
     * @param mw increased player weight after winning a round
     */
    public Player (long seed, int w, int mw) {
        r = new Random(seed);
        weight = w;
        increasedWeight = mw;
    }
}
