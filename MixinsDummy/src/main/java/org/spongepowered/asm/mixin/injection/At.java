package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface At {

    String value();

    String target() default "";

    Shift shift() default Shift.AFTER;

    int ordinal() default -1;

    @Deprecated
    boolean remap() default true;


    enum Shift {
        BEFORE,
        AFTER
    }

}
