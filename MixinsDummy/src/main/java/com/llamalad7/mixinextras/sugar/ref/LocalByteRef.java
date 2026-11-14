package com.llamalad7.mixinextras.sugar.ref;

/**
 * A reference to a local variable of type {@code byte}.
 *
 * @see LocalRef
 */
public interface LocalByteRef {

    byte get();

    void set(byte value);

}
