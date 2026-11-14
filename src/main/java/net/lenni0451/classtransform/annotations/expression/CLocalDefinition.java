package net.lenni0451.classtransform.annotations.expression;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes a local variable which can be referenced from an {@link CExpression}.<br>
 * The discriminators have the same meaning as for {@code CLocalVariable}.
 */
@Retention(RetentionPolicy.CLASS)
public @interface CLocalDefinition {

    /**
     * The type of the local variable.
     *
     * @return The type
     */
    @AnnotationRemap(RemapType.DESCRIPTOR)
    Class<?> type() default void.class;

    /**
     * @return The name of the local variable
     */
    String name() default "";

    /**
     * @return Additional names of the local variable
     */
    String[] names() default {};

    /**
     * @return The ordinal of the local variable
     */
    int ordinal() default -1;

    /**
     * @return The local variable index
     */
    int index() default -1;

    /**
     * @return If only method parameters should be considered
     */
    boolean argsOnly() default false;

    /**
     * Print all available local variables of the target method and abort the injection.
     *
     * @return If the local variables should be printed
     */
    boolean print() default false;

}
