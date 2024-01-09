package de.hasait.sddups.service;

import de.hasait.util.dup.AbstractDupBranch;
import de.hasait.util.dup.AbstractDupNode;
import de.hasait.util.dup.DupLeaf;
import de.hasait.util.dup.DupObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileDupRepo {

    private final AbstractDupNode<File> root = level0();
    private final ConcurrentLinkedQueue<DupLeaf<File>> leafs = new ConcurrentLinkedQueue<>();

    public void add(File file) {
        root.add(file);
    }

    private AbstractDupNode<File> level0() {
        return new FileSizeDupBranch(null, this::level1);
    }

    private AbstractDupNode<File> level1(AbstractDupBranch<?, File> parent) {
        return new FileContentHashDupBranch(parent, this::level2, 4 * 1024);
    }

    private AbstractDupNode<File> level2(AbstractDupBranch<?, File> parent) {
        return new FileContentHashDupBranch(parent, this::level3, Long.MAX_VALUE);
    }

    private AbstractDupNode<File> level3(AbstractDupBranch<?, File> parent) {
        DupLeaf<File> leaf = new DupLeaf<>(parent);
        leafs.add(leaf);
        return leaf;
    }

    public Collection<DupLeaf<File>> getLeafs() {
        return leafs;
    }

    public Collection<DupObject<File>> collectObjects() {
        List<DupObject<File>> result = new ArrayList<>();
        root.collectObjects(result);
        return result;
    }

}
