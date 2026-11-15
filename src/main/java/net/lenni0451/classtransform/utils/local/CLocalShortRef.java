package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code short}.
 *
 * <p>Short values use int instructions in the JVM; this class boxes them as {@link Short} so the declared type is
 * preserved across the reference. Reading a slot that holds a different wrapper type throws a
 * {@link ClassCastException}, and values are neither widened nor narrowed automatically.
 *
 * <p>Instances are created per transformer invocation and must not outlive the transformer method. The class is not
 * thread-safe.
 */
@ParametersAreNonnullByDefault
public class CLocalShortRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalShortRef() {
        this(new Object[]{(short) 0}, 0);
    }

    /**
     * Creates a reference backed by the given array slot; a read expects a boxed {@link Short} in that slot.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalShortRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Short} from the backing slot and unboxes it to {@code short}.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Short}
     */
    public short get() {
        return (Short) this.values[this.index];
    }

    /**
     * Boxes {@code value} as a {@link Short} and stores it in the backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final short value) {
        this.values[this.index] = value;
    }

}
