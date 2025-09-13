package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public interface AnnotationTranslator {

    void translate(final AnnotationNode annotation, final Map<String, Object> values);

    default void dynamicTranslate(final AnnotationNode annotation) {
        AnnotationTranslator translator = AnnotationTranslatorManager.getTranslator(Type.getType(annotation.desc));
        if (translator != null) {
            Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
            translator.translate(annotation, values);
            annotation.values = AnnotationUtils.mapToList(values);
        }
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

    default void move(final Map<String, Object> values, final String from, final String to) {
        if (values.containsKey(from)) values.put(to, values.remove(from));
    }

    default <F, T> void map(final Map<String, Object> values, final String from, final String to, final Function<F, T> mapper) {
        if (values.containsKey(from)) values.put(to, mapper.apply((F) values.remove(from)));
    }

    default void move(final Map<String, Object> from, final Map<String, Object> to, final String key) {
        if (from.containsKey(key)) to.put(key, from.remove(key));
    }

}
