package de.hasait.util.dup;

public class DupObject<T> {

    private final AbstractDupNode<T> node;
    private final T object;

    public DupObject(AbstractDupNode<T> node, T object) {
        this.node = node;
        this.object = object;
    }

    public AbstractDupNode<T> getNode() {
        return node;
    }

    public T getObject() {
        return object;
    }

    public long getNodeId() {
        return node.getId();
    }

    public int getNodeSize() {
        if (node instanceof DupLeaf<T> leaf) {
            return leaf.size();
        }
        return 1;
    }

    public void remove() {
        node.remove(object);
    }

}
