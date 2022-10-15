package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyConstant {

    String[] method();

    Slice slice() default @Slice;

    Constant constant();

    @Deprecated
    int require() default -1;

    @Deprecated
    boolean remap() default true;

}
