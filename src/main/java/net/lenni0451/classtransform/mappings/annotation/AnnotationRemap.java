package net.lenni0451.classtransform.mappings.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AnnotationRemap {

    RemapType value();

    FillType fill() default FillType.REPLACE;

}
