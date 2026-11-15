package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker on a parameter which captures a local variable of the target method, or declares a local
 * variable for an expression definition.
 * <p>
 * The parameter type identifies the local variable's type and may also be a
 * {@link com.llamalad7.mixinextras.sugar.ref.LocalRef} variant to read and write the variable. This
 * compile-only stub mirrors the MixinExtras annotation so that the MixinsTranslator can translate it.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Local {

    /**
     * Whether to print a table of the available variables so you can determine the right discriminators to use.<br>
     * This will abort the injection.
     *
     * @return If the available variables should be printed
     */
    boolean print() default false;

    /**
     * Selects the local variable by its zero-based ordinal among variables of the same type.
     * <p>
     * The default {@code -1} leaves this discriminator unset.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * Selects the local variable by its slot index in the local variable table.
     * <p>
     * The default {@code -1} leaves this discriminator unset.
     *
     * @return The index
     */
    int index() default -1;

    /**
     * Names of the local variable in the local variable table, tried in order until one matches.
     * <p>
     * An empty array leaves this discriminator unset.
     *
     * @return The names
     */
    String[] name() default {};

    /**
     * Whether only the method's parameters are considered when selecting the local variable.
     * <p>
     * This excludes the {@code this} reference and variables created inside the method body.
     *
     * @return If only parameters should be included
     */
    boolean argsOnly() default false;

    /**
     * The type of the local variable when this annotation is used in a definition.
     * <p>
     * It is ignored for parameters. The default is {@code void.class}.
     *
     * @return The type
     */
    Class<?> type() default void.class;

}
