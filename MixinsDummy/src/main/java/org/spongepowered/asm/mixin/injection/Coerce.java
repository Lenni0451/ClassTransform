package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a handler parameter or handler method return value that is coerced to the expected type. This compatibility
 * stub marks code for the translator and does nothing else.
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Coerce {
}
