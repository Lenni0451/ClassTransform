package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code double}.
 * <p>
 * Double values occupy two local slots and are exposed as double-precision primitives by this
 * interface.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalDoubleRef}.
 *
 * @see LocalRef
 */
public interface LocalDoubleRef {

    /**
     * Returns the double-precision value of the referenced local.
     *
     * @return The current double value
     */
    double get();

    /**
     * Sets the referenced local to the given {@code double}.
     *
     * @param value The double value to store
     */
    void set(double value);

}
