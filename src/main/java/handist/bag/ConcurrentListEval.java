package handist.bag;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.ParallelReceiver;

public class ConcurrentListEval {

    public static class ConcListReceiver<T> implements ParallelReceiver<T> {

        final ConcurrentLinkedQueue<T> list;

        public ConcListReceiver() {
            list = new ConcurrentLinkedQueue<>();
        }

        @Override
        public void clear() {
            list.clear();
        }

        @Override
        public boolean contains(Object v) {
            return false;
        }

        @Override
        public Consumer<T> getReceiver() {
            return (o) -> list.add(o);
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public Iterator<T> iterator() {
            return list.iterator();
        }

        @Override
        public int size() {
            return list.size();
        }
    }

    public static void main(String[] args) {
        System.err
                .println(ConcurrentListEval.class.getCanonicalName() + " received arguments " + Arrays.toString(args));
        System.err.println("Available 'cores' on host: " + Runtime.getRuntime().availableProcessors());

        int actorCount, chunkSize, iterationCount, parallelism, computationWeight;
        int chunkCount;
        try {
            actorCount = Integer.parseInt(args[0]);
            chunkSize = Integer.parseInt(args[1]);
            iterationCount = Integer.parseInt(args[2]);
            parallelism = Integer.parseInt(args[3]);
            computationWeight = Integer.parseInt(args[4]);
            chunkCount = actorCount / chunkSize;
        } catch (final Exception e) {
            printUsage();
            return;
        }

        // INITIALIZATION
        final long initStart = System.nanoTime();
        final ChunkedList<Actor> actors = new ChunkedList<>();
        for (int chunkNumber = 0; chunkNumber < chunkCount; chunkNumber++) {
            final LongRange chunkRange = new LongRange(chunkNumber * chunkSize, (chunkNumber + 1) * chunkSize);
            final Chunk<Actor> c = new Chunk<>(chunkRange, l -> {
                return new Actor(l);
            });
            actors.add(c);
        }
        final long initElapsedTime = System.nanoTime() - initStart;
        System.out.println("Init  ; " + initElapsedTime / 1e6);

        // MAIN LOAD
        for (int i = 0; i < iterationCount; i++) {
            final ConcListReceiver<Order> orders = new ConcListReceiver<>();
            final long start = System.nanoTime();
            actors.parallelForEach(parallelism, (actor, collecter) -> {
                final Order o = actor.getOrder(computationWeight);
                collecter.accept(o);
            }, orders);

            final long elapsedTime = System.nanoTime() - start;

            System.out.println("Iter " + i + "; " + elapsedTime / 1e6);
        }
    }

    private static void printUsage() {
        System.err.println("Usage <actor count> <chunk size> <iteration count> <parallelism> <computation weight>");
    }
}
