package net.lenni0451.classtransform.transformer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.instrument.ClassFileTransformer;

/**
 * A bytecode transformer like {@link ClassFileTransformer}.
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface IBytecodeTransformer {

    /**
     * Transform the raw bytecode of all loaded classes.<br>
     * Return null if the class should not be transformed.
     * <p>
     * Transformers are called in registration order, each receiving the bytecode returned by the previous one. A null
     * return leaves the bytecode unchanged. Called before the class is parsed into a {@link org.objectweb.asm.tree.ClassNode}.
     *
     * @param className               The name of the transformed class
     * @param bytecode                The raw bytecode of the class
     * @param calculateStackMapFrames If the stack map frames should be computed
     * @return The transformed bytecode or null if not transformed
     */
    byte[] transform(final String className, final byte[] bytecode, final boolean calculateStackMapFrames);

}
