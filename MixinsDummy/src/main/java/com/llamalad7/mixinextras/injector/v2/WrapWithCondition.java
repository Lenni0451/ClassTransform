package com.llamalad7.mixinextras.injector.v2;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for a handler that decides whether a matched operation is executed.
 * <p>
 * The handler returns {@code true} to run the wrapped operation and {@code false} to skip it. This is
 * the replacement for the deprecated annotation in the parent package.
 * <p>
 * This compile-only compatibility stub has no behavior; the Mixin translator rewrites it to
 * ClassTransform's wrap condition annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapWithCondition {

    /**
     * The target methods containing operations to evaluate with this condition.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * The supported targets are {@code INVOKE} and {@code FIELD}.
     *
     * @return The operations to wrap
     */
    At[] at();

    /**
     * Named slices that the condition targets may reference.
     *
     * @return The named slices available to injection points
     */
    Slice[] slice() default {};

    /**
     * Whether the operation selectors of this annotation are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

    /**
     * The minimum number of operations the condition must be applied to.
     *
     * @return The minimum wrapped operation count
     */
    int require() default -1;

    /**
     * The expected number of condition applications, used for count debugging.
     *
     * @return The expected wrapped operation count
     */
    int expect() default 1;

    /**
     * The maximum number of operations the condition may be applied to.
     *
     * @return The maximum wrapped operation count
     */
    int allow() default -1;

    /**
     * The order in which this condition is applied; lower values run first.
     *
     * @return The application order
     */
    int order() default 1000;

}
