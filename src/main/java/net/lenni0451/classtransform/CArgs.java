package net.lenni0451.classtransform;

import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

/**
 * The argument bundle passed to {@link net.lenni0451.classtransform.annotations.injection.CModifyArgs} handlers.<br>
 * Values can be read and replaced by index. Primitive values are boxed in the bundle.
 * <p>
 * The bundle owns its value array: the constructor copies the supplied array and {@link #getAll()}
 * returns a copy, so changing either array does not affect the bundle.
 */
@ParametersAreNonnullByDefault
public final class CArgs {

    private final Type[] types;
    private final Object[] values;

    /**
     * Create a new argument bundle from a method descriptor.
     *
     * @param descriptor The descriptor of the modified invocation
     * @param values     The argument values, one per descriptor argument
     * @throws IllegalArgumentException If the number of values does not match the descriptor
     */
    public CArgs(final String descriptor, final Object[] values) {
        this.types = Type.getArgumentTypes(descriptor);
        if (this.types.length != values.length) {
            throw new IllegalArgumentException("Expected " + this.types.length + " arguments but got " + values.length);
        }
        this.values = values.clone();
    }

    /**
     * Get the number of arguments in the bundle.
     *
     * @return The number of arguments
     */
    public int size() {
        return this.values.length;
    }

    /**
     * Get the declared type of an argument.
     *
     * @param index The argument index
     * @return The argument type
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public Type getType(final int index) {
        this.checkIndex(index);
        return this.types[index];
    }

    /**
     * Get an argument value.
     * <p>
     * The cast to {@code T} is unchecked, so requesting a type that does not match the stored
     * value fails with a {@link ClassCastException} at the call site.
     *
     * @param index The argument index
     * @param <T>   The argument type
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final int index) {
        this.checkIndex(index);
        return (T) this.values[index];
    }

    /**
     * Get an argument value as an object without a generic cast.
     *
     * @param index The argument index
     * @return The argument value as object
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public Object getObject(final int index) {
        return this.get(index);
    }

    /**
     * Replace an argument value.
     * <p>
     * Primitive arguments must be passed boxed.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void set(final int index, final Object value) {
        this.checkIndex(index);
        this.values[index] = value;
    }

    /**
     * Replace all argument values at once.
     *
     * @param values The new values, one per argument
     * @throws IllegalArgumentException If the number of values does not match the bundle size
     */
    public void setAll(final Object... values) {
        if (values.length != this.values.length) {
            throw new IllegalArgumentException("Expected " + this.values.length + " arguments but got " + values.length);
        }
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    /**
     * Get a copy of all argument values.
     *
     * @return A copy of all argument values
     */
    public Object[] getAll() {
        return this.values.clone();
    }

    /**
     * Get the argument at the given index as a boolean.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed boolean
     */
    public boolean getBoolean(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a boolean value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setBoolean(final int index, final boolean value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as a byte.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed byte
     */
    public byte getByte(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a byte value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setByte(final int index, final byte value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as a char.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed char
     */
    public char getChar(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a char value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setChar(final int index, final char value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as a short.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed short
     */
    public short getShort(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a short value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setShort(final int index, final short value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as an int.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed int
     */
    public int getInt(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to an int value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setInt(final int index, final int value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as a long.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed long
     */
    public long getLong(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a long value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setLong(final int index, final long value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as a float.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed float
     */
    public float getFloat(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a float value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setFloat(final int index, final float value) {
        this.set(index, value);
    }

    /**
     * Get the argument at the given index as a double.
     *
     * @param index The argument index
     * @return The argument value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws ClassCastException        If the argument is not a boxed double
     */
    public double getDouble(final int index) {
        return this.get(index);
    }

    /**
     * Set the argument at the given index to a double value.
     *
     * @param index The argument index
     * @param value The new value
     * @throws IndexOutOfBoundsException If the index is out of bounds
     */
    public void setDouble(final int index, final double value) {
        this.set(index, value);
    }

    private void checkIndex(final int index) {
        if (index < 0 || index >= this.values.length) {
            throw new IndexOutOfBoundsException("Argument index " + index + " is out of bounds for " + this.values.length + " arguments");
        }
    }

    @Override
    public String toString() {
        return "CArgs" + Arrays.toString(this.values);
    }

}
