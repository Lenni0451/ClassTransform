package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a parameter as a cancellable callback.<br>
 * The parameter type must be {@link net.lenni0451.classtransform.InjectionCallback}.<br>
 * The callback can be used to cancel the target method from any injection handler.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CCancellable {
}
