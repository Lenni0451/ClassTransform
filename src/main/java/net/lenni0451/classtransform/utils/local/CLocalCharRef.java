package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code char}.<br>
 * Instances are created by ClassTransform for transformer methods and must not be stored after the method returns.
 */
@ParametersAreNonnullByDefault
public class CLocalCharRef {

    private final Object[] values;
    private final int index;

    /**
     * Create a new reference with the default value {@code 0}.
     */
    public CLocalCharRef() {
        this(new Object[]{(char) 0}, 0);
    }

    /**
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalCharRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * @return The current value of the local variable
     */
    public char get() {
        return (Character) this.values[this.index];
    }

    /**
     * @param value The new value of the local variable
     */
    public void set(final char value) {
        this.values[this.index] = value;
    }

}
