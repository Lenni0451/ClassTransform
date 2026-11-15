package com.llamalad7.mixinextras.injector.wrapoperation;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for a handler that wraps a matched operation and may invoke it.
 * <p>
 * The handler receives the operation arguments followed by an {@link Operation} for the original
 * operation, and may call it zero or more times. Method calls, field accesses, object creation and
 * constants can be wrapped. This compile-only compatibility stub has no behavior; the Mixin translator
 * maps the annotation to ClassTransform's operation wrapper.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapOperation {

    /**
     * The target methods containing the operations to wrap.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * The injection points selecting the operations to wrap.
     * <p>
     * Exactly one of this member and {@link #constant()} must be specified.
     *
     * @return The operation targets
     */
    At[] at() default {};

    /**
     * Constant discriminators selecting the constants to wrap. A {@code classValue} discriminator selects
     * {@code INSTANCEOF} checks instead of ordinary constants.
     * <p>
     * Exactly one of this member and {@link #at()} must be specified.
     *
     * @return The constant targets
     */
    Constant[] constant() default {};

    /**
     * Named slices that narrow the search for the selected operations.
     *
     * @return The slices
     */
    Slice[] slice() default {};

    /**
     * Whether the operation selectors and constant targets are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

    /**
     * The minimum number of matched operations that must be wrapped.
     *
     * @return The minimum wrapped operation count
     */
    int require() default -1;

    /**
     * The expected number of wrapped operations, used for count debugging.
     *
     * @return The expected wrapped operation count
     */
    int expect() default 1;

    /**
     * The maximum number of matched operations that may be wrapped.
     *
     * @return The maximum wrapped operation count
     */
    int allow() default -1;

    /**
     * The order in which this wrapper is applied; lower values run first.
     *
     * @return The application order
     */
    int order() default 1000;

}
