package net.lenni0451.classtransform.utils.tree;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * A basic class provider with only the {@link BasicClassProvider#getClass(String)} implemented.<br>
 * You need to register all transformer classes with direct paths
 */
public class BasicClassProvider implements IClassProvider {

    private final ClassLoader classLoader;

    public BasicClassProvider() {
        this(BasicClassProvider.class.getClassLoader());
    }

    public BasicClassProvider(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public byte[] getClass(String name) {
        try {
            InputStream is = this.classLoader.getResourceAsStream(slash(name) + ".class");
            if (is == null) throw new ClassNotFoundException(name);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
            return baos.toByteArray();
        } catch (Throwable t) {
            this.sneak(t);
        }
        throw new RuntimeException("Unable to find class '" + name + "'");
    }

    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        throw new UnsupportedOperationException("Not implemented");
    }


    private <T extends Throwable> void sneak(final Throwable t) throws T {
        throw (T) t;
    }

}
