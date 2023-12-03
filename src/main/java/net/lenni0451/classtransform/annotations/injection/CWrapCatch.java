package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wrap an entire method or a single instruction in a try-catch block.<br>
 * The transformer method must have the same return type as the target method.<br>
 * The transformer method requires an object extending {@link Throwable} as the only parameter.<br>
 * The parameter type is the type of the exception caught by the try-catch block.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CWrapCatch {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] value();

    /**
     * The method owner, name and descriptor of the target instruction to wrap.<br>
     * Only method calls are supported.<br>
     * If this is not specified the entire method will be wrapped.<br>
     * e.g. {@code Ljava/io/InputStream;close()V}
     *
     * @return The method owner, name and descriptor
     */
    @AnnotationRemap(value = RemapType.MEMBER, fill = FillType.KEEP_EMPTY)
    String target() default "";

    /**
     * The ordinal of the target instruction.<br>
     * Ignored if the entire method should be wrapped.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * The slice of instructions to search for the target.<br>
     * Ignored if the entire method should be wrapped.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

}
