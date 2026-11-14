package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A hook for annotation handlers which need to apply changes after all other handlers ran.
 */
@ParametersAreNonnullByDefault
public interface IAnnotationHandlerFinalizer {

    /**
     * Apply all pending changes to the transformed class.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The transformed class node
     */
    void finish(final TransformerManager transformerManager, final ClassNode transformedClass);

}
