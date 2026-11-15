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
 * When using the flattened members or {@link #constants()}, at most one constant member may be set per selector;
 * if none is set, zero condition expansion must imply an {@code int 0} selector.
 * {@link #targets()} is an alternative and cannot be combined with those members.
 * <p>
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
     * The methods whose constant loads are replaced.
     * <p>
     * Entries use the combined name and descriptor form and support wildcards, for example
     * {@code load(I)I} or {@code load*}.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * Multiple constant selectors for one transformer method.<br>
     * If this is set, the flattened constant members on this annotation must not be used
     * and each element may set at most one constant value member; if none is set, its
     * {@link CConstant#expandZeroConditions()} must imply an {@code int 0} selector.
     *
     * @return The constant selectors
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CConstant[] constants() default {};

    /**
     * Explicit injection targets.<br>
     * This can be used to select constants with an {@code EXPRESSION} target.
     * Explicit targets cannot be combined with flattened constant members or {@link #constants()}.
     *
     * @return The injection targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] targets() default {};

    /**
     * The ordinal of the target constant among all matches.
     * A value of {@code -1} selects all matches.
     * This is ignored when {@link #constants()} is used.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * Expand comparisons with zero to the matching conditional opcodes.<br>
     * The transformer method must return an int and take the original int constant as argument.
     * If no constant member is set, this implies an {@code int 0} selector.
     * This is ignored when {@link #constants()} or {@link #targets()} is used.
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
     * Additional named slices selectable by {@link CTarget#slice()} or {@link #sliceId()}, for example to
     * restrict the flattened constant selector; every id must be unique within this annotation.
     *
     * @return Additional named slices
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice[] slices() default {};

    /**
     * The id of the slice used by the flattened constant selector.
     * This is only used when neither {@link #constants()} nor {@link #targets()} is set.
     * An empty id uses the default slice.
     *
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
     * The maximum number of matched constants that may be modified.
     * Values below one are treated as unlimited.
     * The cap never drops below {@link #require()}.
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
     * Whether the method, constant and nested target selectors of this annotation should be remapped.
     *
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
     * At most one constant value member may be set; if none is set,
     * {@link #expandZeroConditions()} must imply an {@code int 0} selector.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({})
    @interface CConstant {

        /**
         * Select {@code ACONST_NULL} as the target constant.
         * The transformer method must return an object type and take no arguments.
         *
         * @return If {@code ACONST_NULL} should be modified
         */
        boolean nullValue() default false;

        /**
         * Select the given int constant.
         * The transformer method must return an int and can optionally take an int as the first argument.
         *
         * @return The int constant to modify
         */
        int intValue() default 0;

        /**
         * Select the given long constant.
         * The transformer method must return a long and can optionally take a long as the first argument.
         *
         * @return The long constant to modify
         */
        long longValue() default 0L;

        /**
         * Select the given float constant.
         * The transformer method must return a float and can optionally take a float as the first argument.
         *
         * @return The float constant to modify
         */
        float floatValue() default 0F;

        /**
         * Select the given double constant.
         * The transformer method must return a double and can optionally take a double as the first argument.
         *
         * @return The double constant to modify
         */
        double doubleValue() default 0D;

        /**
         * Select the given String constant.
         * The transformer method must return a String and can optionally take a String as the first argument.
         *
         * @return The String constant to modify
         */
        String stringValue() default "";

        /**
         * Select the given class constant.
         * The transformer method must return a class and can optionally take a class as the first argument.
         *
         * @return The class constant to modify
         */
        @AnnotationRemap(RemapType.DESCRIPTOR)
        Class<?> typeValue() default Object.class;

        /**
         * The ordinal of the target constant among all matches.
         * A value of {@code -1} selects all matches.
         *
         * @return The ordinal of the target constant
         */
        int ordinal() default -1;

        /**
         * The id of the named slice to use for this selector.
         * An empty id uses the default slice.
         *
         * @return The id of the slice to use
         */
        String slice() default "";

        /**
         * The comparisons with zero to expand for this selector.
         *
         * @return The zero conditions to expand
         */
        Condition[] expandZeroConditions() default {};

        /**
         * Whether verbose debug logging should be enabled while searching for this constant.
         *
         * @return If debug logging should be enabled
         */
        boolean log() default false;

    }

}
