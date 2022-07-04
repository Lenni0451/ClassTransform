package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class as a transformer class<br>
 * If your transformer does not have this annotation, an {@link IllegalStateException} will be thrown
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface CTransformer {

    /**
     * The classes to inject
     */
    Class<?>[] value() default {};

    /**
     * The name of the classes to inject (Use this if you can't access the classes)
     */
    @AnnotationRemap(RemapType.CLASS)
    String[] name() default {};

}
