package net.lenni0451.classtransform.transformer;

import org.objectweb.asm.tree.ClassNode;

public interface IAnnotationHandlerPreprocessor {

    /**
     * Process a transformer before it is read<br>
     * You can modify the class transform annotations before parsing
     *
     * @param node The {@link ClassNode} of the transformer
     */
    void process(final ClassNode node);

}
