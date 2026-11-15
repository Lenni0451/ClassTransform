package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code float}.
 * <p>
 * Float locals use a single local slot, and this interface exposes their values as single-precision
 * primitives.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalFloatRef}.
 *
 * @see LocalRef
 */
public interface LocalFloatRef {

    /**
     * Returns the single-precision value of the referenced local.
     *
     * @return The current float value
     */
    float get();

    /**
     * Sets the referenced local to the given {@code float}.
     *
     * @param value The float value to store
     */
    void set(float value);

}
