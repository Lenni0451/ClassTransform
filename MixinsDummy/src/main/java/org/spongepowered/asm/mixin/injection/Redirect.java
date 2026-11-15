package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redirects a method call, field access or object creation to a handler method. The stub is a compatibility carrier for
 * the selector values and has no behavior.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {

    /**
     * Methods containing the calls to redirect, by name or pattern.
     *
     * @return The target method patterns
     */
    String[] method() default {};

    /**
     * Descriptor selectors for candidate target methods.
     *
     * @return The target descriptors
     */
    Desc[] target() default {};

    /**
     * The call, field access or object creation to redirect. This member is required.
     *
     * @return The redirect target
     */
    At at();

    /**
     * Slice used to restrict the matched call sites.
     *
     * @return The slice
     */
    Slice slice() default @Slice;

    /**
     * Constraint that must hold for the redirect to be installed.
     *
     * @return The constraint expression
     */
    String constraints() default "";

    /**
     * The least number of successful injections; -1 falls back to the default.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * The count expected at translation time, used by diagnostic checks.
     *
     * @return The expected injection count
     */
    int expect() default 1;

    /**
     * Injection count ceiling; values below one are ignored.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * Determines when this injector runs; lower values come first.
     *
     * @return The application order
     */
    int order() default 10000;

    /**
     * Whether the targets named here are remapped.
     *
     * @return Whether targets are remapped
     */
    boolean remap() default true;

}
