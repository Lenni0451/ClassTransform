package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

@ParametersAreNonnullByDefault
class AtTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CTarget.class);
        Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
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
                else throw new IllegalArgumentException("Invalid target: " + target);
                values.put("target", target);
            } else if (value.equalsIgnoreCase("JUMP")) {
                Object opcode = values.get("opcode");
                if (!(opcode instanceof Integer)) throw new IllegalArgumentException("ClassTransform requires an opcode to be specified");
                values.put("value", "OPCODE");
                values.put("target", opcode);
            } else if (value.equalsIgnoreCase("FIELD")) {
                if (values.containsKey("opcode")) {
                    throw new IllegalArgumentException("ClassTransform does not support the FIELD target with opcode. Please refer to the At#opcode javadoc for information");
                }
            }
        }
        if (values.containsKey("shift")) {
            String[] shift = (String[]) values.get("shift");
            shift[0] = typeDescriptor(CTarget.Shift.class);
        } else {
            //Mixins injects before by default most of the time
            values.put("shift", new String[]{typeDescriptor(CTarget.Shift.class), "BEFORE"});
        }
        annotation.values = AnnotationUtils.mapToList(values);
    }

}
