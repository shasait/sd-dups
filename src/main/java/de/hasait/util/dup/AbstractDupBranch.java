package de.hasait.util.dup;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public abstract class AbstractDupBranch<K, T> extends AbstractDupNode<T> {

    private final Function<AbstractDupBranch<?, T>, AbstractDupNode<T>> childNodeFactory;

    private final AtomicLong size = new AtomicLong();
    private final ConcurrentLinkedDeque<T> in = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<K, AbstractDupNode<T>> map = new ConcurrentHashMap<>();

    public AbstractDupBranch(AbstractDupBranch<?, T> parent, Function<AbstractDupBranch<?, T>, AbstractDupNode<T>> childNodeFactory) {
        super(parent);

        this.childNodeFactory = childNodeFactory;
    }

    @Override
    public final void add(T object) {
        if (size.incrementAndGet() < 2) {
            in.add(object);
        } else {
            while (true) {
                T previousObject = in.poll();
                if (previousObject != null) {
                    analyze(previousObject);
                } else {
                    break;
                }
            }
            analyze(object);
        }
    }

    @Override
    public final void collectObjects(List<DupObject<T>> result) {
        map.values().forEach(node -> node.collectObjects(result));
    }

    protected abstract K determineKey(T object);

    protected boolean filter(K key, T object) {
        return true;
    }

    private void analyze(T object) {
        K key = determineKey(object);
        if (filter(key, object)) {
            AbstractDupNode<T> childNode = map.computeIfAbsent(key, ignore -> childNodeFactory.apply(this));
            childNode.add(object);
        }
    }

    @Override
    public void remove(T object) {
        // na
    }

}
