package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a target member by owner, name, return type and arguments. This compatibility stub only holds the
 * descriptor; ClassTransform translates it into its own selector.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Descriptors.class)
public @interface Desc {

    /**
     * Identifier for this descriptor.
     *
     * @return The descriptor identifier
     */
    String id() default "";

    /**
     * Declaring class of the member; {@code void} means the transformed class.
     *
     * @return The owner class
     */
    Class<?> owner() default void.class;

    /**
     * Name of the member.
     *
     * @return The member name
     */
    String value();

    /**
     * Return type of a method or declared type of a field; {@code void} when none is given.
     *
     * @return The return or field type
     */
    Class<?> ret() default void.class;

    /**
     * Argument types of a method; ignored for fields.
     *
     * @return The argument types
     */
    Class<?>[] args() default {};

    /**
     * Further descriptors describing member accesses relative to this one.
     *
     * @return The next descriptors
     */
    Next[] next() default {};

    /**
     * Minimum number of matches this descriptor must find.
     *
     * @return The minimum match count
     */
    int min() default 0;

    /**
     * Maximum number of matches this descriptor may find.
     *
     * @return The maximum match count
     */
    int max() default Integer.MAX_VALUE;

}
