package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override a method in a class.<br>
 * The transformer method must have the same parameters and return type as the overridden method.<br>
 * The access of the transformer method has to be higher or equal to the overridden method (private < package private < protected < public).<br>
 * If the overridden method is static, the transformer method has to be static as well.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface COverride {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. print(Ljava/lang/String;)V
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(RemapType.SHORT_MEMBER)
    String[] value() default {};

}
