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
     * <p>
     * Called once per transformed class after the complete handler chain ran, including handlers inserted before or
     * after this one, and in chain order. The class node may be modified in place.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The transformed class node
     */
    void finish(final TransformerManager transformerManager, final ClassNode transformedClass);

}
