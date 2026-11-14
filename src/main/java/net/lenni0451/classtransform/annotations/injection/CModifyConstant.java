package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.InjectionType;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify a constant value in a method.<br>
 * The target constant must exactly match the given value.<br>
 * If the target is not found an exception will be thrown unless {@link #optional()} is set to {@code true}.<br>
 * The following types are supported:<br>
 * <ul>
 *     <li>{@link #nullValue()}</li>
 *     <li>{@link #intValue()}</li>
 *     <li>{@link #longValue()}</li>
 *     <li>{@link #floatValue()}</li>
 *     <li>{@link #doubleValue()}</li>
 *     <li>{@link #stringValue()}</li>
 *     <li>{@link #typeValue()}</li>
 * </ul>
 * The transformer method has to return the same type as the target constant and needs to be static if the target method is static.<br>
 * The transformer method can optionally take the same type as the target constant as the first argument (except for {@link #nullValue()}).
 * This argument will be the original value of the constant.<br>
 * With {@link #expandZeroConditions()} comparisons with zero can be expanded to conditional opcodes.<br>
 * <br>
 * Modifying a constant in a method:<br>
 * <pre>
 * &#64;CModifyConstant(method = "print", stringValue = "test")
 * public String modifyConstant(String original) {
 *     //Do something with the original value and return the new value
 *     return original + "!";
 * }
 * </pre>
 * If your target has to be chosen more precisely you can use a {@link #slice()} to narrow down the search.
 *
 * @see CSlice
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CModifyConstant">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InjectionType
public @interface CModifyConstant {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * Multiple constant selectors for one transformer method.<br>
     * If this is set, the flattened constant members on this annotation must not be used.
     *
     * @return The constant selectors
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CConstant[] constants() default {};

    /**
     * Explicit injection targets.<br>
     * This can be used to select constants with an {@code EXPRESSION} target.
     *
     * @return The injection targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] targets() default {};

    /**
     * The ordinal of target constant.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * Expand comparisons with zero to the matching conditional opcodes.<br>
     * The transformer method must return an int and take the original int constant as argument.
     *
     * @return The zero conditions to expand
     */
    Condition[] expandZeroConditions() default {};

    /**
     * Enable verbose debug logging when searching for constants.
     *
     * @return If debug logging should be enabled
     */
    boolean log() default false;

    /**
     * The slice to narrow down the search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * @return Additional named slices
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice[] slices() default {};

    /**
     * @return The id of the slice to select
     */
    String sliceId() default "";

    /**
     * The minimum number of successful injections across all selected methods.<br>
     * An explicitly specified negative value uses the manager default. Zero allows no matches.<br>
     * If omitted, the existing mandatory-target and optional-target behavior is retained.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * The expected minimum count, checked only when injection count debugging is enabled.
     *
     * @return The expected injection count
     */
    int expect() default 1;

    /**
     * The maximum count. Values below one are ignored.<br>
     * The effective maximum is never lower than {@link #require()}.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * The application order within the injection phase, across all transformers.<br>
     * Lower values are applied first.
     *
     * @return The injection order
     */
    int order() default 10000;

    /**
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

    /**
     * Make this injection optional.<br>
     * If the target is not found an exception will be thrown unless this is set to {@code true}.
     *
     * @return If the target is optional
     */
    boolean optional() default false;


    /**
     * Set {@code ACONST_NULL} as the target.<br>
     * The transformer method must return any object and does not take any arguments.<br>
     * The return value type is not verified so make sure to return the correct type.
     *
     * @return If ACONST_NULL should be modified
     */
    boolean nullValue() default false;

    /**
     * Set the given int as the target.<br>
     * The transformer method must return an int and can optionally take an int as the first argument.
     *
     * @return The int constant to modify
     */
    int intValue() default 0;

    /**
     * Set the given long as the target.<br>
     * The transformer method must return a long and can optionally take a long as the first argument.
     *
     * @return The long constant to modify
     */
    long longValue() default 0L;

    /**
     * Set the given float as the target.<br>
     * The transformer method must return a float and can optionally take a float as the first argument.
     *
     * @return The float constant to modify
     */
    float floatValue() default 0F;

    /**
     * Set the given double as the target.<br>
     * The transformer method must return a double and can optionally take a double as the first argument.
     *
     * @return The double constant to modify
     */
    double doubleValue() default 0D;

    /**
     * Set the given String as the target.<br>
     * The transformer method must return a String and can optionally take a String as the first argument.
     *
     * @return The String constant to modify
     */
    String stringValue() default "";

    /**
     * Set the given class as the target.<br>
     * The transformer method must return a class and can optionally take a class as the first argument.
     *
     * @return The class constant to modify
     */
    @AnnotationRemap(RemapType.DESCRIPTOR)
    Class<?> typeValue() default Object.class;


    /**
     * Comparison conditions with zero which can be expanded by {@link #expandZeroConditions()}.
     */
    enum Condition {
        /**
         * Match {@code x < 0} and {@code x >= 0}.
         */
        LESS_THAN_ZERO,
        /**
         * Match {@code x <= 0} and {@code x > 0}.
         */
        LESS_THAN_OR_EQUAL_TO_ZERO,
        /**
         * Match {@code x >= 0} and {@code x < 0}.
         */
        GREATER_THAN_OR_EQUAL_TO_ZERO,
        /**
         * Match {@code x > 0} and {@code x <= 0}.
         */
        GREATER_THAN_ZERO
    }


    /**
     * A single constant selector for {@link #constants()}.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({})
    @interface CConstant {

        /**
         * @return If {@code ACONST_NULL} should be modified
         */
        boolean nullValue() default false;

        /**
         * @return The int constant to modify
         */
        int intValue() default 0;

        /**
         * @return The long constant to modify
         */
        long longValue() default 0L;

        /**
         * @return The float constant to modify
         */
        float floatValue() default 0F;

        /**
         * @return The double constant to modify
         */
        double doubleValue() default 0D;

        /**
         * @return The String constant to modify
         */
        String stringValue() default "";

        /**
         * @return The class constant to modify
         */
        @AnnotationRemap(RemapType.DESCRIPTOR)
        Class<?> typeValue() default Object.class;

        /**
         * @return The ordinal of the target constant
         */
        int ordinal() default -1;

        /**
         * @return The id of the slice to use
         */
        String slice() default "";

        /**
         * @return The zero conditions to expand
         */
        Condition[] expandZeroConditions() default {};

        /**
         * @return If debug logging should be enabled
         */
        boolean log() default false;

    }

}
