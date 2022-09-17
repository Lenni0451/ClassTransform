package net.lenni0451.classtransform.mixinstranslator.annotationtranslator;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mixinstranslator.IAnnotationTranslator;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public class AtTranslator implements IAnnotationTranslator {

    @Override
    public void translate(Map<String, IAnnotationTranslator> translators, AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CTarget.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        if (values.containsKey("shift")) {
            String[] shift = (String[]) values.get("shift");
            shift[0] = Type.getDescriptor(CTarget.Shift.class);
        }
        annotation.values = AnnotationParser.mapToList(values);
    }

}
