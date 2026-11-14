package net.lenni0451.classtransform.annotations.injection;

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
 * Modify the receiver of a non-static method call or field access.<br>
 * The transformer method must accept the original receiver and return the modified receiver of the same type.<br>
 * The receiver may be followed by the arguments of the target operation and a prefix of the target method arguments.<br>
 * The target operation must be a non-static call ({@code INVOKEVIRTUAL}, {@code INVOKEINTERFACE} or {@code INVOKESPECIAL})
 * or a non-static field access ({@code GETFIELD} or {@code PUTFIELD}).<br>
 * Constructors and static operations are not supported.<br>
 * For {@code INVOKESPECIAL}, the call must be on the transformed class or one of its super types.<br>
 * If the target method is static, the transformer method must be static as well.<br>
 * The original operation is kept in the method and multiple transformers can modify the same receiver in sequence.<br>
 * <br>
 * Modify the receiver of a method call:<br>
 * <pre>
 * &#64;CModifyReceiver(method = "print", target = &#64;CTarget(value = "INVOKE", target = "Ljava/lang/Object;toString()Ljava/lang/String;"))
 * public Object modifyReceiver(Object original) {
 *     return this.replacement;
 * }
 * </pre>
 * Modify the receiver of a field write:<br>
 * <pre>
 * &#64;CModifyReceiver(method = "print", target = &#64;CTarget(value = "FIELD", target = "Ljava/lang/Integer;value:I", opcode = Opcodes.PUTFIELD))
 * public Integer modifyReceiver(Integer original, int value) {
 *     return this.replacement;
 * }
 * </pre>
 *
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@InjectionType
public @interface CModifyReceiver {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The operations to modify the receiver of.<br>
     * Overlapping targets only modify each receiver once per transformer method.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] target();

    /**
     * The slice to narrow down the search for the target operations.
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
    int order() default 1000;

    /**
     * @return If the selectors in this annotation should be remapped
     */
    boolean remap() default true;

}
