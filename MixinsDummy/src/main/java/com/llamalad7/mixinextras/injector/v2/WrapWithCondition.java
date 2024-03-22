package com.llamalad7.mixinextras.injector.v2;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapWithCondition {

    String[] method();

    /**
     * An array of targets is not supported. Only the first target will be used.<br>
     * An exception will be thrown during the translation process if more than one target is specified.<br>
     * <b>The only supported targets are {@code INVOKE} and {@code FIELD}.</b>
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
