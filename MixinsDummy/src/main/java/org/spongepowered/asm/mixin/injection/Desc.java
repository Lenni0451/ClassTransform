package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Descriptors.class)
public @interface Desc {

    String id() default "";

    Class<?> owner() default void.class;

    String value();

    Class<?> ret() default void.class;

    Class<?>[] args() default {};

    Next[] next() default {};

    int min() default 0;

    int max() default Integer.MAX_VALUE;

}
