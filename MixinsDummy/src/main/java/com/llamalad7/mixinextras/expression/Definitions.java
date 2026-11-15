package com.llamalad7.mixinextras.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link Definition} annotations on one handler method.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator expands the container so that every
 * contained definition is converted on its own.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Definitions {

    /**
     * The repeated definitions.
     *
     * @return The definitions
     */
    Definition[] value();

}
