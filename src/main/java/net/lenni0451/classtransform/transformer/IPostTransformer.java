package net.lenni0451.classtransform.transformer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A handler for all transformed classes after they are transformed.
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface IPostTransformer {

    /**
     * Handle the name and bytecode of all transformed classes.
     *
     * @param className The name of the class
     * @param bytecode  The transformed bytecode of the class
     */
    void transform(final String className, final byte[] bytecode);

    /**
     * Handle the name and bytecode of all transformed classes.<br>
     * If the class should be replaced return the new bytecode, otherwise return {@code null}.
     *
     * @param className The name of the class
     * @param bytecode  The transformed bytecode of the class
     * @return The new bytecode or null if not replaced
     */
    @Nullable
    default byte[] replace(final String className, final byte[] bytecode) {
        this.transform(className, bytecode);
        return null;
    }

}
