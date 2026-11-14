package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

public @interface ModifyExpressionValue {

    String[] method();

    /**
     * The supported targets are {@code INVOKE}, {@code FIELD}, {@code NEW} and {@code CONSTANT}.
     *
     * @return The target expressions
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
