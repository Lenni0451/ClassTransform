package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inject into a method using direct ASM<br>
 * You get direct access to the {@link org.objectweb.asm.tree.MethodNode} of the target method
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CASM {

    /**
     * The method name and descriptors to inject into<br>
     * e.g. print(Ljava/lang/String;)V<br>
     * If you want the whole {@link org.objectweb.asm.tree.ClassNode} do not add any targets
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, fill = FillType.KEEP_EMPTY)
    String[] value() default {};

    /**
     * The shift of the CASM injection
     */
    Shift shift() default Shift.TOP;


    enum Shift {
        /**
         * Execute the transformer at the top of the handler chain
         */
        TOP,
        /**
         * Execute the transformer at the bottom of the handler chain
         */
        BOTTOM
    }

}
