package net.lenni0451.classtransform.transformer;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A preprocessor for annotation handlers.<br>
 * This is used to modify the transformer class before it is parsed.
 */
@ParametersAreNonnullByDefault
@FunctionalInterface
public interface IAnnotationHandlerPreprocessor {

    /**
     * Process a transformer before it is read.<br>
     * You can modify the class transform annotations before parsing.
     *
     * @param node The class node of the transformer
     */
    void process(final ClassNode node);

    /**
     * Process a transformer before it is read.<br>
     * You can modify the class transform annotations before parsing.
     *
     * @param node The class node of the transformer
     * @return The modified class node
     */
    default ClassNode replace(final ClassNode node) {
        return node;
    }

}
