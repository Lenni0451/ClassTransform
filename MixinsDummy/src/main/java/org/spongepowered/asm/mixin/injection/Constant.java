package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects a constant literal for a constant-modifying injector such as {@link ModifyConstant}, or for
 * {@link com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation#constant()}. The stub stores only selector
 * values and performs no matching itself.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Constant {

    /**
     * Whether {@code null} is the constant to match.
     *
     * @return Whether to match {@code null}
     */
    boolean nullValue() default false;

    /**
     * The {@code int} constant to match.
     *
     * @return The int constant
     */
    int intValue() default 0;

    /**
     * The {@code float} constant to match.
     *
     * @return The float constant
     */
    float floatValue() default 0.0F;

    /**
     * The {@code long} constant to match.
     *
     * @return The long constant
     */
    long longValue() default 0L;

    /**
     * The {@code double} constant to match.
     *
     * @return The double constant
     */
    double doubleValue() default 0.0;

    /**
     * The {@code String} constant to match.
     *
     * @return The string constant
     */
    String stringValue() default "";

    /**
     * The class constant to match.
     *
     * @return The class constant
     */
    Class<?> classValue() default Object.class;

    /**
     * Which match to modify when several constants qualify; -1 does not select by ordinal.
     *
     * @return The ordinal or -1
     */
    int ordinal() default -1;

    /**
     * Identifier of the slice used to narrow the constant search.
     *
     * @return The slice identifier
     */
    String slice() default "";

    /**
     * Comparisons with zero that should also be treated as constants.
     *
     * @return The zero conditions to expand
     */
    Condition[] expandZeroConditions() default {};

    /**
     * Enables verbose logging while constants are searched.
     *
     * @return Whether debug logging is enabled
     */
    boolean log() default false;

    /**
     * Comparison with zero that can be widened to the corresponding conditional opcode.
     */
    enum Condition {
        /**
         * A comparison against zero using the less-than operator.
         */
        LESS_THAN_ZERO,
        /**
         * A comparison against zero using the less-than-or-equal operator.
         */
        LESS_THAN_OR_EQUAL_TO_ZERO,
        /**
         * A comparison against zero using the greater-than-or-equal operator.
         */
        GREATER_THAN_OR_EQUAL_TO_ZERO,
        /**
         * A comparison against zero using the greater-than operator.
         */
        GREATER_THAN_ZERO
    }

}
