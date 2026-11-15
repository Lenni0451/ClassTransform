package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Marker for a handler that replaces the value produced by a matched expression.
 * <p>
 * The handler receives the original expression value and returns the value to use instead. This
 * compile-only compatibility stub has no behavior; the Mixin translator maps the annotation to
 * ClassTransform's expression value modifier.
 */
public @interface ModifyExpressionValue {

    /**
     * The target methods whose expressions are modified.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * The supported targets are {@code INVOKE}, {@code FIELD}, {@code NEW} and {@code CONSTANT}.
     *
     * @return The target expressions
     */
    At[] at();

    /**
     * Named slices that the expression targets may reference.
     *
     * @return The named slices available to injection points
     */
    Slice[] slice() default {};

    /**
     * Whether the selectors in this annotation are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

    /**
     * The minimum number of expression matches that must be modified.
     *
     * @return The minimum match count
     */
    int require() default -1;

    /**
     * The expected number of matches, checked when injection count debugging is enabled.
     *
     * @return The expected match count
     */
    int expect() default 1;

    /**
     * The maximum number of matches that may be modified.
     *
     * @return The maximum match count
     */
    int allow() default -1;

    /**
     * The order in which expression value modifications are applied; lower values run first.
     *
     * @return The application order
     */
    int order() default 1000;

}
