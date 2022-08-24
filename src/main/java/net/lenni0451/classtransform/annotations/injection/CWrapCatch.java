package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wrap an entire method in a try-catch block<br>
 * The method with this annotation must have the same return type as the original method<br>
 * Only one parameter is allowed, and it must be of type {@link Throwable}<br>
 * The parameter also declares which exception should be caught
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CWrapCatch {

    /**
     * The name and description of the method to wrap<br>
     * e.g. print(Ljava/lang/String;)V
     */
    @AnnotationRemap(RemapType.SHORT_MEMBER)
    String[] value();

    /**
     * The method owner, name and descriptor of the target method<br>
     * Only method calls are supported<br>
     * e.g. Ljava/io/InputStream;close()V
     */
    @AnnotationRemap(value = RemapType.MEMBER, fill = FillType.KEEP_EMPTY)
    String target() default "";

    /**
     * The ordinal of the target<br>
     * Only used if a target is specified
     */
    int ordinal() default -1;

    /**
     * The slice to use<br>
     * Only used if a target is specified
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

}
