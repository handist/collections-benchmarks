package handist.perturbation;

import static apgas.Constructs.*;

import java.io.Serializable;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import apgas.Place;

/**
 * Pseudo-random configurable disturbance program
 *
 * @author Patrick Finnerty
 *
 */
public class Disturb implements Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 4309003539738115885L;

    public static final String OPT_THREAD_COUNT = "t";
    public static final String OPT_THREAD_COUNT_DEFAULT = "4";
    public static final String OPT_WORK_LOAD = "l";
    public static final String OPT_WORK_LOAD_DEFAULT = "60";

    public static void computeHashesUntil(long stopStamp, int seed) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        final byte[] hash = new byte[20];
        for (int i = 0; i < 16; ++i) {
            hash[i] = 0;
        }
        hash[16] = (byte) (seed >> 24);
        hash[17] = (byte) (seed >> 16);
        hash[18] = (byte) (seed >> 8);
        hash[19] = (byte) seed;
        md.update(hash, 0, 20);
        while (System.nanoTime() < stopStamp) {
            try {
                md.digest(hash, 0, 20);
            } catch (final DigestException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * Main method. Call without arguments to obtain the help / usage
     *
     * @param args arguments for this program
     */
    public static void main(String[] args) {
        final Options opts = makeOptions();
        if (args.length < 1) {
            printHelp(opts);
            return;
        }
        final String[] argsToParse = Arrays.copyOfRange(args, 0, args.length - 1);
        long seed = 0;
        try {
            seed = Long.parseLong(args[args.length - 1]);
        } catch (final Exception e) {
            System.err.println("Problem parsing the seed");
            printHelp(opts);
            return;
        }

        final CommandLineParser parser = new DefaultParser();
        String workTimeArguments, threadArgument;
        try {
            final CommandLine line = parser.parse(opts, argsToParse);
            workTimeArguments = line.getOptionValue(OPT_WORK_LOAD, OPT_WORK_LOAD_DEFAULT);
            threadArgument = line.getOptionValue(OPT_THREAD_COUNT, OPT_THREAD_COUNT_DEFAULT);
        } catch (final ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            printHelp(opts);
            return;
        }

        // Convert the options to usable parameters for the program
        // TODO enable the use of ranges
        final int threadCount = Integer.parseInt(threadArgument);
        final int workTime = Integer.parseInt(workTimeArguments);

        final Random rnd = new Random(seed);
        // Run program ad vitam eternam (until kill signal received)
        final int nbPlaces = places().size();
        System.out.println("stamp ; seconds since start ; victim ; time(s) ; thread cound");
        final long startStamp = System.nanoTime();
        for (;;) {
            final Place target = place(rnd.nextInt(nbPlaces));
            // TODO with ranges, determine nb threads / length of work here
            final long stamp = System.nanoTime();
            System.out.println(stamp + " ; " + (stamp - startStamp) / 1e9 + " ; " + target.id + " ; "
                    + workTimeArguments + " ; " + threadCount);
            at(target, () -> {
                final long stopStamp = (long) (System.nanoTime() + (workTime * 1e9));
                for (int i = 0; i < threadCount; i++) {
                    final int threadSeed = i;
                    async(() -> computeHashesUntil(stopStamp, threadSeed));
                }
            });
        }
    }

    /**
     * Construct the option description of this program.
     *
     * @return Appropriately built {@link Options} object
     */
    private static Options makeOptions() {
        final Options opts = new Options();

        opts.addOption(OPT_THREAD_COUNT, true,
                "Number of threads to occupy on a host with. Can be fixed or randomly chosen within a range [a,b] specified through format < a-b >.");
        opts.addOption(OPT_WORK_LOAD, true, "time spent active on a host in seconds. Defaults to 60s.");

        return opts;
    }

    private static void printHelp(Options opts) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java ... handist.perturbation.Disturb [options] seed", opts);
    }

}
