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
     * The name of the shared variable.
     * Values with the same name, namespace and scope refer to the same variable during one invocation.
     *
     * @return The name of the shared variable
     */
    String value();

    /**
     * The namespace of the shared variable.<br>
     * A non-empty namespace is shared across transformers; if empty, the variable is scoped to the
     * transformer class unless {@link #global()} is set.
     *
     * @return The namespace
     */
    String namespace() default "";

    /**
     * Whether the shared variable should be shared across transformer classes without a namespace.
     * This has no effect when {@link #namespace()} is set.
     *
     * @return If the shared variable should be globally accessible
     */
    boolean global() default false;

}
