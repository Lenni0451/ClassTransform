package com.llamalad7.mixinextras.sugar.ref;

/**
 * A reference to a local variable of type {@code boolean}.
 *
 * @see LocalRef
 */
public interface LocalBooleanRef {

    boolean get();

    void set(boolean value);

}
