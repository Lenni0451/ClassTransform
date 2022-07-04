package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redirect a specific method/field call
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CRedirect {

    /**
     * The method name and descriptors to inject into<br>
     * e.g. print(Ljava/lang/String;)V
     */
    @AnnotationRemap(RemapType.SHORT_MEMBER)
    String[] method();

    /**
     * The redirect target to use
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget target();

    /**
     * The slice to use
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * The ordinal of the target<br>
     * Use -1 to use all targets
     */
    int ordinal() default -1;

}
