package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method declaration that replaces a method with the same signature in the target class. This stub lets such code
 * compile and carries no behavior; ClassTransform performs any translation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Overwrite {

    /**
     * Semicolon-separated constraint expression of the form {@code TOKEN(range)} that must pass before the overwrite
     * applies.
     *
     * @return The constraint expression
     */
    String constraints() default "";

    /**
     * Additional target method names tried if the primary name does not resolve.
     *
     * @return The alternative names
     */
    String[] aliases() default {};

    /**
     * Whether the overwritten method name is remapped.
     *
     * @return Whether the name is remapped
     */
    boolean remap() default true;

}
