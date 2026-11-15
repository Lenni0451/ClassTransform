package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable holding an object value.
 *
 * <p>The value is read from and written to one slot of an {@link Object}{@code []} that is shared by all references of
 * a single transformer invocation. The array holds both references and boxed primitive values, so a primitive local
 * variable is accessed through one of the primitive reference classes, which store their wrapper type in the same
 * array rather than requiring separate storage.
 *
 * <p>ClassTransform creates instances for parameters annotated with {@code CLocalVariable} or {@code CShared}; they
 * are only valid until the transformer method returns, and keeping one afterwards leaves a stale view of the captured
 * local variable. The class performs no synchronization and is not thread-safe.
 *
 * @param <T> The compile time type of the local variable
 */
@ParametersAreNonnullByDefault
public class CLocalRef<T> {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code null}.
     */
    public CLocalRef() {
        this(new Object[]{null}, 0);
    }

    /**
     * Creates a reference to the object stored at the given array slot. No type check is performed until
     * {@link #get()} is called.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the object stored at the backing slot.
     *
     * <p>The cast to {@code T} is unchecked, so a value of an incompatible type causes a
     * {@link ClassCastException} at the call site.
     *
     * @return The current value of the local variable, possibly {@code null}
     */
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) this.values[this.index];
    }

    /**
     * Stores the given object in the backing slot.
     *
     * @param value The new value of the local variable, may be {@code null}
     */
    public void set(final T value) {
        this.values[this.index] = value;
    }

}
