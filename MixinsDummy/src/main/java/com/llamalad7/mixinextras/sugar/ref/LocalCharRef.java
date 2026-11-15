package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code char}.
 * <p>
 * Values are exposed as UTF-16 code units, matching the {@code char} type of the local, and occupy
 * the same int-sized slot as other narrow integer locals.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalCharRef}.
 *
 * @see LocalRef
 */
public interface LocalCharRef {

    /**
     * Returns the character currently held by the referenced local.
     *
     * @return The current character
     */
    char get();

    /**
     * Writes a character into the referenced local.
     *
     * @param value The character to store
     */
    void set(char value);

}
