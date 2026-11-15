package com.llamalad7.mixinextras.injector.wrapoperation;

/**
 * Represents a wrapped operation.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites it to ClassTransform's
 * {@code COperation}.
 *
 * @param <R> The return type of the operation
 */
@FunctionalInterface
public interface Operation<R> {

    /**
     * Invokes the next level of the operation chain, ending with the original operation.
     *
     * @param args The arguments for the operation
     * @return The result of the operation
     */
    R call(Object... args);

}
