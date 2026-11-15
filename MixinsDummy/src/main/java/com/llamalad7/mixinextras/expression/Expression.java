package com.llamalad7.mixinextras.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Expression pattern attached to an injection handler.
 * <p>
 * This compile-only compatibility stub has no behavior; the Mixin translator turns the annotation into
 * ClassTransform's expression annotation so that an injection point can target the matched expression.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Expressions.class)
public @interface Expression {

    /**
     * The expression patterns to match.
     *
     * @return The expression patterns
     */
    String[] value();

    /**
     * The identifier an {@code @At("MIXINEXTRAS:EXPRESSION")} point uses to select this expression.
     * <p>
     * An empty identifier leaves the expression unnamed.
     *
     * @return The expression identifier
     */
    String id() default "";

}
