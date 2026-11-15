package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify the receiver of a non-static method call or field access.<br>
 * The handler receives the original receiver, the arguments of the operation and optionally
 * a prefix of the enclosing method arguments.
 * <p>
 * This compile-only compatibility stub has no behavior; the Mixin translator rewrites it to
 * ClassTransform's receiver modifier.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyReceiver {

    /**
     * The target methods whose operations are modified.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * The supported targets are {@code INVOKE} and {@code FIELD}.
     *
     * @return The operations to modify
     */
    At[] at();

    /**
     * Named slices that the receiver targets may reference.
     *
     * @return The named slices available to injection points
     */
    Slice[] slice() default {};

    /**
     * Whether the operation targets of this annotation are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

    /**
     * The minimum number of receiver modifications that must succeed.
     *
     * @return The minimum modification count
     */
    int require() default -1;

    /**
     * The expected number of receiver modifications, used for count debugging.
     *
     * @return The expected modification count
     */
    int expect() default 1;

    /**
     * The maximum number of receivers that may be modified.
     *
     * @return The maximum modification count
     */
    int allow() default -1;

    /**
     * The order in which receiver modifications are applied; lower values run first.
     *
     * @return The application order
     */
    int order() default 1000;

}
