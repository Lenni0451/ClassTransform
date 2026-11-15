package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nullable;
import java.nio.file.FileSystem;

/**
 * A {@link FileSystemClassProvider} that closes the wrapped {@link FileSystem} when it is closed.
 * <p>
 * This provider takes ownership of the file system, so the caller must call {@link #close()} once the provider is no
 * longer needed.
 */
public class ClosableFileSystemClassProvider extends FileSystemClassProvider implements AutoCloseable {

    /**
     * Creates a provider for the root directory of the given file system.
     *
     * @param fileSystem The file system to read classes from, closed when this provider is closed
     */
    public ClosableFileSystemClassProvider(final FileSystem fileSystem) {
        super(fileSystem);
    }

    /**
     * Creates a provider for the root directory of the given file system.
     *
     * @param fileSystem The file system to read classes from, closed when this provider is closed
     * @param parent     The parent provider queried when a class is not found in the file system
     */
    public ClosableFileSystemClassProvider(final FileSystem fileSystem, @Nullable final IClassProvider parent) {
        super(fileSystem, parent);
    }

    /**
     * Creates a provider for the root directory of the given file system.
     *
     * @param parent     The parent provider queried when a class is not found in the file system
     * @param fileSystem The file system to read classes from, closed when this provider is closed
     */
    public ClosableFileSystemClassProvider(@Nullable final IClassProvider parent, final FileSystem fileSystem) {
        super(parent, fileSystem);
    }

    @Override
    public void close() throws Exception {
        this.getFileSystem().close();
    }

}
