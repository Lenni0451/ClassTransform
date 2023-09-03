package net.lenni0451.classtransform.mappings.annotation;

import net.lenni0451.classtransform.annotations.injection.CASM;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An internal annotation used to mark fields of ClassTransform annotations to be remapped.<br>
 * You also need to specify this if you create your own transformer annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AnnotationRemap {

    /**
     * The type of the annotation field.<br>
     * e.g. {@link RemapType#CLASS}
     *
     * @return The type
     */
    RemapType value();

    /**
     * The type of action to perform when remapping the annotation field.<br>
     * e.g. {@link FillType#KEEP_EMPTY} when allowing empty values (see {@link CASM} for an example)
     *
     * @return The action type
     */
    FillType fill() default FillType.REPLACE;

    /**
     * Allow a class prefix to be added to <b>short members only</b>.<br>
     * e.g. {@code "Ljava/lang/String;hashCode()I"} or {@code "java/lang/String.hashCode()I"}
     *
     * @return If a class prefix should be allowed
     */
    boolean allowClassPrefix() default false;

}
