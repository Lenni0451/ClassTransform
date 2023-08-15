package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect {

    String[] method();

    At at();

    Slice slice() default @Slice;

    /**
     * Require is partially supported as it gets remapped to {@code optional = (require <= 0)}.<br>
     * It is still marked as deprecated since it is not fully supported
     */
    @Deprecated
    int require() default -1;

}
