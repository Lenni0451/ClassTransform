package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.InjectionType;
import net.lenni0451.classtransform.transformer.InjectionPhase;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to wrap a method call or a field with a condition (if statement).<br>
 * The return type of the transformer method must always be a boolean.<br>
 * The operation arguments may be followed by a prefix of the enclosing method arguments.<br>
 * A transformer method without arguments is also supported.<br>
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
 *     <li>Only field sets are supported (PUTFIELD, PUTSTATIC).</li>
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
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CWrapCondition">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@InjectionType(InjectionPhase.CONDITION)
public @interface CWrapCondition {

    /**
     * The methods containing the operation wrapped by the condition.
     * <p>
     * Selectors use the combined name and descriptor form and can contain wildcards, for example
     * {@code test()Z} or {@code test*}.
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

    /**
     * Additional named slices for the wrapped condition, selectable by {@link CTarget#slice()};
     * each id may only be declared once.
     *
     * @return Additional named slices selected by {@link CTarget#slice()}
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice[] slices() default {};

    /**
     * The minimum number of successful injections across all selected methods.<br>
     * An explicitly specified negative value uses the manager default. Zero allows no matches.<br>
     * If omitted, the existing mandatory-target and optional-target behavior is retained.
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
     * The maximum number of wrapped operations that may receive the condition.
     * A value below one leaves the count unrestricted.
     * The bound is never below {@link #require()}.
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
     * Whether the condition and target selectors of this injection should be remapped.
     *
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

    /**
     * Conditions normally run after regular injections and expression modifiers.<br>
     * The regular phase is available for compatibility with early condition injectors.
     *
     * @return The injection phase
     */
    InjectionPhase phase() default InjectionPhase.CONDITION;

}
