package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify the receiver of a non-static method call or field access.<br>
 * The handler receives the original receiver, the arguments of the operation and optionally
 * a prefix of the enclosing method arguments.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyReceiver {

    String[] method();

    /**
     * The supported targets are {@code INVOKE} and {@code FIELD}.
     *
     * @return The operations to modify
     */
    At[] at();

    /**
     * @return The named slices available to injection points
     */
    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;

    int order() default 1000;

}
