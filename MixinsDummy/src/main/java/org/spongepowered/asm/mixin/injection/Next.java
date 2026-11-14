package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Next {

    String name() default "";

    Class<?> ret() default void.class;

    Class<?>[] args() default {};

    int min() default 0;

    int max() default Integer.MAX_VALUE;

}
