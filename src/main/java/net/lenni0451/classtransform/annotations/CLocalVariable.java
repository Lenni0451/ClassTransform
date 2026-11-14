package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.utils.local.CLocalRef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Capture a local variable of the target method.<br>
 * The priority for identifying the local variable is:<br>
 * 1. {@link #name()} and {@link #names()}: The names of the local variable found in the local variable table.<br>
 * 2. {@link #ordinal()}: The ordinal of the local variable in the local variable table.<br>
 * 3. {@link #index()}: The variable index of the local variable.<br>
 * <br>
 * If none of them are set, exactly one local variable of the parameter type must be in scope at the injection point.<br>
 * The parameter type can be the local variable type or one of the reference types in
 * {@code net.lenni0451.classtransform.utils.local} (e.g. {@link CLocalRef}).<br>
 * Reference parameters can both read and write the local variable.
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
     * Additional names of the local variable found in the local variable table.<br>
     * The names are tried in order after {@link #name()}.<br>
     * This can be used if the local variable name differs between versions of the target class.
     *
     * @return The additional names of the local variable
     */
    String[] names() default {};

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
     * Select the only local variable of the parameter type which is in scope at the injection point.<br>
     * This is the selection mode used by MixinExtras when no name, ordinal or index is given.
     *
     * @return If the local variable should be selected implicitly by its type
     */
    boolean implicit() default false;

    /**
     * Require the type of the local variable to match the parameter type exactly.<br>
     * This is enabled by the MixinExtras translation.
     *
     * @return If the local variable type must match the parameter type
     */
    boolean strict() default false;

    /**
     * Only select local variables which are method parameters.<br>
     * This excludes the {@code this} reference and all variables created inside the method.
     *
     * @return If only method parameters should be selected
     */
    boolean argsOnly() default false;

    /**
     * Print all available local variables at the injection point and abort the injection.<br>
     * This can be used to find the correct name, ordinal or index for a local variable.
     *
     * @return If the local variables should be printed
     */
    boolean print() default false;

    /**
     * Copy the value of the local variable back to the caller method.<br>
     * When marking a local variable as modifiable the type of the method parameter is deciding the type of the stored local variable.<br>
     * <b>You can use {@link Object} to get any non-primitive variable, but it will be stored back as an {@link Object} and not as the original type.<br>
     * This may cause a {@link ClassCastException} when not careful.</b><br>
     * Reference parameters are always written back and do not require this option.
     *
     * @return If the local variable should be copied back
     */
    boolean modifiable() default false;

}
