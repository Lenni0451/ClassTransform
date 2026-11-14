package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Share {

    /**
     * @return The id for this shared value
     */
    String value();

    /**
     * The namespace for this shared value.<br>
     * If empty, the enclosing mixin class name is used.
     *
     * @return The namespace
     */
    String namespace() default "";

}
