package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A class provider that queries a list of delegate providers in order.
 * <p>
 * Class bytes are returned from the first delegate that finds the class, and a {@link ClassNotFoundException} is only
 * thrown when every delegate fails. The class map returned by {@link #getAllClasses()} merges the maps of all
 * delegates, with entries of later delegates replacing earlier ones; delegates that do not support enumeration are
 * skipped.
 */
@ParametersAreNonnullByDefault
public class DelegatingClassProvider implements IClassProvider {

    private final IClassProvider[] delegates;

    /**
     * Creates a delegating provider.
     *
     * @param delegate  The first delegate to query
     * @param delegates Additional delegates queried in the given order
     */
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
        Map<String, Supplier<byte[]>> classes = new HashMap<>();
        for (IClassProvider delegate : this.delegates) {
            try {
                classes.putAll(delegate.getAllClasses());
            } catch (UnsupportedOperationException ignored) {
            }
        }
        return classes;
    }

}
