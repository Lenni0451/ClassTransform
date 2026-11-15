package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code long}.
 *
 * <p>Values are boxed as {@link Long}. Although a long local variable occupies two JVM stack slots, the index passed to
 * the constructor is a plain index into the backing array, so only one element is used. A slot holding a different
 * wrapper type makes {@link #get()} throw a {@link ClassCastException}.
 *
 * <p>Instances are not thread-safe and are only valid until the transformer method that received them returns.
 */
@ParametersAreNonnullByDefault
public class CLocalLongRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalLongRef() {
        this(new Object[]{0L}, 0);
    }

    /**
     * Creates a reference backed by a single array element, even though a long local variable occupies two JVM
     * stack slots; the slot has to hold a boxed {@link Long} for reads to succeed.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalLongRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Long} from the backing slot and unboxes it to {@code long}.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Long}
     */
    public long get() {
        return (Long) this.values[this.index];
    }

    /**
     * Boxes {@code value} as a {@link Long} and stores it in the single backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final long value) {
        this.values[this.index] = value;
    }

}
