package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inject into a method using direct ASM.<br>
 * You get direct access to the class/method node of the target.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CASM {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * To get the class node keep the target empty.<br>
     * e.g. print(Ljava/lang/String;)V
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, fill = FillType.KEEP_EMPTY)
    String[] value() default {};

    /**
     * The shift of the CASM injection (before or after the other transformer).
     *
     * @return The shift
     */
    Shift shift() default Shift.TOP;


    enum Shift {
        /**
         * Execute the transformer at the top of the handler chain.
         */
        TOP,
        /**
         * Execute the transformer at the bottom of the handler chain.
         */
        BOTTOM
    }

}
