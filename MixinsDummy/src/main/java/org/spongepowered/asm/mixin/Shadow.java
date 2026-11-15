package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field or method declaration that shadows a member of the target class. This is a compile-time compatibility stub
 * only, with no runtime behavior.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Shadow {

    /**
     * Prefix removed from shadow method names before the target member name is resolved.
     *
     * @return The shadow prefix
     */
    String prefix() default "shadow$";

    /**
     * Alternative target member names tried when the primary name is not found.
     *
     * @return The alternative names
     */
    String[] aliases() default {};

    /**
     * Whether the shadowed member is remapped.
     *
     * @return Whether the member is remapped
     */
    boolean remap() default true;

}
