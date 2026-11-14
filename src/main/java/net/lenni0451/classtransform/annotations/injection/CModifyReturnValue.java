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
 * Modify the value returned by a method.<br>
 * The transformer method must accept the original return value and return the modified value of the same type.<br>
 * The original value may be followed by a prefix of the target method arguments.<br>
 * The target method must not return void.<br>
 * If the target method is static, the transformer method must be static as well.<br>
 * Multiple transformers can modify the same return value in sequence.<br>
 * <br>
 * <pre>
 * &#64;CModifyReturnValue(method = "getName", target = &#64;CTarget("RETURN"))
 * public String modifyName(String original) {
 *     return original.toUpperCase();
 * }
 * </pre>
 *
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CModifyReturnValue {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code getName()Ljava/lang/String;} or {@code get*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The return instructions to modify.<br>
     * Use {@code RETURN} for all returns or {@code TAIL} for the last return.<br>
     * Overlapping targets only modify each return once per transformer method.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] target() default @CTarget("RETURN");

    /**
     * The slice to narrow down the search for the return instructions.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

}
