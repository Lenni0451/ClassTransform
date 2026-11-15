package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code byte}.
 *
 * <p>Byte values are widened to int in JVM stack frames, so this class boxes the value as {@link Byte} to keep the
 * declared type visible to the transformer. The slot must actually contain a {@link Byte}; any other wrapper type,
 * such as the {@link Integer} of an {@code int} reference, makes {@link #get()} throw a {@link ClassCastException}.
 * No numeric conversion or narrowing is performed on read or write.
 *
 * <p>Instances belong to a single transformer invocation and must not be stored after the transformer method
 * returns. The class is not thread-safe.
 */
@ParametersAreNonnullByDefault
public class CLocalByteRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalByteRef() {
        this(new Object[]{(byte) 0}, 0);
    }

    /**
     * Creates a reference backed by the given array slot. The slot has to hold a boxed {@link Byte}, as no
     * narrowing from a wider wrapper type is applied.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalByteRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Byte} from the backing slot and unboxes it to {@code byte}.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Byte}
     */
    public byte get() {
        return (Byte) this.values[this.index];
    }

    /**
     * Boxes {@code value} as a {@link Byte} and stores it in the backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final byte value) {
        this.values[this.index] = value;
    }

}
