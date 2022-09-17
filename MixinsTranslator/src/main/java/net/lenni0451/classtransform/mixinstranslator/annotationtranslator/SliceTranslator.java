package net.lenni0451.classtransform.mixinstranslator.annotationtranslator;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.mixinstranslator.IAnnotationTranslator;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public class SliceTranslator implements IAnnotationTranslator {

    @Override
    public void translate(Map<String, IAnnotationTranslator> translators, AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CSlice.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        if (values.containsKey("from")) this.dynamicTranslate(translators, (AnnotationNode) values.get("from"));
        if (values.containsKey("to")) this.dynamicTranslate(translators, (AnnotationNode) values.get("to"));
        annotation.values = AnnotationParser.mapToList(values);
    }

}
