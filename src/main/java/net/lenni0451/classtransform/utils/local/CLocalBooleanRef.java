package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code boolean}.
 *
 * <p>The JVM stores boolean local variables in int slots, but this class keeps the value boxed as {@link Boolean}, so
 * {@link #get()} returns a real boolean and {@link #set(boolean)} always stores a {@link Boolean}. A slot holding a
 * different wrapper type, for example an {@link Integer} written by an {@code int} reference, makes {@link #get()}
 * throw a {@link ClassCastException}.
 *
 * <p>ClassTransform creates instances for one transformer invocation only; they must not be kept after the
 * transformer method returns. The class is not thread-safe.
 */
@ParametersAreNonnullByDefault
public class CLocalBooleanRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code false}.
     */
    public CLocalBooleanRef() {
        this(new Object[]{false}, 0);
    }

    /**
     * Creates a reference backed by the given array slot, which has to hold a boxed {@link Boolean} for reads
     * to succeed.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalBooleanRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Boolean} from the backing slot, where the JVM keeps the value in an int slot.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Boolean}
     */
    public boolean get() {
        return (Boolean) this.values[this.index];
    }

    /**
     * Stores {@code value} as a boxed {@link Boolean} in the backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final boolean value) {
        this.values[this.index] = value;
    }

}
