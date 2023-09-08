package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class DelegatingClassProvider implements IClassProvider {

    @Nullable
    private final IClassProvider delegate;

    public DelegatingClassProvider(@Nullable final IClassProvider delegate) {
        this.delegate = delegate;
    }

    @Nullable
    public IClassProvider getDelegate() {
        return this.delegate;
    }

    @Nonnull
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        if (this.delegate == null) throw new ClassNotFoundException(name);
        return this.delegate.getClass(name);
    }

    @Nonnull
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        if (this.delegate == null) return Collections.emptyMap();
        return this.delegate.getAllClasses();
    }

}
