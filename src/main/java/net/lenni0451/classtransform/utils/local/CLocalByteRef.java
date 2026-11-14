package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code byte}.<br>
 * Instances are created by ClassTransform for transformer methods and must not be stored after the method returns.
 */
@ParametersAreNonnullByDefault
public class CLocalByteRef {

    private final Object[] values;
    private final int index;

    /**
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalByteRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * @return The current value of the local variable
     */
    public byte get() {
        return (Byte) this.values[this.index];
    }

    /**
     * @param value The new value of the local variable
     */
    public void set(final byte value) {
        this.values[this.index] = value;
    }

}
