package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link Desc} annotations on one element. This compatibility stub exists so the repeated
 * annotation compiles and has no behavior.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Descriptors {

    /**
     * The descriptor annotations that were repeated.
     *
     * @return The repeated descriptors
     */
    Desc[] value();

}
