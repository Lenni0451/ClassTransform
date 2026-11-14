package net.lenni0451.classtransform.annotations.expression;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.mappings.dynamic.DefinitionRemapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define identifiers used by {@link CExpression} annotations.
 *
 * @see CExpression
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Repeatable(CDefinitions.class)
public @interface CDefinition {

    /**
     * The identifier to define.
     *
     * @return The identifier
     */
    String id();

    /**
     * Method declarations this identifier matches.<br>
     * Supports the same formats as {@code @CTarget(value = "INVOKE")}.
     *
     * @return The method declarations
     */
    @AnnotationRemap(value = RemapType.DYNAMIC, dynamicRemapper = DefinitionRemapper.class)
    String[] method() default {};

    /**
     * Field declarations this identifier matches.<br>
     * Supports the same formats as {@code @CTarget(value = "FIELD")}.
     *
     * @return The field declarations
     */
    @AnnotationRemap(value = RemapType.DYNAMIC, dynamicRemapper = DefinitionRemapper.class)
    String[] field() default {};

    /**
     * Types this identifier matches.
     *
     * @return The types
     */
    @AnnotationRemap(RemapType.DESCRIPTOR)
    Class<?>[] type() default {};

    /**
     * Local variables this identifier matches.
     *
     * @return The local variable definitions
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CLocalDefinition[] local() default {};

    /**
     * @return If the declarations should be remapped
     */
    boolean remap() default true;

}
