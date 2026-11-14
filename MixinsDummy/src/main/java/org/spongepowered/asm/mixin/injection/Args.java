package org.spongepowered.asm.mixin.injection;

/**
 * Argument bundle passed to a {@link ModifyArgs} handler.
 */
public class Args {

    public <T> T get(final int index) {
        return null;
    }

    public Object getType(final int index) {
        return null;
    }

    public void set(final int index, final Object value) {
    }

    public Object[] getAll() {
        return new Object[0];
    }

    public int size() {
        return 0;
    }

}
