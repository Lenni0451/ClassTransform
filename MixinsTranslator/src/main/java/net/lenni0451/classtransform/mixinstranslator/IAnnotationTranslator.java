package net.lenni0451.classtransform.mixinstranslator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public interface IAnnotationTranslator {

    void translate(final Map<String, IAnnotationTranslator> translators, final AnnotationNode annotation);

    default void dynamicTranslate(final Map<String, IAnnotationTranslator> translators, final AnnotationNode annotation) {
        IAnnotationTranslator translator = translators.get(Type.getType(annotation.desc).getClassName());
        if (translator != null) translator.translate(translators, annotation);
    }

}
