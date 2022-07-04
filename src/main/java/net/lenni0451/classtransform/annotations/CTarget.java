package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the target of the class transformation
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface CTarget {

    /**
     * The type of the target
     */
    String value();

    /**
     * The target of the type<br>
     * e.g. type=INVOKE, target=Ljava/lang/Object;hashCode()I
     */
    @AnnotationRemap(RemapType.MEMBER)
    String target() default "";

    /**
     * The shift for injecting<br>
     * BEFORE/AFTER the target<br>
     * This does not work with all targets
     */
    Shift shift() default Shift.AFTER;

    /**
     * The ordinal of the target<br>
     * This does not work with all targets
     */
    int ordinal() default -1;


    enum Shift {
        /**
         * Inject the code before the target
         */
        BEFORE,
        /**
         * Inject the code after the target
         */
        AFTER
    }

}
