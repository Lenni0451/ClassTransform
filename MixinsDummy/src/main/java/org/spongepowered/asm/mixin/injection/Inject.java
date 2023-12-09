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

    /**
     * An array of slices is not supported. Only the first slice will be used.<br>
     * An exception will be thrown during the translation process if more than one slice is specified.
     */
    Slice[] slice() default @Slice;

    boolean cancellable() default false;

    /**
     * Require is partially supported as it gets remapped to {@code optional = (require <= 0)}.<br>
     * It is still marked as deprecated since it is not fully supported
     */
    @Deprecated
    int require() default -1;

    @Deprecated
    boolean remap() default true;

}
