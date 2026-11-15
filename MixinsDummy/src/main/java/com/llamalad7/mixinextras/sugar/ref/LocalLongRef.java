package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code long}.
 * <p>
 * Long values occupy two local slots in the JVM, so the referenced variable spans consecutive slot
 * indices.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalLongRef}.
 *
 * @see LocalRef
 */
public interface LocalLongRef {

    /**
     * Returns the current {@code long} value of the referenced local.
     *
     * @return The current long value
     */
    long get();

    /**
     * Stores a {@code long} value in the referenced local.
     *
     * @param value The long value to store
     */
    void set(long value);

}
