package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify a constant value in a method
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CModifyConstant {

    /**
     * The method names and descriptors to inject into<br>
     * e.g. print(Ljava/lang/String;)V
     */
    @AnnotationRemap(RemapType.SHORT_MEMBER)
    String[] method();

    /**
     * The ordinal of the target<br>
     * Use -1 to use all targets
     */
    int ordinal() default -1;

    /**
     * The slice to use
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * If the target is optional or an exception should be thrown if not found
     */
    boolean optional() default false;


    /**
     * Set ACONST_NULL as target
     */
    boolean nullValue() default false;

    /**
     * Set the given int as target
     */
    int intValue() default 0;

    /**
     * Set the given long as target
     */
    long longValue() default 0L;

    /**
     * Set the given float as target
     */
    float floatValue() default 0F;

    /**
     * Set the given double as target
     */
    double doubleValue() default 0D;

    /**
     * Set the given String as target
     */
    String stringValue() default "";

    /**
     * Set the given type as target
     */
    Class<?> typeValue() default Object.class;

}
