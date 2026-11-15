package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for a handler that replaces a value returned by the target method.
 * <p>
 * The handler receives the original return value and returns the replacement. This compile-only
 * compatibility stub has no behavior; the Mixin translator maps the annotation to ClassTransform's
 * return value modifier.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModifyReturnValue {

    /**
     * The target methods whose return values are modified.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * The supported targets are {@code RETURN} and {@code TAIL}.
     *
     * @return The return instructions
     */
    At[] at();

    /**
     * Named slices that the return targets may reference.
     *
     * @return The named slices available to injection points
     */
    Slice[] slice() default {};

    /**
     * Whether the return selectors are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

    /**
     * The minimum number of return sites that must be modified.
     *
     * @return The minimum modification count
     */
    int require() default -1;

    /**
     * The expected number of return modifications, used for count debugging.
     *
     * @return The expected modification count
     */
    int expect() default 1;

    /**
     * The maximum number of return sites that may be modified.
     *
     * @return The maximum modification count
     */
    int allow() default -1;

    /**
     * The order in which return value modifications are applied; lower values run first.
     *
     * @return The application order
     */
    int order() default 1000;

}
