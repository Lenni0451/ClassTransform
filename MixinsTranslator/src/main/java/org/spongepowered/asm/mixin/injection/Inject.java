package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    String[] method();

    At[] at();

    Slice slice() default @Slice;

    boolean cancellable() default false;

    @Deprecated
    int require() default -1;

    @Deprecated
    boolean remap() default true;

}
