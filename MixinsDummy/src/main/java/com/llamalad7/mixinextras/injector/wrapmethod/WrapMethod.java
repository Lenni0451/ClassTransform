package com.llamalad7.mixinextras.injector.wrapmethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for a handler that wraps a whole target method.
 * <p>
 * The handler receives the target method arguments followed by an
 * {@link com.llamalad7.mixinextras.injector.wrapoperation.Operation} that invokes the original body,
 * which may be called any number of times. This compile-only compatibility stub has no behavior; the
 * Mixin translator converts the annotation to ClassTransform's method wrapper.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapMethod {

    /**
     * The target methods to wrap.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * Whether the target method selectors are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

    /**
     * The minimum number of target methods that must be wrapped.
     *
     * @return The minimum wrapper count
     */
    int require() default -1;

    /**
     * The expected number of method wrappers, used for count debugging.
     *
     * @return The expected wrapper count
     */
    int expect() default 1;

    /**
     * The maximum number of target methods that may be wrapped.
     *
     * @return The maximum wrapper count
     */
    int allow() default -1;

    /**
     * The wrapper order; lower values become the inner wrappers.
     *
     * @return The wrapper order
     */
    int order() default 1000;

}
