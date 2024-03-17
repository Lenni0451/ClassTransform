package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wrap an entire method or a single method call in a try-catch block.<br>
 * The transformer method must have the caught exception as the only parameter.<br>
 * <p>When wrapping an entire method:</p>
 * <ul>
 *     <li>The transformer method must have the same return type as the wrapped method.</li>
 * </ul>
 * <p>When wrapping a method call instruction:</p>
 * <ul>
 *     <li>The transformer method must have the same return type as the wrapped method.</li>
 * </ul>
 * The transformer method has to be static if the target method is static.<br>
 * <br>
 * Wrapping an entire method:<br>
 * <pre>
 * &#64;CWrapCatch()
 * public String catchException(Exception e) {
 *      //Rethrow the exception or return a default value
 * }
 * </pre>
 * Wrapping a method call instruction:<br>
 * <pre>
 * &#64;CWrapCatch(target = "...")
 * public String catchException(Exception e) {
 *      //Rethrow the exception or return a default value
 * }
 * </pre>
 * If your target has to be chosen more precisely you can use a {@link #slice()} to narrow down the search.
 *
 * @see CSlice
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CWrapCatch">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CWrapCatch {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] value();

    /**
     * The method owner, name and descriptor of the target method call instruction to wrap.<br>
     * If this is not specified the entire method will be wrapped.<br>
     * e.g. {@code Ljava/io/InputStream;close()V}
     *
     * @return The method owner, name and descriptor
     */
    @AnnotationRemap(value = RemapType.MEMBER, fill = FillType.KEEP_EMPTY)
    String target() default "";

    /**
     * The ordinal of target method call instruction.<br>
     * Ignored if the entire method should be wrapped.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * The slice to narrow down the search for the target.<br>
     * Ignored if the entire method should be wrapped.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

}
