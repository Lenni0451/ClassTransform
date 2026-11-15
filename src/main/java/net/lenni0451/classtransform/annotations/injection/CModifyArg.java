package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.InjectionType;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify a single argument of a method invocation.<br>
 * The target must be a method invocation or a constructor invocation selected by {@code INVOKE} or {@code NEW}.<br>
 * The transformer method must accept the original argument value and return the new value of the same type.<br>
 * When the transformer method has more than one argument, the arguments must match the invocation arguments.<br>
 * If the target method is static, the transformer method must be static as well.<br>
 * <br>
 * Modify an argument:
 * <pre>
 * &#64;CModifyArg(method = "print", target = &#64;CTarget(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"), index = 0)
 * public int modifyValue(int value) {
 *     return value + 1;
 * }
 * </pre>
 *
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InjectionType
public @interface CModifyArg {

    /**
     * The methods whose invocation argument is changed.
     * <p>
     * Selectors use a name or a name with descriptor and support wildcards across several methods, for
     * example {@code accept(Ljava/lang/Object;)V} or {@code accept*}.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The invocation to modify the argument of.
     * This may be omitted when {@link #targets()} is used.
     *
     * @return The target
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget target() default @CTarget("");

    /**
     * Additional invocations to modify the arguments of.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] targets() default {};

    /**
     * Descriptor selectors that narrow the invocations whose argument is changed.
     *
     * @return The descriptor selectors
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CDesc[] desc() default {};

    /**
     * The zero-based argument index to modify, excluding the invocation receiver.
     *
     * @return The argument index or -1 to infer it from the handler type
     */
    int index() default -1;

    /**
     * The slice to narrow down the search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * Additional named slices selectable by {@link CTarget#slice()} when locating the modified
     * invocation; ids have to be unique within the annotation.
     *
     * @return Additional named slices selected by {@link CTarget#slice()}
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice[] slices() default {};

    /**
     * Constraints which must be validated for this injector to succeed.
     *
     * @return The constraints
     */
    String constraints() default "";

    /**
     * The minimum number of successful injections across all selected methods.
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
     * The maximum number of invocations whose argument may be changed.
     * Values below one disable the limit.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * The application order within the injection phase, across all transformers.
     *
     * @return The injection order
     */
    int order() default 1000;

    /**
     * Whether the method, invocation and argument index selectors of this injection should be remapped.
     *
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
