package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.InjectionType;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.transformer.InjectionPhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wrap an operation and optionally call the next level of the operation chain.<br>
 * The transformer method receives the operation arguments, a {@link COperation} and optionally
 * a prefix of the target method arguments.<br>
 * The operation is only executed if the transformer calls it. It can be called multiple times,
 * with modified arguments or not at all.<br>
 * Supported operations are method invocations, field accesses, object creation and {@code instanceof}.<br>
 * <br>
 * <pre>
 * &#64;CWrapOperation(method = "print", target = &#64;CTarget(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;"))
 * public String wrap(Object instance, COperation&lt;String&gt; original) {
 *     if (this.useOriginal) return original.call(instance);
 *     return "wrapped";
 * }
 * </pre>
 *
 * @see COperation
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@InjectionType(InjectionPhase.LATE)
public @interface CWrapOperation {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The operations to wrap.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] target();

    /**
     * The slice to narrow down the search for the operations.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * @return Additional named slices selected by {@link CTarget#slice()}
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice[] slices() default {};

    /**
     * The minimum number of successful injections across all selected methods.<br>
     * An explicitly specified negative value uses the manager default. Zero allows no matches.
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
     * The application order within the injection phase, across all transformers.<br>
     * Lower values are applied first.
     *
     * @return The injection order
     */
    int order() default 1000;

    /**
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
