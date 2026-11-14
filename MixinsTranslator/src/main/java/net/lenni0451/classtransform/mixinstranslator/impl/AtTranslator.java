package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

@ParametersAreNonnullByDefault
class AtTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CTarget.class);
        String point = (String) values.get("value");
        if ("MIXINEXTRAS:EXPRESSION".equalsIgnoreCase(point)) {
            values.put("value", "EXPRESSION");
            Object id = values.remove("id");
            if (id instanceof String) values.put("target", id);
            values.put("remap", false);
            point = "EXPRESSION";
        } else {
            int suffix = point.lastIndexOf(':');
            if (suffix >= 0) {
                values.put("selector", point.substring(suffix + 1));
                point = point.substring(0, suffix);
                values.put("value", point);
            }
        }
        if ("CONSTANT".equalsIgnoreCase((String) values.get("value")) && values.containsKey("args")) {
            String discriminator = null;
            for (String argument : (List<String>) values.remove("args")) {
                if (argument.startsWith("log=")) continue;
                if (!argument.matches("(?s)(nullValue|intValue|floatValue|longValue|doubleValue|stringValue|classValue)=.*")) {
                    throw new IllegalArgumentException("Unsupported CONSTANT argument: " + argument);
                }
                if (discriminator != null) throw new IllegalArgumentException("Multiple constant discriminators are not supported");
                discriminator = argument;
            }
            if (discriminator != null) {
                if (discriminator.startsWith("classValue=")) {
                    String className = discriminator.substring(11).replace('.', '/');
                    discriminator = "classValue=" + Type.getObjectType(className).getDescriptor();
                }
                values.put("target", discriminator);
            }
        }
        if (values.containsKey("value") && values.containsKey("target")) {
            String value = (String) values.get("value");
            String target = (String) values.get("target");
            if (value.equalsIgnoreCase("CONSTANT")) {
                if (target.equals("nullValue=false")) target = "null false";
                else if (target.startsWith("nullValue")) target = "null";
                else if (target.startsWith("intValue=")) target = "int " + target.substring(9);
                else if (target.startsWith("floatValue=")) target = "float " + target.substring(11);
                else if (target.startsWith("longValue=")) target = "long " + target.substring(10);
                else if (target.startsWith("doubleValue=")) target = "double " + target.substring(12);
                else if (target.startsWith("stringValue=")) target = "string " + target.substring(12);
                else if (target.startsWith("classValue=")) target = "type " + target.substring(11);
                else throw new IllegalArgumentException("Invalid target: " + target);
                values.put("target", target);
            }
        }
        if (values.containsKey("shift")) {
            String[] shift = (String[]) values.get("shift");
            shift[0] = typeDescriptor(CTarget.Shift.class);
            if ("NONE".equals(shift[1])) shift[1] = "BEFORE";
            else if ("BEFORE".equals(shift[1]) || "AFTER".equals(shift[1])) {
                values.put("by", "BEFORE".equals(shift[1]) ? -1 : 1);
                shift[1] = "BY";
            }
        } else {
            //Mixins injects before by default most of the time
            values.put("shift", new String[]{typeDescriptor(CTarget.Shift.class), "BEFORE"});
        }
    }

}
