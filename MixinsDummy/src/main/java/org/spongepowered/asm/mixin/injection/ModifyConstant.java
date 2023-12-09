package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyConstant {

    String[] method();

    /**
     * An array of slices is not supported. Only the first slice will be used.<br>
     * An exception will be thrown during the translation process if more than one slice is specified.
     */
    Slice[] slice() default {};

    /**
     * An array of constants is not supported. Only the first constant will be used.<br>
     * An exception will be thrown during the translation process if more than one constant is specified.
     */
    Constant[] constant();

    @Deprecated
    int require() default -1;

    @Deprecated
    boolean remap() default true;

}
