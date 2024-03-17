package net.lenni0451.classtransform.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The priority for identifying the local variable is:<br>
 * 1. {@link #name()}: The name of the local variable found in the local variable table.<br>
 * 2. {@link #ordinal()}: The ordinal of the local variable in the local variable table.<br>
 * 3. {@link #index()}: The variable index of the local variable.<br>
 * <br>
 * If none of {@link #name()}, {@link #ordinal()} or {@link #index()} are set, the local variable will be identified by the name of the method parameter.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CLocalVariable">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CLocalVariable {

    /**
     * The name of the local variable found in the local variable table.<br>
     * The local variable table is optional in the class file but is required for the name resolution.<br>
     * Use the index if no local variable table is present.
     *
     * @return The name of the local variable
     */
    String name() default "";

    /**
     * The ordinal of the local variable to get from the local variable table.<br>
     * The ordinal is counted for every type of local variable (e.g. I, L, Ljava/lang/String;, ...).<br>
     * Use the index if no local variable table is present.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * The var index of the local variable to get.
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
