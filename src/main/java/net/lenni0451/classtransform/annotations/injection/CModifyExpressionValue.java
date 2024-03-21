package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modify the value of an expression (method call/field access/new object creation).<br>
 * <br>
 * When modifying the value (return value) of a method call, the target method must return a value and not be void.<br>
 * Field values can be modified with field gets ({@code GETFIELD}/{@code GETSTATIC}) and field sets ({@code PUTFIELD}/{@code PUTSTATIC}).<br>
 * New object creations can be modified with the {@code NEW} target.<br>
 * <br>
 * The transformer method must have the modified value type as the only parameter and return type.<br>
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
public @interface CModifyExpressionValue {

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
     * The target expression to modify the value of.
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

}
