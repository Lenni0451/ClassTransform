package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyConstant {

    String[] method();

    Slice[] slice() default {};

    Constant[] constant();

    int require() default -1;

    int expect() default 1;

    int allow() default -1;

    int order() default 10000;

    boolean remap() default true;

}
