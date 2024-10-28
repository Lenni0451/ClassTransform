package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A dummy class provider that returns an empty class for every requested class.
 */
@ParametersAreNonnullByDefault
public class DummyClassProvider implements IClassProvider {

    @Nonnull
    @Override
    public byte[] getClass(String name) {
        ClassNode dummyClass = ASMUtils.createEmptyClass(name);
        return ASMUtils.toStacklessBytes(dummyClass);
    }

    @Nonnull
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        return Collections.emptyMap();
    }

}
