package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.InjectionType;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redirect a specified method call or field access.<br>
 * The following target types are supported:<br>
 * <ul>
 *     <li>INVOKE</li>
 *     <li>FIELD</li>
 *     <li>GETFIELD</li>
 *     <li>PUTFIELD</li>
 *     <li>NEW</li>
 * </ul>
 * <p>When redirecting a method call:</p>
 * <ul>
 *     <li>The transformer method must have the same parameters and return type as the redirected method.</li>
 *     <li>If the redirected method is not static, the instance of the method owner has to be the first parameter of the transformer method.</li>
 * </ul>
 * <p>When redirecting a field access:</p>
 * <ul>
 *     <li>The transformer method must have the same return type as the redirected field.</li>
 *     <li>If the redirected field is not static the instance of the field owner has to be the first parameter of the transformer method.</li>
 *     <li>No other parameters than the non-static instance are allowed.</li>
 * </ul>
 * The transformer method has to be static if the target method/field is static.<br>
 * <br>
 * Redirecting a method call:<br>
 * <pre>
 * &#64;CRedirect(method = "print", target = &#64;CTarget(value = "INVOKE", target = "..."))
 * public String redirect(Object instance, int arg1, String arg2) {
 *     //Do something with the arguments and return the new value
 * }
 * </pre>
 * Redirecting a field access:<br>
 * <pre>
 * &#64;CRedirect(method = "print", target = &#64;CTarget(value = "GETFIELD", target = "..."))
 * public String redirect(Object instance) {
 *     //Do something with the instance and return the new value
 * }
 * </pre>
 * See {@link CTarget} for more information about targets.<br>
 * If your target has to be chosen more precisely you can use a {@link #slice()} to narrow down the search.
 *
 * @see CTarget
 * @see CSlice
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CRedirect">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@InjectionType
public @interface CRedirect {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true, fill = FillType.KEEP_EMPTY)
    String[] method() default {};

    /**
     * The target methods described by their owner, name, return type and arguments.<br>
     * This can be used instead of or in addition to {@link #method()}.
     *
     * @return The target method descriptors
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CDesc[] desc() default {};

    /**
     * Constraints which have to pass for this injection to be applied.
     *
     * @return The constraints
     */
    String constraints() default "";

    /**
     * The target for the redirect.
     *
     * @return The target
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget target();

    /**
     * The slice to narrow down the search for the target.
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
     * The maximum count. Values below one are ignored.<br>
     * The effective maximum is never lower than {@link #require()}.
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
    int order() default 10000;

    /**
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
