package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class DelegatingClassProvider implements IClassProvider {

    private final IClassProvider[] delegates;

    public DelegatingClassProvider(final IClassProvider delegate, final IClassProvider... delegates) {
        this.delegates = new IClassProvider[delegates.length + 1];
        this.delegates[0] = delegate;
        System.arraycopy(delegates, 0, this.delegates, 1, delegates.length);
    }

    @Nonnull
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        for (IClassProvider delegate : this.delegates) {
            try {
                return delegate.getClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Nonnull
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        Map<String, Supplier<byte[]>> classes = this.delegates[0].getAllClasses();
        for (int i = 1; i < this.delegates.length; i++) classes.putAll(this.delegates[i].getAllClasses());
        return classes;
    }

}
