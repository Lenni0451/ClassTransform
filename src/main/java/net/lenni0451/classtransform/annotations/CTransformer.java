package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class as a transformer class.<br>
 * If your transformer does not have this annotation, an {@link IllegalStateException} will be thrown when registering it.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CTransformer">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface CTransformer {

    /**
     * The classes to transform.
     *
     * @return The classes to transform
     */
    Class<?>[] value() default {};

    /**
     * The name of the classes to transform.<br>
     * This requires the complete name including the package separated by a dot.<br>
     * Use this if you can't access the class directly.
     *
     * @return The name of the classes to transform
     */
    @AnnotationRemap(RemapType.CLASS)
    String[] name() default {};

}
