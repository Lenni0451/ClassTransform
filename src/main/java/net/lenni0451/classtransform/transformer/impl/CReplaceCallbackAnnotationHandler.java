package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CReplaceCallback;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.InjectionCallbackReplacer;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Applies the {@link CReplaceCallback} annotation by replacing every {@link net.lenni0451.classtransform.InjectionCallback} reference in the transformed class with an {@code Object[]} based callback.
 * <p>
 * This allows the transformed class to run without the callback class on its classpath.
 */
@ParametersAreNonnullByDefault
public class CReplaceCallbackAnnotationHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        if (AnnotationUtils.hasAnnotation(transformer, CReplaceCallback.class)) InjectionCallbackReplacer.replaceCallback(transformedClass);
    }

}
