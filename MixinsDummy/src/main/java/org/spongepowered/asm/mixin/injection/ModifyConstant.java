package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replaces constant literals found in a target method. This compatibility stub holds the selector configuration and
 * has no behavior.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyConstant {

    /**
     * Methods in which constants are replaced, by name or name-and-descriptor pattern. This member is required.
     *
     * @return The target method patterns
     */
    String[] method();

    /**
     * Slices that narrow the constant search; none are declared by default.
     *
     * @return The slices
     */
    Slice[] slice() default {};

    /**
     * The constant selectors to match. This member is required.
     *
     * @return The constant selectors
     */
    Constant[] constant();

    /**
     * Minimum number of matches that must be applied; a negative value means the default.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * Number of injections anticipated; reported by count diagnostics.
     *
     * @return The expected injection count
     */
    int expect() default 1;

    /**
     * Cap on matches; -1 or lower disables the cap.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * Position in the injection sequence, with lower values applied first.
     *
     * @return The application order
     */
    int order() default 10000;

    /**
     * Whether references in the annotation should be remapped.
     *
     * @return Whether references are remapped
     */
    boolean remap() default true;

}
