package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inline the injection method into the target method<br>
 * This can be useful to inject into already loaded classes
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface CInline {
}
