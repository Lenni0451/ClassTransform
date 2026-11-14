package com.llamalad7.mixinextras.sugar.ref;

/**
 * Represents a reference to a local variable.
 * Specialised variants are provided in the same package for all primitive types.
 *
 * @param <T> The type of the local variable
 */
public interface LocalRef<T> {

    /**
     * @return The current value of the variable
     */
    T get();

    /**
     * @param value The new value of the variable
     */
    void set(T value);

}
