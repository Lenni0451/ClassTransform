package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration ensuring that a member or nested type never overwrites a matching member of the target. The member is
 * still added to the target and may be renamed or discarded. This compatibility stub neither remaps nor transforms
 * anything.
 *
 * @deprecated The represented upstream annotation is not supported by ClassTransform; the marker has no effect and
 *     exists only for source compatibility.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Unique {
}
