package net.lenni0451.classtransform.annotations.expression;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Describes a local variable which can be referenced from an {@link CExpression}.<br>
 * The discriminators have the same meaning as for {@link net.lenni0451.classtransform.annotations.CLocalVariable}.
 */
@Retention(RetentionPolicy.CLASS)
public @interface CLocalDefinition {

    /**
     * The type of the local variable.
     * {@code void} leaves the type unrestricted.
     *
     * @return The type
     */
    @AnnotationRemap(RemapType.DESCRIPTOR)
    Class<?> type() default void.class;

    /**
     * The name of the local variable in the local variable table.
     *
     * @return The name of the local variable
     */
    String name() default "";

    /**
     * Additional accepted local variable names.
     * The names are merged with {@link #name()} into one set of accepted candidates rather than tried only
     * after it; a local variable matches when its name equals any of them.
     *
     * @return Additional names of the local variable
     */
    String[] names() default {};

    /**
     * The ordinal of the local variable among variables of the same type.
     * A value of {@code -1} disables ordinal matching.
     *
     * @return The ordinal of the local variable
     */
    int ordinal() default -1;

    /**
     * The absolute variable index.
     * A value of {@code -1} disables index matching.
     *
     * @return The local variable index
     */
    int index() default -1;

    /**
     * Whether only method parameters should be considered.
     *
     * @return If only method parameters should be considered
     */
    boolean argsOnly() default false;

    /**
     * Print all available local variables of the target method and abort the injection.
     * The abort is implemented by throwing an exception.
     *
     * @return If the local variables should be printed
     */
    boolean print() default false;

}
