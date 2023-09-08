package net.lenni0451.classtransform.utils.tree;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * A basic class provider with only the {@link #getClass(String)} implemented.<br>
 * You need to register all transformer classes with direct paths as the {@link #getAllClasses()} method is required for wildcard matching.
 */
@ParametersAreNonnullByDefault
public class BasicClassProvider implements IClassProvider {

    protected ClassLoader classLoader;

    public BasicClassProvider() {
        this(BasicClassProvider.class.getClassLoader());
    }

    public BasicClassProvider(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    @Nonnull
    public byte[] getClass(String name) throws ClassNotFoundException {
        try (InputStream is = this.classLoader.getResourceAsStream(slash(name) + ".class")) {
            Objects.requireNonNull(is, "Class input stream is null");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
            return baos.toByteArray();
        } catch (Throwable t) {
            throw new ClassNotFoundException(name, t);
        }
    }

    @Override
    @Nonnull
    public Map<String, Supplier<byte[]>> getAllClasses() {
        throw new UnsupportedOperationException("Not implemented");
    }

}
