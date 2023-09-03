package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify a constant value in a method.<br>
 * Supported types:<br>
 * - {@code null}<br>
 * - {@code int}<br>
 * - {@code long}<br>
 * - {@code float}<br>
 * - {@code double}<br>
 * - {@link String}<br>
 * - {@link Class}
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CModifyConstant {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. print(Ljava/lang/String;)V
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The ordinal of target constant.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * The slice of instructions to search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * If the target is optional or an exception should be thrown if not found.
     *
     * @return If the target is optional
     */
    boolean optional() default false;


    /**
     * Set ACONST_NULL as the target.<br>
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
    Class<?> typeValue() default Object.class;

}
