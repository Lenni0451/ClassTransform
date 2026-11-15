package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration marking a method or field as final in the target class. This compatibility stub represents an upstream
 * annotation that ClassTransform does not translate, so it does nothing.
 *
 * @deprecated The upstream annotation is unsupported here and has no effect; the stub is kept only so existing code
 *     still compiles.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Final {
}
