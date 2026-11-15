package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rewrites a local variable at a chosen instruction. This compatibility stub carries the variable selector and does not
 * modify anything.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyVariable {

    /**
     * Methods whose local variables are modified, by name or pattern.
     *
     * @return The target method patterns
     */
    String[] method() default {};

    /**
     * Descriptors selecting the methods whose variables are modified.
     *
     * @return The target descriptors
     */
    Desc[] target() default {};

    /**
     * Slice that limits where the variable is searched for.
     *
     * @return The slice
     */
    Slice slice() default @Slice;

    /**
     * The instruction at which the variable value is changed. This member is required.
     *
     * @return The injection point
     */
    At at();

    /**
     * Prints the local variables visible at the injection point when the target cannot be resolved.
     *
     * @return Whether local variables are printed
     */
    boolean print() default false;

    /**
     * Selects the variable by its ordinal among variables of the handler's type; -1 disables that selection.
     *
     * @return The variable ordinal or -1
     */
    int ordinal() default -1;

    /**
     * Absolute local variable index to change; -1 disables index selection.
     *
     * @return The variable index or -1
     */
    int index() default -1;

    /**
     * Candidate names of the variable to change.
     *
     * @return The variable names
     */
    String[] name() default {};

    /**
     * Restricts matching to method arguments, ignoring locals created inside the method.
     *
     * @return Whether only arguments are considered
     */
    boolean argsOnly() default false;

    /**
     * Turns remapping of this injector's selectors on or off.
     *
     * @return Whether selectors are remapped
     */
    boolean remap() default true;

    /**
     * Required count of applied injections; -1 leaves the policy to the translator.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * Forecast of the injection count, consulted only by count debugging.
     *
     * @return The expected injection count
     */
    int expect() default 1;

    /**
     * Maximum applied injections allowed; negative values impose no limit.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * Expression checked before the variable is modified.
     *
     * @return The constraint expression
     */
    String constraints() default "";

    /**
     * Relative execution order; lower values take precedence.
     *
     * @return The application order
     */
    int order() default 1000;

}
