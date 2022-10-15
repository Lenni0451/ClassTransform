package net.lenni0451.classtransform.mixinstranslator.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public interface IAnnotationTranslator {

    void translate(final AnnotationNode annotation);

    default void dynamicTranslate(final AnnotationNode annotation) {
        IAnnotationTranslator translator = AnnotationTranslatorManager.getTranslator(Type.getType(annotation.desc));
        if (translator != null) translator.translate(annotation);
    }

}
