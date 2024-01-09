package de.hasait.sddups.service;

import de.hasait.util.dup.AbstractDupBranch;
import de.hasait.util.dup.AbstractDupNode;

import java.io.File;
import java.util.function.Function;

/**
 *
 */
public class FileSizeDupBranch extends AbstractDupBranch<Long, File> {

    public FileSizeDupBranch(AbstractDupBranch<?, File> parent,
                             Function<AbstractDupBranch<?, File>, AbstractDupNode<File>> childNodeFactory) {
        super(parent, childNodeFactory);
    }

    @Override
    protected Long determineKey(File file) {
        return file.length();
    }

    @Override
    protected boolean filter(Long key, File object) {
        return key > 16 * 1024;
    }

}
