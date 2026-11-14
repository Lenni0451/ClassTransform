package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.transformer.InjectionPhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark an annotation as an injection with count constraints and ordering.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InjectionType {

    /**
     * @return The default phase of the injection
     */
    InjectionPhase value() default InjectionPhase.REGULAR;

}
