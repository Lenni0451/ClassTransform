package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CReplaceCallback;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.InjectionCallbackReplacer;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The annotation handler for the {@link CReplaceCallback} annotation.
 */
@ParametersAreNonnullByDefault
public class CReplaceCallbackAnnotationHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        if (AnnotationUtils.hasAnnotation(transformer, CReplaceCallback.class)) InjectionCallbackReplacer.replaceCallback(transformedClass);
    }

}
