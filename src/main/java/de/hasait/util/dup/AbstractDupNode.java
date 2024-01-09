package de.hasait.util.dup;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractDupNode<T> {

    private static final AtomicLong nextId = new AtomicLong();

    private final long id = nextId.incrementAndGet();

    private final AbstractDupBranch<?, T> parent;

    public AbstractDupNode(AbstractDupBranch<?, T> parent) {
        this.parent = parent;
    }

    public final AbstractDupBranch<?, T> getParent() {
        return parent;
    }

    public final long getId() {
        return id;
    }

    public abstract void add(T object);

    public abstract void collectObjects(List<DupObject<T>> result);

    public abstract void remove(T object);

}
