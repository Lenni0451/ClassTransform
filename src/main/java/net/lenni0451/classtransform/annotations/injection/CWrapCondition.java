package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to wrap a method call or a field with a condition (if statement).<br>
 * The return type of the transformer method must always be a boolean.<br>
 * <p>When wrapping a method call:</p>
 * <ul>
 *     <li>If the wrapped method is not static, the instance of the method owner should be the first argument.</li>
 *     <li>The remaining arguments should be the same as the arguments of the method being wrapped.</li>
 *     <li>The return value of the wrapped method has to be void.</li>
 * </ul>
 * <p>When wrapping a field:</p>
 * <ul>
 *     <li>If the wrapped field is not static, the instance of the field owner should be the first argument.</li>
 *     <li>The second argument should be the new value for the field.</li>
 *     <li>Only field gets are supported (GETFIELD, GETSTATIC).</li>
 * </ul>
 * <br>
 * <p>Example usage:</p>
 * <pre>
 * &#64;CWrapCondition(method = "print", target = &#64;CTarget(value = "INVOKE", target = "..."))
 * public boolean condition(Object instance, String arg) {
 *     // Return true or false based on some condition
 * }
 * </pre>
 * If your target has to be chosen more precisely you can use a {@link #slice()} to narrow down the search.
 *
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CWrapCondition {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The target which should be wrapped with a condition.<br>
     * This can be used to wrap multiple targets at once.
     *
     * @return The target
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] target();

    /**
     * The slice to narrow down the search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

}
