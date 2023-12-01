package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Local {

    @Deprecated
    boolean print() default false;

    int ordinal() default -1;

    int index() default -1;

    /**
     * In MixinExtras the name is an array of possible names.<br>
     * ClassTransform only supports one name.
     */
    String name() default "";

    @Deprecated
    boolean argsOnly() default false;

}
