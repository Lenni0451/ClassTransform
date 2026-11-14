package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow a handler parameter or return type to be coerced.<br>
 * Parameters marked with this annotation can accept a supertype of the value or an int-like type for an int value.<br>
 * A handler method marked with this annotation can return a supertype of the expected value,
 * which is cast back to the expected type after the call.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface CCoerce {
}
