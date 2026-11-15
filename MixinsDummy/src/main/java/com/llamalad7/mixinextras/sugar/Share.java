package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker on a parameter which shares a value with other injection handlers of the same target
 * method invocation.
 * <p>
 * The parameter may be the value itself or a local reference type, in which case all handlers using
 * the same key observe the same mutable value. This compile-only stub mirrors the MixinExtras
 * annotation so that the MixinsTranslator can translate it.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Share {

    /**
     * Identifies the shared value within its namespace.
     * <p>
     * Handlers using the same id and namespace observe the same value.
     *
     * @return The id for this shared value
     */
    String value();

    /**
     * The namespace for this shared value.<br>
     * If empty, the enclosing mixin class name is used. Values with the same id in different
     * namespaces are independent.
     *
     * @return The namespace
     */
    String namespace() default "";

}
