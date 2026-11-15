package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Changes several arguments of a method or constructor invocation through an {@link Args} bundle. This stub only
 * carries the annotation values and has no behavior.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyArgs {

    /**
     * Target method names or patterns containing the invocation to modify.
     *
     * @return The target method patterns
     */
    String[] method() default {};

    /**
     * Descriptor selectors that match the methods containing the invocation whose argument list is passed to
     * the handler.
     *
     * @return The descriptors of the candidate methods
     */
    Desc[] target() default {};

    /**
     * A slice that narrows the invocation search.
     *
     * @return The slice
     */
    Slice slice() default @Slice;

    /**
     * The invocation instruction whose arguments are changed. This member is required.
     *
     * @return The invocation target
     */
    At at();

    /**
     * Controls remapping of the names referenced by this annotation.
     *
     * @return Whether references are remapped
     */
    boolean remap() default true;

    /**
     * How many injections must succeed at minimum; -1 uses the manager default.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * Expected match count for debugging; not enforced during normal translation.
     *
     * @return The expected match count
     */
    int expect() default 1;

    /**
     * Largest permitted injection count; values below one are ignored.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * A condition on the target that must hold for the modification to apply.
     *
     * @return The constraint expression
     */
    String constraints() default "";

    /**
     * Ordering key for this injector; lower numbers execute first.
     *
     * @return The application order
     */
    int order() default 1000;

}
