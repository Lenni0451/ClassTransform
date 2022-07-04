package net.lenni0451.classtransform.transformer;

public interface IPostTransformer {

    /**
     * Transform the target class after all ClassTransformers have been applied
     *
     * @param className The name of the {@link Class}
     * @param bytecode  The raw bytecode of the {@link Class}
     */
    void transform(final String className, final byte[] bytecode);

}
