package de.hasait.sddups.service;

import de.hasait.util.dup.AbstractDupBranch;
import de.hasait.util.dup.AbstractDupNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 *
 */
public class FileContentHashDupBranch extends AbstractDupBranch<String, File> {

    private final long maxBytesToHash;

    public FileContentHashDupBranch(AbstractDupBranch<?, File> parent,
                                    Function<AbstractDupBranch<?, File>, AbstractDupNode<File>> childNodeFactory, long maxBytesToHash) {
        super(parent, childNodeFactory);
        this.maxBytesToHash = maxBytesToHash;
    }

    @Override
    protected String determineKey(File file) {
        try {
            try (InputStream fileIn = FileUtils.openInputStream(file); InputStream in = new BoundedInputStream(fileIn, maxBytesToHash)) {
                return DigestUtils.sha256Hex(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
