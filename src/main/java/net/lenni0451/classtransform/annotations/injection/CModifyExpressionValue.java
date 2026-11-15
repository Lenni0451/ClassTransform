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
 * Modify the value of an expression (method call/field access/new object creation/constant).<br>
 * <br>
 * When modifying the value (return value) of a method call, the target method must return a value and not be void.<br>
 * Field values can be modified with field gets ({@code GETFIELD}/{@code GETSTATIC}) and field sets ({@code PUTFIELD}/{@code PUTSTATIC}).<br>
 * New object creations can be modified with the {@code NEW} target.<br>
 * Constants can be modified with the {@code CONSTANT} target.<br>
 * <br>
 * The transformer method must accept the modified value and return a value of the same type.<br>
 * The modified value may be followed by a prefix of the target method arguments.<br>
 * If the transformer target method is static, the transformer method must be static as well.<br>
 * <br>
 * Modify the return value of a method call:<br>
 * <pre>
 * &#64;CModifyExpressionValue(method = "print", target = &#64;CTarget(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;"))
 * public String modifyValue(String value) {
 *     return value.toUpperCase();
 * }
 * </pre>
 * Modify the value of a field access:<br>
 * <pre>
 * &#64;CModifyExpressionValue(method = "print", target = &#64;CTarget(value = "GETFIELD", target = "Ljava/lang/Integer;MAX_VALUE:I"))
 * public int modifyValue(int value) {
 *     return Integer.MIN_VALUE;
 * }
 * </pre>
 * Modify the value of a new object creation:<br>
 * <pre>
 * &#64;CModifyExpressionValue(method = "print", target = &#64;CTarget(value = "NEW", target = "java/lang/String"))
 * public String modifyValue(String value) {
 *     return value.toUpperCase();
 * }
 * </pre>
 *
 * @see CTarget
 * @see CSlice
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CModifyExpressionValue">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@InjectionType(InjectionPhase.EXPRESSION)
public @interface CModifyExpressionValue {

    /**
     * The methods whose expression values are modified.
     * <p>
     * Name and descriptor are combined in one entry, and wildcards select several methods, for example
     * {@code compute(Ljava/lang/String;)I} or {@code compute*}.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The target expression to modify the value of.<br>
     * This may be omitted when {@link #targets()} is used.
     *
     * @return The target
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget target() default @CTarget("");

    /**
     * Additional target expressions to modify the value of.<br>
     * This can be used instead of or together with {@link #target()}.<br>
     * Overlapping targets only modify each expression once per transformer method.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] targets() default {};

    /**
     * The slice to narrow down the search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * Additional named slices for the expression search, selectable by {@link CTarget#slice()};
     * slice ids must not repeat.
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
     * The maximum number of expression results that may be replaced.
     * A value below one means no explicit limit.
     * The effective maximum stays at least {@link #require()}.
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
     * Whether the expression and target selectors of this injection should be remapped.
     *
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
