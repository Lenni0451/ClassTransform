package org.spongepowered.asm.mixin.injection;

import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    String[] method() default {};

    Desc[] target() default {};

    At[] at();

    Slice[] slice() default @Slice;

    String constraints() default "";

    boolean cancellable() default false;

    LocalCapture locals() default LocalCapture.NO_CAPTURE;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;

    int order() default 1000;

    boolean remap() default true;

}
