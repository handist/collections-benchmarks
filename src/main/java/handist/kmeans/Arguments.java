package handist.kmeans;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Arguments {

    public static final String NB_CLUSTERS = "k";
    public static final String DIMENSION = "n";
    public static final String NB_CHUNKS = "c";
    public static final String PTS_PER_CHUNKS = "p";
    public static final String NB_ITERATIONS = "i";
    public static final String DISTRIBUTION = "d";
    public static final String SEED = "s";

    public static final Options opts = makeOptions();

    /**
     * Construct the option description of this program.
     *
     * @return Appropriately built {@link Options} object
     */
    private static Options makeOptions() {
        final Options opts = new Options();

        opts.addRequiredOption(NB_CLUSTERS, "nbCluster", true, "number of clusters <k>");
        opts.addRequiredOption(DIMENSION, "dimension", true, "dimension of individual points");
        opts.addRequiredOption(NB_ITERATIONS, "iterations", true, "number of iterations of the kmeans algorithm");
        opts.addRequiredOption(NB_CHUNKS, "chunks", true, "number of chunks");
        opts.addRequiredOption(PTS_PER_CHUNKS, "ptsPerChunk", true, "p");
        opts.addRequiredOption(DISTRIBUTION, "distribution", true, "initial point distribution (flat/triangle)");
        opts.addRequiredOption(SEED, "seed", true, "seed used to generate the points");

        return opts;
    }

    public static void printHelp(String mainClass) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java ... " + mainClass, opts);
    }

    public final int k, dim, iterations, ptsPerChunk, nbChunks;

    public final long seed;

    public final String distribution;

    /**
     * Parses the arguments and saves them to the public members of this class
     * instance
     *
     * @param args string arguments to parse
     * @throws ParseException if an option / argument was not set
     */
    public Arguments(String[] args) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        final CommandLine l = parser.parse(opts, args);

        k = Integer.parseInt(l.getOptionValue(NB_CLUSTERS));
        dim = Integer.parseInt(l.getOptionValue(DIMENSION));
        iterations = Integer.parseInt(l.getOptionValue(NB_ITERATIONS));
        nbChunks = Integer.parseInt(l.getOptionValue(NB_CHUNKS));
        ptsPerChunk = Integer.parseInt(l.getOptionValue(PTS_PER_CHUNKS));
        seed = Long.parseLong(l.getOptionValue(SEED));
        distribution = l.getOptionValue(DISTRIBUTION);
    }

}
