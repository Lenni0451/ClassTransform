package net.lenni0451.classtransform.utils.local;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A mutable reference to a local variable of type {@code char}.
 *
 * <p>Characters are unsigned 16 bit values that the JVM handles in int slots, but this class stores them boxed as
 * {@link Character} and returns the raw UTF-16 code unit unchanged. A slot containing another wrapper type makes
 * {@link #get()} throw a {@link ClassCastException}, as there is no conversion from numbers to characters.
 *
 * <p>Instances are only valid for the transformer invocation that created them and are not thread-safe.
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
     * Creates a reference backed by the given array slot, which must contain a boxed {@link Character} unless
     * only {@link #set(char)} is used.
     *
     * @param values The backing array of local variable values
     * @param index  The index of the value in the backing array
     */
    public CLocalCharRef(final Object[] values, final int index) {
        this.values = values;
        this.index = index;
    }

    /**
     * Reads the boxed {@link Character} and returns its raw UTF-16 code unit.
     *
     * @return The current value of the local variable
     * @throws ClassCastException If the slot does not contain a {@link Character}
     */
    public char get() {
        return (Character) this.values[this.index];
    }

    /**
     * Boxes the code unit as a {@link Character} and stores it in the backing slot.
     *
     * @param value The new value of the local variable
     */
    public void set(final char value) {
        this.values[this.index] = value;
    }

}
