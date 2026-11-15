package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a region of a method between two injection points. This compatibility stub holds the boundaries and has no
 * behavior.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Slice {

    /**
     * Identifier used by injections to select this slice.
     *
     * @return The slice identifier
     */
    String id() default "";

    /**
     * Start boundary of the slice; defaults to {@code HEAD}.
     *
     * @return The start boundary
     */
    At from() default @At("HEAD");

    /**
     * End boundary of the slice; defaults to {@code TAIL}.
     *
     * @return The end boundary
     */
    At to() default @At("TAIL");

}
