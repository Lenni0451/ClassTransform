package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.FileSystem;

@ParametersAreNonnullByDefault
public class FileSystemClassProvider extends PathClassProvider {

    private final FileSystem fileSystem;

    public FileSystemClassProvider(final FileSystem fileSystem) {
        this(null, fileSystem);
    }

    public FileSystemClassProvider(final FileSystem fileSystem, @Nullable final IClassProvider parent) {
        this(parent, fileSystem);
    }

    public FileSystemClassProvider(@Nullable final IClassProvider parent, final FileSystem fileSystem) {
        super(parent, fileSystem.getRootDirectories().iterator().next());
        this.fileSystem = fileSystem;
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

}
