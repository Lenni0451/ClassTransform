package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
     * The index of the local variable by type.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * The LVT index of the local variable.
     *
     * @return The index
     */
    int index() default -1;

    /**
     * Names of the local variable.
     *
     * @return The names
     */
    String[] name() default {};

    /**
     * Whether only the method's parameters should be included.
     *
     * @return If only parameters should be included
     */
    boolean argsOnly() default false;

    /**
     * Specifies the type of this local.<br>
     * Only for use in definitions.
     *
     * @return The type
     */
    Class<?> type() default void.class;

}
