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

    @Deprecated
    Class<?> classValue() default Object.class;

}
