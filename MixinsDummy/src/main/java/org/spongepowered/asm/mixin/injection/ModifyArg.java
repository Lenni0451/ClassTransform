package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Changes a single argument of a method or constructor invocation. Compatibility stub only: the annotation holds
 * selector values and performs no transformation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyArg {

    /**
     * Methods whose invocations are targeted, by name or name-and-descriptor pattern.
     *
     * @return The target method patterns
     */
    String[] method() default {};

    /**
     * Descriptor selectors that match the methods containing the invocation whose argument is changed.
     *
     * @return The descriptors of the candidate methods
     */
    Desc[] target() default {};

    /**
     * Slice limiting the search region for the target invocation.
     *
     * @return The slice
     */
    Slice slice() default @Slice;

    /**
     * The invocation instruction whose argument is changed. This member is required.
     *
     * @return The invocation target
     */
    At at();

    /**
     * Argument index to change; -1 infers it from the handler parameter type.
     *
     * @return The argument index or -1
     */
    int index() default -1;

    /**
     * Whether this injector's targets are subject to remapping.
     *
     * @return Whether targets are remapped
     */
    boolean remap() default true;

    /**
     * Lower bound on applied injections; -1 defers to the default requirement.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * Anticipated number of matches; used for diagnostics when count checks are on.
     *
     * @return The expected match count
     */
    int expect() default 1;

    /**
     * Upper bound on applied matches; a negative value means no explicit limit.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * Constraint expression gating whether the injection runs.
     *
     * @return The constraint expression
     */
    String constraints() default "";

    /**
     * Sequence position among injectors; smaller values are applied earlier.
     *
     * @return The application order
     */
    int order() default 1000;

}
