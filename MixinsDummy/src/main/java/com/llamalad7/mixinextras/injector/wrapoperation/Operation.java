package com.llamalad7.mixinextras.injector.wrapoperation;

/**
 * Represents a wrapped operation.
 *
 * @param <R> The return type of the operation
 */
@FunctionalInterface
public interface Operation<R> {

    R call(Object... args);

}
