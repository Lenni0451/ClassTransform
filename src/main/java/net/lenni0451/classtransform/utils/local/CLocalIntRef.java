package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code int}.
 *
 * <p>Values are boxed as {@link Integer}. This is also the type to use when the class file does not narrow a slot down
 * further, because {@code boolean}, {@code byte}, {@code char} and {@code short} all occupy int slots. The slot must
 * contain an {@link Integer}; when it holds a different wrapper type, {@link #get()} throws a
 * {@link ClassCastException}.
 *
 * <p>Instances are created by ClassTransform for a single transformer invocation and must not be stored after the
 * transformer method returns. The class is not thread-safe.
 */
@ParametersAreNonnullByDefault
public class CLocalIntRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalIntRef() {
        this(new Object[]{0}, 0);
    }

    /**
     * Creates a reference backed by the given array slot, which has to hold a boxed {@link Integer} for reads
     * to succeed.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalIntRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Integer} from the backing slot and unboxes it to {@code int}.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain an {@link Integer}
     */
    public int get() {
        return (Integer) this.values[this.index];
    }

    /**
     * Boxes {@code value} as an {@link Integer} and stores it in the backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final int value) {
        this.values[this.index] = value;
    }

}
