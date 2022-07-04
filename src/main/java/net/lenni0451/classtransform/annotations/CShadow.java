package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create a shadow copy of a method/field/constructor to directly access the original after injection
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface CShadow {

    /**
     * The name of the member to shadow<br>
     * e.g. print(Ljava/lang/String;)V
     */
    @AnnotationRemap(RemapType.SHORT_MEMBER)
    String value() default "";

}
