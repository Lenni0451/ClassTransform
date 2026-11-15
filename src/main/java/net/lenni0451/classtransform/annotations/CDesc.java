package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A descriptor for matching a target member.<br>
 * Unlike member declarations, all elements are matched explicitly.<br>
 * The owner defaults to the transformed class, the return type defaults to {@code void}
 * and an empty argument list matches members without arguments.
 *
 * @see CDescs
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(CDescs.class)
public @interface CDesc {

    /**
     * An identifier for this descriptor.
     * A {@link CTarget#desc()} or other descriptor selector with an empty value can reference this id
     * instead of restating the descriptor.
     *
     * @return The identifier
     */
    String id() default "";

    /**
     * The owner of the member, or {@code void} to use the transformed class.
     *
     * @return The owner
     */
    @AnnotationRemap(RemapType.DESCRIPTOR)
    Class<?> owner() default void.class;

    /**
     * The name of the member.
     *
     * @return The member name
     */
    String value();

    /**
     * The return type for methods, or the declared type for fields.
     *
     * @return The return or field type
     */
    @AnnotationRemap(RemapType.DESCRIPTOR)
    Class<?> ret() default void.class;

    /**
     * The argument types for methods. Ignored for fields.
     *
     * @return The argument types
     */
    Class<?>[] args() default {};

    /**
     * Lambda implementation methods reachable from this member.
     * Each element selects implementations referenced by {@code invokedynamic} instructions in the previous method.
     *
     * @return The next descriptors
     */
    Next[] next() default {};

    /**
     * The minimum number of matches required.
     * A value of zero imposes no minimum.
     *
     * @return The minimum match count
     */
    int min() default 0;

    /**
     * The maximum number of matches allowed.
     * A value below one disables the upper bound.
     *
     * @return The maximum match count
     */
    int max() default Integer.MAX_VALUE;


    /**
     * A next element of a {@link CDesc} path.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({})
    @interface Next {

        /**
         * The name of the lambda implementation method.
         * An empty name accepts any implementation name.
         *
         * @return The member name
         */
        String name() default "";

        /**
         * The return type of the lambda implementation method.
         *
         * @return The return type of the implementation method
         */
        @AnnotationRemap(RemapType.DESCRIPTOR)
        Class<?> ret() default void.class;

        /**
         * The argument types of the lambda implementation method.
         *
         * @return The argument types
         */
        Class<?>[] args() default {};

        /**
         * The minimum number of implementations required.
         *
         * @return The minimum match count
         */
        int min() default 0;

        /**
         * The maximum number of implementations allowed.
         * A value below one disables the upper bound.
         *
         * @return The maximum match count
         */
        int max() default Integer.MAX_VALUE;

    }

}
