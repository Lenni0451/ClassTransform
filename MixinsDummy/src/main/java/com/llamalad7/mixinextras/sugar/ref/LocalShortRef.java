package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code short}.
 * <p>
 * The value lives in an int-sized JVM slot, but reads and writes through this interface keep the
 * narrower {@code short} type.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalShortRef}.
 *
 * @see LocalRef
 */
public interface LocalShortRef {

    /**
     * Returns the current {@code short} value of the referenced local.
     *
     * @return The current short value
     */
    short get();

    /**
     * Assigns a new {@code short} value to the referenced local.
     *
     * @param value The short value to assign
     */
    void set(short value);

}
