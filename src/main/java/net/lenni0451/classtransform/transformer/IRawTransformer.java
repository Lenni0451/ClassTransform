package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import org.objectweb.asm.tree.ClassNode;

public interface IRawTransformer {

    /**
     * Transform the target class directly
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The target {@link ClassNode}
     * @return The transformed {@link ClassNode} or the same if nothing was changed
     */
    ClassNode transformer(final TransformerManager transformerManager, final ClassNode transformedClass);

}
