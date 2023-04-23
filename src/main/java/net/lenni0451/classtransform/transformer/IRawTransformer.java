package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import org.objectweb.asm.tree.ClassNode;

/**
 * A transformer which has access to the parsed class node of the transformed class.
 */
public interface IRawTransformer {

    /**
     * Transform the target class node.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The target class node
     * @return The transformed class node or the same if nothing was changed
     */
    ClassNode transform(final TransformerManager transformerManager, final ClassNode transformedClass);

}
