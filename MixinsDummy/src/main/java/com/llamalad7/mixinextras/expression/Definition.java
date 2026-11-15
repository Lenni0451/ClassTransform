package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.sugar.Local;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declaration of a named identifier referenced by expression patterns.
 * <p>
 * This compile-only compatibility stub has no behavior; the translator rewrites each definition to
 * ClassTransform's equivalent definition, including any local variable definitions, before the expression
 * is resolved.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Repeatable(Definitions.class)
public @interface Definition {

    /**
     * The name expression patterns use to reference this definition.
     *
     * @return The definition identifier
     */
    String id();

    /**
     * Method declarations matched by this identifier.
     *
     * @return The method declarations
     */
    String[] method() default {};

    /**
     * Field declarations matched by this identifier.
     *
     * @return The field declarations
     */
    String[] field() default {};

    /**
     * Types matched by this identifier.
     *
     * @return The matched types
     */
    Class<?>[] type() default {};

    /**
     * Local variables matched by this identifier.
     *
     * @return The local variable definitions
     *
     * @see Local
     */
    Local[] local() default {};

    /**
     * Whether the declarations of this definition are remapped.
     *
     * @return If the declarations are remapped
     */
    boolean remap() default true;

}
