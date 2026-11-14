package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ModifyReturnValue {

    String[] method();

    /**
     * The supported targets are {@code RETURN} and {@code TAIL}.
     *
     * @return The return instructions
     */
    At[] at();

    /**
     * An exception will be thrown during the translation process if more than one slice is specified.
     *
     * @return The slice
     */
    Slice[] slice() default {};

    @Deprecated
    boolean remap() default true;

    @Deprecated
    int require() default -1;

    @Deprecated
    int expect() default 1;

    @Deprecated
    int allow() default -1;

    @Deprecated
    int order() default 1000;

}
