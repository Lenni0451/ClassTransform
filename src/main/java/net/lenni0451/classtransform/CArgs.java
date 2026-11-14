package net.lenni0451.classtransform;

import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

/**
 * The argument bundle passed to {@link net.lenni0451.classtransform.annotations.injection.CModifyArgs} handlers.<br>
 * Values can be read and replaced by index. Primitive values are boxed in the bundle.
 */
@ParametersAreNonnullByDefault
public final class CArgs {

    private final Type[] types;
    private final Object[] values;

    /**
     * @param descriptor The descriptor of the modified invocation
     * @param values     The argument values
     */
    public CArgs(final String descriptor, final Object[] values) {
        this.types = Type.getArgumentTypes(descriptor);
        if (this.types.length != values.length) {
            throw new IllegalArgumentException("Expected " + this.types.length + " arguments but got " + values.length);
        }
        this.values = values.clone();
    }

    /**
     * @return The number of arguments
     */
    public int size() {
        return this.values.length;
    }

    /**
     * @param index The argument index
     * @return The argument type
     */
    public Type getType(final int index) {
        this.checkIndex(index);
        return this.types[index];
    }

    /**
     * Get an argument value.
     *
     * @param index The argument index
     * @param <T>   The argument type
     * @return The argument value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final int index) {
        this.checkIndex(index);
        return (T) this.values[index];
    }

    /**
     * @param index The argument index
     * @return The argument value as object
     */
    public Object getObject(final int index) {
        return this.get(index);
    }

    /**
     * Replace an argument value.
     *
     * @param index The argument index
     * @param value The new value
     */
    public void set(final int index, final Object value) {
        this.checkIndex(index);
        this.values[index] = value;
    }

    /**
     * Replace all argument values.
     *
     * @param values The new values
     */
    public void setAll(final Object... values) {
        if (values.length != this.values.length) {
            throw new IllegalArgumentException("Expected " + this.values.length + " arguments but got " + values.length);
        }
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    /**
     * @return A copy of all argument values
     */
    public Object[] getAll() {
        return this.values.clone();
    }

    public boolean getBoolean(final int index) {
        return this.get(index);
    }

    public void setBoolean(final int index, final boolean value) {
        this.set(index, value);
    }

    public byte getByte(final int index) {
        return this.get(index);
    }

    public void setByte(final int index, final byte value) {
        this.set(index, value);
    }

    public char getChar(final int index) {
        return this.get(index);
    }

    public void setChar(final int index, final char value) {
        this.set(index, value);
    }

    public short getShort(final int index) {
        return this.get(index);
    }

    public void setShort(final int index, final short value) {
        this.set(index, value);
    }

    public int getInt(final int index) {
        return this.get(index);
    }

    public void setInt(final int index, final int value) {
        this.set(index, value);
    }

    public long getLong(final int index) {
        return this.get(index);
    }

    public void setLong(final int index, final long value) {
        this.set(index, value);
    }

    public float getFloat(final int index) {
        return this.get(index);
    }

    public void setFloat(final int index, final float value) {
        this.set(index, value);
    }

    public double getDouble(final int index) {
        return this.get(index);
    }

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
