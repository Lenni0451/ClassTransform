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
 * Modify a local variable at an injection point.<br>
 * The transformer method must accept the current value and return the new value of the same type.<br>
 * The modified value may be followed by a prefix of the target method arguments.<br>
 * If the target method is static, the transformer method must be static as well.<br>
 * <br>
 * The variable can be selected by {@link #index()}, {@link #ordinal()} or {@link #name()}.<br>
 * If none of them are set exactly one variable of the handler type must be in scope at the injection point.<br>
 * With {@code argsOnly = true} only method arguments are considered.<br>
 * <br>
 * The special targets {@code LOAD} and {@code STORE} modify the variable before it is loaded or after it is stored.<br>
 * <br>
 * Modify a method argument:
 * <pre>
 * &#64;CModifyVariable(method = "print", target = &#64;CTarget("HEAD"), index = 1)
 * public String modifyName(String name) {
 *     return name.toUpperCase();
 * }
 * </pre>
 *
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InjectionType
public @interface CModifyVariable {

    /**
     * The methods that declare the local variable to modify.
     * <p>
     * Selectors name a method and its descriptor, optionally with wildcards to cover several methods, for
     * example {@code run()V} or {@code run*}.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The target expression to modify the value at.<br>
     * This may be omitted when {@link #targets()} is used.
     *
     * @return The target
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget target() default @CTarget("");

    /**
     * Additional target expressions to modify the value at.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] targets() default {};

    /**
     * Descriptor selectors that restrict the methods declaring the modified variable.
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
     * Additional named slices for locating the modified variable, selectable by {@link CTarget#slice()};
     * duplicate ids are not allowed.
     *
     * @return Additional named slices selected by {@link CTarget#slice()}
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice[] slices() default {};

    /**
     * The absolute variable index to modify.
     *
     * @return The variable index or -1
     */
    int index() default -1;

    /**
     * The ordinal of the variable type to modify.
     * A value of {@code -1} disables ordinal matching.
     *
     * @return The variable ordinal or -1
     */
    int ordinal() default -1;

    /**
     * The possible names of the variable to modify.
     *
     * @return The variable names
     */
    String[] name() default {};

    /**
     * Only consider method arguments and not local variables created inside the method.
     *
     * @return If only method arguments should be considered
     */
    boolean argsOnly() default false;

    /**
     * Print all local variables at the injection point instead of failing to resolve the target.
     *
     * @return If the local variables should be printed
     */
    boolean print() default false;

    /**
     * Constraints which must be validated for this injector to succeed.
     *
     * @return The constraints
     */
    String constraints() default "";

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
     * The maximum number of local variable modifications to apply.
     * Values below one remove the limit.
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
     * Whether the variable and target selectors of this injection should be remapped.
     *
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
