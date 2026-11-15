package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A mixin declaration naming the target classes it applies to. This compatibility stub exists so code referencing the
 * upstream annotation compiles; it has no behavior of its own.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Mixin {

    /**
     * The classes this mixin is applied to.
     *
     * @return The target classes
     */
    Class<?>[] value() default {};

    /**
     * Fully qualified target class names, for classes that cannot be referenced directly.
     *
     * @return The target class names
     */
    String[] targets() default {};

    /**
     * Ordering weight among mixins; higher values are applied first.
     *
     * @return The priority
     */
    int priority() default 1000;

    /**
     * Whether the mixin's member references are remapped.
     *
     * @return Whether references are remapped
     */
    boolean remap() default true;

}
