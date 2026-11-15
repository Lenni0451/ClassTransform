package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Groups injector handlers so their successful injection counts are checked together. This compatibility stub has no
 * behavior; the translator rewrites it to ClassTransform's {@code CGroup}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Group {

    /**
     * Group name; an empty name places the handler in the shared default group.
     *
     * @return The group name
     */
    String name() default "";

    /**
     * Minimum successful injections across the group; -1 uses the default requirement.
     *
     * @return The minimum count or -1
     */
    int min() default -1;

    /**
     * Maximum successful injections across the group; -1 means no limit.
     *
     * @return The maximum count or -1
     */
    int max() default -1;

}
