package handist.moldyn;

import static apgas.Constructs.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.RangedList;

abstract public class AccumulatorManager<T, R> {

    private final ChunkedList<T> target;

    private final ConcurrentHashMap<Thread, ChunkedList<R>> copies = new ConcurrentHashMap<>();

    public AccumulatorManager(ChunkedList<T> target) {
        this.target = target;
    }

    public void close() {
        copies.clear();
    }

    public ChunkedList<R> getCopy() {
        final Thread thread = Thread.currentThread();
        final ChunkedList<R> ret = copies.get(thread);
        if (ret == null) {
            // target(ChunkedList<T>) 縺ｨ蜷後§ range 縺ｮ Chunk 繧呈戟縺､
            // ChunkedList<R> 繧堤畑諢上☆繧�
            // R 縺ｮ蛻晄悄迥ｶ諷九�ｯ init() 繧貞ｮ溯｣�縺励※險ｭ螳壹☆繧�
            final ChunkedList<R> list = new ChunkedList<>();
            target.ranges().forEach((LongRange r) -> {
                list.add(new Chunk<R>(r));
                r.forEach((Long index) -> {
                    list.set(index, init());
                });
            });
            copies.put(thread, list);
            return list;
        }
        return ret;
    }

    public RangedList<R> getCopy(LongRange range) {
        if (range.size() == 0) {
            return null;
        }
        return getCopy().getChunk(range).subList(range);
    }

    protected abstract R init();

    // thread 蛻�逕ｨ諢上＠縺� ChunkedList<R> 繧� target 縺ｫ縺ｾ縺ｨ繧√ｋ
    // 縺ｾ縺ｨ繧√ｋ譁ｹ豕輔�ｯ reduce() 繧貞ｮ溯｣�縺励※險ｭ螳壹☆繧�
    public void merge() {
        copies.forEach((th, copy) -> {
            copy.forEachChunk((c) -> {
                // System.out.println("Merge: thread " + th.getId() + ", range" + c.getRange());
                final RangedList<T> t = target.getChunk(c.getRange());
                if (t == null) {
                    System.out.println("error: " + target.ranges() + " does not include " + c.getRange());
                }
                t.map(c.getRange(), c, (t1, c1) -> {
                    reduce(t1, c1);
                });
            });
        });
    }

    public void parallelMerge(int nthreads) {
        final List<ChunkedList<T>> split = target.separate(nthreads);
        finish(() -> {
            for (final ChunkedList<T> split0 : split) {
                async(() -> {
                    split0.forEachChunk((chunk) -> {
                        copies.forEach((thread, copy) -> {
                            final RangedList<R> copy1 = copy.subList1(chunk.getRange());
                            chunk.map(chunk.getRange(), copy1, (t1, c1) -> {
                                reduce(t1, c1);
                            });
                        });
                    });
                });
            }
        });
    }

    protected abstract void reduce(T target, R copy);
}
