package com.llamalad7.mixinextras.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link Expression} annotations on one handler method.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator expands the container and converts
 * every contained expression individually.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Expressions {

    /**
     * The repeated expressions.
     *
     * @return The expressions
     */
    Expression[] value();

}
