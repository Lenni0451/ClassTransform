package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a slice of bytecode to make choosing the injection target easier.
 * <p>
 * Both boundaries are inclusive; an empty boundary extends to the start or end of the method.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CSlice">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface CSlice {

    /**
     * The id used to select this slice from {@link CTarget#slice()} or
     * {@link net.lenni0451.classtransform.annotations.injection.CModifyConstant#sliceId()}.
     * An empty id declares the default slice of the injection annotation.
     *
     * @return The id used to select this slice
     */
    String id() default "";

    /**
     * The start of the slice.<br>
     * The target must match only a single instruction unless {@link CTarget#selector()} selects FIRST or LAST.
     *
     * @return The start of the slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget from() default @CTarget("");

    /**
     * The end of the slice.<br>
     * The target must match only a single instruction unless {@link CTarget#selector()} selects FIRST or LAST.
     *
     * @return The end of the slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget to() default @CTarget("");

}
