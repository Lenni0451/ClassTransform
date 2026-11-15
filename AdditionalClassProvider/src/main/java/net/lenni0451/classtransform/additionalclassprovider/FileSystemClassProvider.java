package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.FileSystem;

/**
 * A class provider that reads classes from the root directory of a {@link FileSystem}, such as a jar file system.
 * <p>
 * The file system is not closed by this provider; use {@link ClosableFileSystemClassProvider} if the provider should
 * own and close it.
 */
@ParametersAreNonnullByDefault
public class FileSystemClassProvider extends PathClassProvider {

    private final FileSystem fileSystem;

    /**
     * Creates a provider for the root directory of the given file system.
     *
     * @param fileSystem The file system to read classes from
     */
    public FileSystemClassProvider(final FileSystem fileSystem) {
        this(null, fileSystem);
    }

    /**
     * Creates a provider for the root directory of the given file system.
     *
     * @param fileSystem The file system to read classes from
     * @param parent     The parent provider queried when a class is not found in the file system
     */
    public FileSystemClassProvider(final FileSystem fileSystem, @Nullable final IClassProvider parent) {
        this(parent, fileSystem);
    }

    /**
     * Creates a provider for the root directory of the given file system.
     *
     * @param parent     The parent provider queried when a class is not found in the file system
     * @param fileSystem The file system to read classes from
     */
    public FileSystemClassProvider(@Nullable final IClassProvider parent, final FileSystem fileSystem) {
        super(parent, fileSystem.getRootDirectories().iterator().next());
        this.fileSystem = fileSystem;
    }

    /**
     * Returns the file system this provider reads classes from.
     *
     * @return The file system
     */
    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

}
