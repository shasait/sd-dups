package de.hasait.util.dup;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class DupLeaf<T> extends AbstractDupNode<T> {

    private final CopyOnWriteArrayList<T> objects = new CopyOnWriteArrayList<>();

    public DupLeaf(AbstractDupBranch<?, T> parent) {
        super(parent);
    }

    @Override
    public void add(T object) {
        objects.add(object);
    }

    @Override
    public void collectObjects(List<DupObject<T>> result) {
        if (size() > 1) {
            objects.forEach(object -> result.add(new DupObject<>(this, object)));
        }
    }

    public int size() {
        return objects.size();
    }

    public Stream<T> objectsStream() {
        return objects.stream();
    }

    @Override
    public void remove(T object) {
        objects.remove(object);
    }

}
