package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration marking a final field of the target class as mutable. The stub is present for source compatibility with
 * the upstream annotation and has no behavior.
 *
 * @deprecated ClassTransform does not support the upstream annotation represented by this stub, so applying it changes
 *     nothing.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Mutable {
}
