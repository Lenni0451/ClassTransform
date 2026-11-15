package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Legacy annotation for conditionally executing a wrapped operation.
 * <p>
 * Check the <a href="https://github.com/LlamaLad7/MixinExtras/blob/master/src/main/java/com/llamalad7/mixinextras/injector/WrapWithCondition.java">MixinExtras</a> repository for a reason why this annotation is deprecated.<br>
 * It is recommended to use the {@link com.llamalad7.mixinextras.injector.v2.WrapWithCondition} annotation instead.
 *
 * @deprecated Use the {@link com.llamalad7.mixinextras.injector.v2.WrapWithCondition} annotation instead.
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapWithCondition {

    /**
     * The target methods containing operations to guard with a condition.
     *
     * @return The target method names and descriptors
     */
    String[] method();

    /**
     * The targets of the operations to guard. Every given target is translated.
     * <p>
     * <b>The only supported targets are {@code INVOKE} and field writes.</b>
     *
     * @return The operation targets
     */
    At[] at();

    /**
     * Named slices that the target operations may reference. Every given slice is translated and becomes a
     * named slice in the translated annotation.
     *
     * @return The available slices
     */
    Slice[] slice() default {};

    /**
     * Whether the targets named by this annotation are remapped.
     *
     * @return If the selectors are remapped
     */
    boolean remap() default true;

}
