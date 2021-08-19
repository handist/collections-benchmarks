package handist.moldyn;

import handist.collections.Chunk;
import handist.collections.ChunkedList;
import handist.collections.LongRange;
import handist.collections.RangedList;

import java.util.concurrent.ConcurrentHashMap;

abstract public class AccumulatorManager<T, R> {

    private ChunkedList<T> target;

    private final ConcurrentHashMap<Thread, ChunkedList<R>> copies = new ConcurrentHashMap<>();
    public AccumulatorManager(ChunkedList<T> target) {
        this.target = target;
    }

    // thread 分用意した ChunkedList<R> を target にまとめる
    // まとめる方法は reduce() を実装して設定する
    public void merge() {
        copies.forEach((th, copy) -> {
            copy.forEachChunk((c) -> {
                //System.out.println("Merge: thread " + th.getId() + ", range" + c.getRange());
                RangedList<T> t = target.getChunk(c.getRange());
                if (t==null) {
                    System.out.println("error: "+target.ranges()+ " does not include "+ c.getRange());
                }
                t.map(c.getRange(), c, (t1, c1)->{
                    reduce(t1,c1);
                });
            });
        });
    }

    ChunkedList<R> getCopy() {
        final Thread thread = Thread.currentThread();
        final ChunkedList<R> ret = copies.get(thread);
        if (ret == null) {
            // target(ChunkedList<T>) と同じ range の Chunk を持つ
            // ChunkedList<R> を用意する
            // R の初期状態は init() を実装して設定する
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

     RangedList<R> getCopy(LongRange range) {
         if(range.size()==0) return null;
         return getCopy().getChunk(range).subList(range);
    }

    protected abstract R init();

    protected abstract void reduce(T target, R copy);
    public void close() {
        copies.clear();
    }

}
