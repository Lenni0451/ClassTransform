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
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The invocation to modify the argument of.
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
     * Descriptor selectors for the target methods.
     *
     * @return The descriptor selectors
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CDesc[] desc() default {};

    /**
     * The argument index to modify.
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
     * The maximum count. Values below one are ignored.
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
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
