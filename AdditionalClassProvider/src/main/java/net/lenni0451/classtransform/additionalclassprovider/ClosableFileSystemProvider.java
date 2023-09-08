package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nullable;
import java.nio.file.FileSystem;

public class ClosableFileSystemProvider extends FileSystemClassProvider implements AutoCloseable {

    public ClosableFileSystemProvider(final FileSystem fileSystem) {
        super(fileSystem);
    }

    public ClosableFileSystemProvider(final FileSystem fileSystem, @Nullable final IClassProvider parent) {
        super(fileSystem, parent);
    }

    public ClosableFileSystemProvider(@Nullable final IClassProvider parent, final FileSystem fileSystem) {
        super(parent, fileSystem);
    }

    @Override
    public void close() throws Exception {
        this.getFileSystem().close();
    }

}
