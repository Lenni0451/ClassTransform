package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.InjectionCallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replace all {@link InjectionCallback} instances with an {@code Object[]}.<br>
 * This can be used if the {@link InjectionCallback} class is not available in the target environment (e.g. when injecting into java internals).<br>
 * All important checks from the {@link InjectionCallback} class are also included in the replacement.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CReplaceCallback">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface CReplaceCallback {
}
