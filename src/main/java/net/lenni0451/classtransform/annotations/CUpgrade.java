package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Upgrade the version of a class to use never java features.<br>
 * This does not upgrade any code, it only changes the version of the class.<br>
 * Downgrading is not possible.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CUpgrade">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface CUpgrade {

    /**
     * The new version of the class.<br>
     * If the version is not specified, the class will be upgraded to the version of the transformer.<br>
     * If the class version is higher than the given one, nothing will change.
     *
     * @return The new version of the class
     */
    int value() default -1;

}
