package net.lenni0451.classtransform.additionalclassprovider;

import lombok.SneakyThrows;
import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;
import static net.lenni0451.classtransform.utils.ASMUtils.slash;
import static net.lenni0451.classtransform.utils.Sneaky.sneakySupply;

/**
 * A class provider that reads class files from a directory tree.
 * <p>
 * Classes are resolved relative to the given path, which usually points to a class output directory. Nothing is
 * cached: every lookup reads the file from disk, and the suppliers returned by {@link #getAllClasses()} read their
 * class file again each time they are invoked. Missing classes are delegated to an optional parent provider.
 */
@ParametersAreNonnullByDefault
public class PathClassProvider implements IClassProvider {

    @Nullable
    private final IClassProvider parent;
    private final Path path;

    /**
     * Creates a provider that reads classes from the given directory.
     *
     * @param path The directory containing the class files
     */
    public PathClassProvider(final Path path) {
        this(null, path);
    }

    /**
     * Creates a provider that reads classes from the given directory and falls back to a parent provider.
     *
     * @param path   The directory containing the class files
     * @param parent The parent provider queried when a class is not found
     */
    public PathClassProvider(final Path path, @Nullable final IClassProvider parent) {
        this(parent, path);
    }

    /**
     * Creates a provider that reads classes from the given directory and falls back to a parent provider.
     *
     * @param parent The parent provider queried when a class is not found
     * @param path   The directory containing the class files
     */
    public PathClassProvider(@Nullable final IClassProvider parent, final Path path) {
        this.parent = parent;
        this.path = path;
    }

    @Nonnull
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        try {
            Path path = this.path.resolve(slash(name) + ".class");
            if (Files.exists(path)) return Files.readAllBytes(path);
        } catch (Throwable t) {
            throw new ClassNotFoundException(name, t);
        }
        if (this.parent != null) return this.parent.getClass(name);
        throw new ClassNotFoundException(name);
    }

    @Nonnull
    @Override
    @SneakyThrows
    public Map<String, Supplier<byte[]>> getAllClasses() {
        try (Stream<Path> paths = Files.find(this.path, Integer.MAX_VALUE, (path, basicFileAttributes) -> path.getFileName().toString().endsWith(".class") && basicFileAttributes.isRegularFile())) {
            Map<String, Supplier<byte[]>> classes = paths
                    .collect(HashMap::new, (m, p) -> {
                        String name = dot(this.path.relativize(p).toString());
                        name = name.substring(0, name.length() - 6);
                        m.put(name, sneakySupply(() -> Files.readAllBytes(p)));
                    }, Map::putAll);
            if (this.parent != null) {
                Map<String, Supplier<byte[]>> parentClasses = this.parent.getAllClasses();
                parentClasses.putAll(classes);
                return parentClasses;
            }
            return classes;
        }
    }

}
