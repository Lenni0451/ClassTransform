package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inline an injection method into the target method.<br>
 * This makes injecting into already loaded classes possible.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CInline">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface CInline {
}
