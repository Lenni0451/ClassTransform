package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.CArgs;
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
 * Modify multiple arguments of a method invocation.<br>
 * The target must be a method invocation or a constructor invocation selected by {@code INVOKE} or {@code NEW}.<br>
 * The transformer method must return {@code void} and accept a {@link CArgs} bundle,
 * optionally followed by a prefix of the target method arguments.<br>
 * If the target method is static, the transformer method must be static as well.
 * Handlers targeting a constructor before the {@code super()} or {@code this()} call must be static.<br>
 * <br>
 * Modify all arguments:
 * <pre>
 * &#64;CModifyArgs(method = "print", target = &#64;CTarget(value = "INVOKE", target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;"))
 * public static void modify(CArgs args) {
 *     args.set(0, "modified");
 * }
 * </pre>
 *
 * @see CArgs
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InjectionType
public @interface CModifyArgs {

    /**
     * The methods that contain the invocation whose arguments are changed.
     * <p>
     * Each entry combines name and descriptor and may use wildcards, for example
     * {@code accept(Ljava/lang/Object;I)V} or {@code accept*}.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The invocation to modify the arguments of.
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
     * Descriptor selectors for the invocations whose argument list is modified.
     *
     * @return The descriptor selectors
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CDesc[] desc() default {};

    /**
     * The slice to narrow down the search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * Additional named slices around the modified invocation, selectable by {@link CTarget#slice()};
     * the ids are unique per annotation.
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
     * The maximum number of invocations whose argument list may be modified.
     * A value below one means no cap.
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
     * Whether the invocation selector and the target selectors of the modified arguments should be
     * remapped.
     *
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
