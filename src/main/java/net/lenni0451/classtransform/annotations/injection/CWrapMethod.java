package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.operation.COperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wrap a whole method.<br>
 * The transformer method receives the target method arguments, a {@link COperation} and optionally
 * trailing {@link CShared} reference parameters.<br>
 * The operation calls the original method body. It can be called multiple times, with modified
 * arguments or not at all.<br>
 * The return type of the transformer method has to match the return type of the target method.<br>
 * If the target method is static the transformer method has to be static as well.<br>
 * <br>
 * Shared values can be used to share data between multiple wrappers and the original method body.
 * Only reference types from {@code net.lenni0451.classtransform.utils.local} are supported for
 * wrapper parameters.<br>
 * <br>
 * Wrapping a method:<br>
 * <pre>
 * &#64;CWrapMethod(method = "print")
 * public void print(String text, COperation&lt;Void&gt; original) {
 *     original.call(text);
 * }
 * </pre>
 *
 * @see COperation
 * @see CShared
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CWrapMethod {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The minimum number of successful injections across all selected methods.<br>
     * An explicitly specified negative value uses the manager default.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * The expected minimum count, checked only when injection count debugging is enabled.
     *
     * @return The expected injection count
     */
    int expect() default 1;

    /**
     * The maximum count. Values below one are ignored.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * The application order across all method wrappers.<br>
     * Lower values are applied first and therefore become the inner wrappers.
     *
     * @return The injection order
     */
    int order() default 1000;

    /**
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
