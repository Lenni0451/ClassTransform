package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code float}.
 *
 * <p>Values are boxed as {@link Float} and read back with an exact type cast; neither a conversion from {@code int} nor
 * a widening to {@code double} happens. If the slot contains another wrapper type, {@link #get()} throws a
 * {@link ClassCastException}.
 *
 * <p>Instances are not thread-safe and are only valid until the transformer method that received them returns.
 */
@ParametersAreNonnullByDefault
public class CLocalFloatRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalFloatRef() {
        this(new Object[]{0F}, 0);
    }

    /**
     * Creates a reference backed by the given array slot; the slot has to hold a boxed {@link Float} rather
     * than an {@link Integer} or a {@link Double}.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalFloatRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Float} and unboxes it to {@code float}, preserving NaN and signed zero as stored.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Float}
     */
    public float get() {
        return (Float) this.values[this.index];
    }

    /**
     * Boxes {@code value} as a {@link Float} and stores it unchanged, including NaN values.
     *
     * @param value The new value of the local variable
     */
    public void set(final float value) {
        this.values[this.index] = value;
    }

}
