package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {

    String[] method() default {};

    Desc[] target() default {};

    At at();

    Slice slice() default @Slice;

    String constraints() default "";

    int require() default -1;

    int expect() default 1;

    int allow() default -1;

    int order() default 10000;

    boolean remap() default true;

}
