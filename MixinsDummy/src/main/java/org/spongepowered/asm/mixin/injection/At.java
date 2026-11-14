package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface At {

    String value();

    String target() default "";

    Desc desc() default @Desc(value = "");

    /**
     * Named arguments for the injection point.<br>
     * Constant value discriminators are supported for {@code CONSTANT}.
     *
     * @return The injection point arguments
     */
    String[] args() default {};

    Shift shift() default Shift.NONE;

    int by() default 0;

    String slice() default "";

    String id() default "";

    int ordinal() default -1;

    int opcode() default -1;

    boolean remap() default true;


    enum Shift {
        NONE,
        BEFORE,
        AFTER,
        BY
    }

}
