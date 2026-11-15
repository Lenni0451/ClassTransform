package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding an {@code int}.
 * <p>
 * This is the reference for locals declared as {@code int} rather than as a narrower integer type
 * or {@code boolean}.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalIntRef}.
 *
 * @see LocalRef
 */
public interface LocalIntRef {

    /**
     * Returns the integer currently stored in the referenced local.
     *
     * @return The current integer value
     */
    int get();

    /**
     * Replaces the integer stored in the referenced local.
     *
     * @param value The integer value to store
     */
    void set(int value);

}
