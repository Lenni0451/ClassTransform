package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes one further member in a {@link Desc} path. This compatibility stub is a plain data holder with no
 * behavior.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Next {

    /**
     * Name of the next member; an empty value applies no name filter.
     *
     * @return The member name
     */
    String name() default "";

    /**
     * Return or field type of the next member.
     *
     * @return The return or field type
     */
    Class<?> ret() default void.class;

    /**
     * Argument types of the next member if it is a method.
     *
     * @return The argument types
     */
    Class<?>[] args() default {};

    /**
     * Minimum number of matches for this path element.
     *
     * @return The minimum match count
     */
    int min() default 0;

    /**
     * Maximum number of matches for this path element.
     *
     * @return The maximum match count
     */
    int max() default Integer.MAX_VALUE;

}
