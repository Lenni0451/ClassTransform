package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CLocalVariable {

    /**
     * The name of the local variable found in the local variable table<br>
     * The local variable table is optional in the class file but is required for the name resolution<br>
     * Use the index if no local variable table is present
     */
    String name() default "";

    /**
     * The var index of the local variable to get
     */
    int index() default -1;

    /**
     * The opcode used to load the variable<br>
     * Allowed Values:<br>
     * - ILOAD<br>
     * - LLOAD<br>
     * - FLOAD<br>
     * - DLOAD<br>
     * - ALOAD<br>
     */
    int loadOpcode() default -1;

    /**
     * Copy the value of the local variable back to the caller method<br>
     * When marking a local variable as modifiable the type of the method parameter is deciding the type of the stored local variable<br>
     * <b>You can use {@link Object} to get any non-primitive variable, but it will be stored back as an {@link Object} and not as the original type<br>
     * This may cause a {@link ClassCastException} when not careful</b>
     */
    boolean modifiable() default false;

}
