package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

class AtTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CTarget.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        if (values.containsKey("value") && values.containsKey("target")) {
            String value = (String) values.get("value");
            String target = (String) values.get("target");
            if (value.equalsIgnoreCase("CONSTANT")) {
                if (target.startsWith("nullValue")) target = "null";
                else if (target.startsWith("intValue=")) target = "int " + target.substring(9);
                else if (target.startsWith("floatValue=")) target = "float " + target.substring(11);
                else if (target.startsWith("longValue=")) target = "long " + target.substring(10);
                else if (target.startsWith("doubleValue=")) target = "double " + target.substring(12);
                else if (target.startsWith("stringValue=")) target = "string " + target.substring(12);
                else if (target.startsWith("classValue=")) target = "type " + target.substring(11);
                values.put("target", target);
            }
        }
        if (values.containsKey("shift")) {
            String[] shift = (String[]) values.get("shift");
            shift[0] = Type.getDescriptor(CTarget.Shift.class);
        }
        annotation.values = AnnotationParser.mapToList(values);
    }

}
