package com.llamalad7.mixinextras.sugar.ref;

/**
 * A reference to a local variable of type {@code double}.
 *
 * @see LocalRef
 */
public interface LocalDoubleRef {

    double get();

    void set(double value);

}
