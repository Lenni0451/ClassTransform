package com.llamalad7.mixinextras.sugar.ref;

/**
 * A reference to a local variable of type {@code int}.
 *
 * @see LocalRef
 */
public interface LocalIntRef {

    int get();

    void set(int value);

}
