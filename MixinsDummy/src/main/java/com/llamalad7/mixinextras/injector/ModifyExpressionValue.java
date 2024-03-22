package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

public @interface ModifyExpressionValue {

    String[] method();

    /**
     * An array of targets is not supported. Only the first target will be used.<br>
     * An exception will be thrown during the translation process if more than one target is specified.<br>
     * <b>The only supported targets are {@code INVOKE}, {@code FIELD} and {@code NEW}.</b>
     */
    At[] at();

    /**
     * An array of slices is not supported. Only the first slice will be used.<br>
     * An exception will be thrown during the translation process if more than one slice is specified.
     */
    Slice[] slice() default {};

    @Deprecated
    boolean remap() default true;

}
