package org.spongepowered.asm.mixin.injection;

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

    org.spongepowered.asm.mixin.injection.callback.LocalCapture locals() default org.spongepowered.asm.mixin.injection.callback.LocalCapture.NO_CAPTURE;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;

    int order() default 1000;

    boolean remap() default true;

}
