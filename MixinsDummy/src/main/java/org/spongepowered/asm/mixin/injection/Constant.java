package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Constant {

    boolean nullValue() default false;

    int intValue() default 0;

    float floatValue() default 0.0F;

    long longValue() default 0L;

    double doubleValue() default 0.0;

    String stringValue() default "";

    Class<?> classValue() default Object.class;

    int ordinal() default -1;

    String slice() default "";

    Condition[] expandZeroConditions() default {};

    boolean log() default false;


    enum Condition {
        LESS_THAN_ZERO,
        LESS_THAN_OR_EQUAL_TO_ZERO,
        GREATER_THAN_OR_EQUAL_TO_ZERO,
        GREATER_THAN_ZERO
    }

}
