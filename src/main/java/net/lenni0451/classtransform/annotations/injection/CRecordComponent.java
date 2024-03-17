package net.lenni0451.classtransform.annotations.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add a field as a record component and add the corresponding getter methods.<br>
 * This also adds a new constructor to the record class with all added record components.<br>
 * The {@link Object#toString}/{@link Object#equals(Object)}/{@link Object#hashCode()} methods get reimplemented with the new components.
 * The methods will be ignored if they are overridden manually.<br>
 * <br>
 * Adding a record component:<br>
 * <pre>
 * &#64;CRecordComponent
 * private final String text;
 * </pre>
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CRecordComponent">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface CRecordComponent {

    /**
     * @return If the field should be added as a record component
     */
    boolean addRecordComponent() default true;

    /**
     * @return If the field should be added to the constructor
     */
    boolean addConstructor() default true;

    /**
     * @return If a getter method should be added
     */
    boolean addGetter() default true;

    /**
     * @return If the field should be added to the {@link Object#toString()} method
     */
    boolean addToString() default true;

    /**
     * @return If the field should be added to the {@link Object#equals(Object)} method
     */
    boolean addEquals() default true;

    /**
     * @return If the field should be added to the {@link Object#hashCode()} method
     */
    boolean addHashCode() default true;

}
