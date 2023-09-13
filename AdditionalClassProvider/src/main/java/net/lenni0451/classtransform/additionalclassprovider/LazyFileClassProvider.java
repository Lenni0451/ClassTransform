package net.lenni0451.classtransform.additionalclassprovider;

import lombok.SneakyThrows;
import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.net.URI;
import java.nio.file.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

@ParametersAreNonnullByDefault
public class LazyFileClassProvider implements IClassProvider, AutoCloseable {

    @Nullable
    private final IClassProvider parent;
    private final LazyLoader[] loaders;

    public LazyFileClassProvider(final Collection<File> files) {
        this(null, files);
    }

    public LazyFileClassProvider(final Collection<File> files, @Nullable final IClassProvider parent) {
        this(parent, files);
    }

    public LazyFileClassProvider(@Nullable final IClassProvider parent, final Collection<File> files) {
        this.parent = parent;
        this.loaders = new LazyLoader[files.size()];

        int i = 0;
        for (File file : files) {
            this.loaders[i++] = new LazyLoader(file);
        }
    }

    @Nonnull
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        for (LazyLoader loader : this.loaders) {
            try {
                return loader.getClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (this.parent != null) return this.parent.getClass(name);
        throw new ClassNotFoundException(name);
    }

    @Nonnull
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        if (this.parent == null) return Collections.emptyMap();
        return this.parent.getAllClasses();
    }

    @Override
    public void close() throws Exception {
        for (LazyLoader loader : this.loaders) loader.close();
    }


    private static class LazyLoader implements AutoCloseable {
        private final File file;
        private FileSystem fileSystem;
        private Path root;

        private LazyLoader(final File file) {
            this.file = file;
        }

        @SneakyThrows
        private void open() {
            URI uri = new URI("jar:" + this.file.toURI());
            try {
                this.fileSystem = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                this.fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            }
            this.root = this.fileSystem.getRootDirectories().iterator().next();
        }

        private byte[] getClass(final String name) throws ClassNotFoundException {
            if (this.fileSystem == null) this.open();
            try {
                Path path = this.root.resolve(slash(name) + ".class");
                if (Files.exists(path)) return Files.readAllBytes(path);
            } catch (Throwable t) {
                throw new ClassNotFoundException(name, t);
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        public void close() throws Exception {
            if (this.fileSystem != null) {
                this.fileSystem.close();
                this.fileSystem = null;
            }
        }
    }

}
