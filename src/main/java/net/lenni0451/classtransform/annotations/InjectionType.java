package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.transformer.InjectionPhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that marks an annotation as an injection type and defines its default {@link InjectionPhase}.
 * <p>
 * The phase controls when the injection runs relative to other injection types.
 *
 * @see InjectionPhase
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InjectionType {

    /**
     * The default injection phase used when the annotation does not declare or override a {@code phase} member.
     *
     * @return The default phase of the injection
     */
    InjectionPhase value() default InjectionPhase.REGULAR;

}
