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
 * You get direct access to the class/method node of the target.<br>
 * The transformer methods have to be static and return {@code void}.<br>
 * <br>
 * By default, the transformer will be executed at the top of the handler chain.<br>
 * You can change this by setting the {@link #shift()} value to {@link Shift#BOTTOM}.<br>
 * <br>
 * The code inside the method is isolated from the rest of the class and loaded.<br>
 * Fields inside the transformer class are not accessible from the isolated method.<br>
 * Methods accessed from the isolated method are getting copied into the isolated class.<br>
 * Lambdas are not supported and will throw an exception during the isolation process.<br>
 * <br>
 * Injecting into a class:<br>
 * <pre>
 * &#64;CASM
 * public static void inject(ClassNode classNode) {
 *     //Do something with the class node
 * }
 * </pre>
 * Injecting into a method:<br>
 * <pre>
 * &#64;CASM("print")
 * public static void inject(MethodNode methodNode) {
 *     //Do something with the method node
 * }
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CASM {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * To get the class node keep the target empty.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
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
