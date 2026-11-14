package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.utils.local.CLocalRef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Share a value between multiple methods injecting into the same target method invocation.<br>
 * The annotated parameter can either be the value itself or one of the reference types in
 * {@code net.lenni0451.classtransform.utils.local} (e.g. {@link CLocalRef}).<br>
 * Reference parameters always point to the same object for the same key within one invocation.<br>
 * Shared values are initialized with their default value ({@code 0}, {@code false} or {@code null}).
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CShared">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CShared {

    /**
     * @return The name of the shared variable
     */
    String value();

    /**
     * The namespace of the shared variable.<br>
     * If empty, the shared variable is scoped to the transformer class unless {@link #global()} is set.
     *
     * @return The namespace
     */
    String namespace() default "";

    /**
     * @return If the shared variable should be globally accessible
     */
    boolean global() default false;

}
