package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code boolean}.
 * <p>
 * The JVM stores boolean locals in int-sized slots, so this reference presents the value as a
 * {@code boolean} instead of exposing the underlying slot representation.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalBooleanRef}.
 *
 * @see LocalRef
 */
public interface LocalBooleanRef {

    /**
     * Returns the current value of the referenced {@code boolean} local.
     *
     * @return The current boolean value
     */
    boolean get();

    /**
     * Stores a new value in the referenced {@code boolean} local.
     *
     * @param value The new boolean value
     */
    void set(boolean value);

}
