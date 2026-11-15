package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding an object of type {@code T}.
 * <p>
 * Primitive local variables use the dedicated interfaces in this package, because {@code T} cannot
 * be a primitive type.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalRef}.
 *
 * @param <T> The reference type of the local variable
 */
public interface LocalRef<T> {

    /**
     * Returns the object currently held by the referenced local variable.
     *
     * @return The current value
     */
    T get();

    /**
     * Replaces the object held by the referenced local variable.
     *
     * @param value The new value of the variable
     */
    void set(T value);

}
