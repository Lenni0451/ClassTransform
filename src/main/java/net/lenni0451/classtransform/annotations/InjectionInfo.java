package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods which have been added by ClassTransform.<br>
 * This provides some extra information if the transformed class is dumped.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface InjectionInfo {

    /**
     * The name of the transformer the method originated from.
     *
     * @return The name of the transformer
     */
    String transformer();

    /**
     * The original name of the method before being renamed.
     *
     * @return The original name of the method
     */
    String originalName();

}
