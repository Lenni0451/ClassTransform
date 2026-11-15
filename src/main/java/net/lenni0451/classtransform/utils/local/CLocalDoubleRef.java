package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code double}.
 *
 * <p>Values are boxed as {@link Double} independently of the two JVM stack slots a double local variable occupies; the
 * index is a plain index into the backing array. The slot must contain a {@link Double}, otherwise {@link #get()}
 * throws a {@link ClassCastException}, and no conversion from {@code float} or {@code long} is applied.
 *
 * <p>Instances are not thread-safe and are only valid until the transformer method that received them returns.
 */
@ParametersAreNonnullByDefault
public class CLocalDoubleRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalDoubleRef() {
        this(new Object[]{0D}, 0);
    }

    /**
     * Creates a reference backed by one array element, although a double local variable occupies two JVM stack
     * slots; the slot has to contain a boxed {@link Double}.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalDoubleRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Double} and unboxes it to {@code double}, preserving NaN and signed zero.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Double}
     */
    public double get() {
        return (Double) this.values[this.index];
    }

    /**
     * Boxes {@code value} as a {@link Double} and stores it in the single backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final double value) {
        this.values[this.index] = value;
    }

}
