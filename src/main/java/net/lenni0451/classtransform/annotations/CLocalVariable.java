package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.annotations.injection.CInject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark the parameter of a {@link CInject} transformer method as a local variable.<br>
 * Local variables have to be at the end of the parameter list.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CLocalVariable {

    /**
     * The name of the local variable found in the local variable table.<br>
     * The local variable table is optional in the class file but is required for the name resolution.<br>
     * Use the index if no local variable table is present.<br>
     * If neither the {@link #name()} nor the {@link #index()} is set, the parameter name will be used instead (if available).<br>
     * If {@link #name()} and {@link #index()} are both set, the name will be used if found. Otherwise, the index will be used instead.
     *
     * @return The name of the local variable
     */
    String name() default "";

    /**
     * The var index of the local variable to get.<br>
     * If {@link #name()} and {@link #index()} are both set, the name will be used if found. Otherwise, the index will be used instead.
     *
     * @return The var index
     */
    int index() default -1;

    /**
     * The opcode used to load the variable.<br>
     * Only required if the opcode could not be resolved automatically (e.g. reusing local variable indices).<br>
     * Allowed Values:<br>
     * - ILOAD<br>
     * - LLOAD<br>
     * - FLOAD<br>
     * - DLOAD<br>
     * - ALOAD
     *
     * @return The opcode
     */
    int loadOpcode() default -1;

    /**
     * Copy the value of the local variable back to the caller method.<br>
     * When marking a local variable as modifiable the type of the method parameter is deciding the type of the stored local variable.<br>
     * <b>You can use {@link Object} to get any non-primitive variable, but it will be stored back as an {@link Object} and not as the original type.<br>
     * This may cause a {@link ClassCastException} when not careful.</b>
     *
     * @return If the local variable should be copied back
     */
    boolean modifiable() default false;

}
