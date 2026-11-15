package com.llamalad7.mixinextras.sugar.ref;

/**
 * A mutable reference to a local variable holding a {@code byte}.
 * <p>
 * Narrow integer locals share the int-sized JVM slot representation, while this reference keeps the
 * declared {@code byte} type visible to the injection handler.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code CLocalByteRef}.
 *
 * @see LocalRef
 */
public interface LocalByteRef {

    /**
     * Returns the current {@code byte} stored in the referenced local.
     *
     * @return The current byte value
     */
    byte get();

    /**
     * Updates the referenced local with the supplied {@code byte}.
     *
     * @param value The byte value to store
     */
    void set(byte value);

}
