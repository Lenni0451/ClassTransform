package net.lenni0451.classtransform.mixinstranslator.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public interface IAnnotationTranslator {

    void translate(final AnnotationNode annotation);

    default void dynamicTranslate(final AnnotationNode annotation) {
        IAnnotationTranslator translator = AnnotationTranslatorManager.getTranslator(Type.getType(annotation.desc));
        if (translator != null) translator.translate(annotation);
    }

    default AnnotationNode getSingleAnnotation(final String name, final Map<String, Object> values, final String annotationName) {
        Object rawValue = values.remove(name);
        if (rawValue instanceof AnnotationNode) {
            return (AnnotationNode) rawValue;
        } else if (rawValue instanceof List) {
            values.remove(name);
            List<AnnotationNode> list = (List<AnnotationNode>) rawValue;
            if (list.isEmpty()) {
                return null;
            } else if (list.size() == 1) {
                values.put(name, list.get(0));
                return list.get(0);
            } else {
                throw new IllegalStateException("ClassTransform does not support multiple " + name + " in @" + annotationName + " annotations");
            }
        } else {
            throw new IllegalStateException("Unknown type for " + name + " in @" + annotationName + " annotation");
        }
    }

}
