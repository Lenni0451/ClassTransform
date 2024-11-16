package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;
import static net.lenni0451.classtransform.utils.ASMUtils.slash;

@ParametersAreNonnullByDefault
public class InstrumentationClassProvider implements IClassProvider, ClassFileTransformer {

    private final Set<ClassLoader> classLoaders = Collections.newSetFromMap(new WeakHashMap<>());

    public InstrumentationClassProvider(final Instrumentation instrumentation) {
        this.classLoaders.add(ClassLoader.getSystemClassLoader());
        instrumentation.addTransformer(this);
    }

    @Nonnull
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        name = dot(name);
        for (ClassLoader loader : this.classLoaders) {
            try {
                return this.getClassBytes(loader, name);
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                throw new ClassNotFoundException(name, t);
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Nonnull
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        throw new UnsupportedOperationException();
    }

    private byte[] getClassBytes(final ClassLoader loader, final String clazz) throws IOException, ClassNotFoundException {
        InputStream is = loader.getResourceAsStream(slash(clazz) + ".class");
        if (is == null) throw new ClassNotFoundException(clazz);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) >= 0) baos.write(buf, 0, len);
        return baos.toByteArray();
    }

    @Override
    public byte[] transform(@Nullable ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (loader != null) InstrumentationClassProvider.this.classLoaders.add(loader);
        return null;
    }

}
