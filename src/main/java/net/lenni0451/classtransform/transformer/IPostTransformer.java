package net.lenni0451.classtransform.transformer;

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

}
