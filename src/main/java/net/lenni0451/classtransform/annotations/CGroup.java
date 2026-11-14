package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Aggregate the minimum and maximum injection counts of multiple handler methods.<br>
 * Handlers with the same group name share their successful injection count.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CGroup {

    /**
     * The group name. Handlers without a name share the {@code default} group.
     *
     * @return The group name
     */
    String name() default "";

    /**
     * The minimum number of successful injections across the group.
     *
     * @return The minimum count or -1 for the default
     */
    int min() default -1;

    /**
     * The maximum number of successful injections across the group.
     *
     * @return The maximum count or -1 for no limit
     */
    int max() default -1;

}
