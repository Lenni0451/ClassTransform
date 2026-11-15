package org.spongepowered.asm.mixin.injection;

/**
 * Argument bundle passed to a {@link ModifyArgs} handler.
 * <p>
 * Compatibility stub: the methods return fixed placeholder values and perform no work.
 */
public class Args {

    /**
     * Returns the argument at the given index.
     *
     * @param <T> The expected argument type
     * @param index The argument index
     * @return The argument value
     */
    public <T> T get(final int index) {
        return null;
    }

    /**
     * Returns the type of the argument at the given index.
     *
     * @param index The argument index
     * @return The argument type
     */
    public Object getType(final int index) {
        return null;
    }

    /**
     * Replaces the argument at the given index.
     *
     * @param index The argument index
     * @param value The replacement value
     */
    public void set(final int index, final Object value) {
    }

    /**
     * Returns all arguments in index order.
     *
     * @return The argument values
     */
    public Object[] getAll() {
        return new Object[0];
    }

    /**
     * Returns the number of arguments.
     *
     * @return The argument count
     */
    public int size() {
        return 0;
    }

}
