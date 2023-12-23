package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Share a variable between multiple methods injecting into the same target method.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CShared {

    /**
     * @return The name of the shared variable
     */
    String value();

    /**
     * @return If the shared variable should be globally accessible
     */
    boolean global() default false;

}
