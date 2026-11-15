package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

/**
 * Translates Mixin's {@code @At} into ClassTransform's {@link CTarget}.
 * <p>
 * The injection point is taken from {@code value} and {@code target}. A {@code MIXINEXTRAS:EXPRESSION} point becomes
 * {@code EXPRESSION} with the id as target and remapping disabled, and a point with a {@code :selector} suffix is split
 * into the point type and the {@link CTarget#selector()} value. For {@code CONSTANT} points the discriminator arguments
 * such as {@code intValue=1} are converted to the ClassTransform target format, while {@code log=} and
 * {@code expandZeroConditions=} arguments are kept.
 * <p>
 * Mixin's shift is mapped to ClassTransform's shift and offset: {@code NONE} becomes {@code BEFORE}, {@code BEFORE} and
 * {@code AFTER} become {@code BY} with {@code by} set to {@code -1} or {@code 1}, and a missing shift defaults to
 * {@code BEFORE}. Unsupported constant arguments, multiple constant discriminators and invalid targets are rejected
 * with an {@link IllegalArgumentException}.
 *
 * @see CTarget
 */
@ParametersAreNonnullByDefault
class AtTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CTarget.class);
        Object desc = values.get("desc");
        if (desc instanceof AnnotationNode) this.dynamicTranslate((AnnotationNode) desc);
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
            List<String> extraArgs = new ArrayList<>();
            for (String argument : (List<String>) values.remove("args")) {
                if (argument.startsWith("log=") || argument.startsWith("expandZeroConditions=")) {
                    extraArgs.add(argument);
                    continue;
                }
                if (!argument.matches("(?s)(nullValue|intValue|floatValue|longValue|doubleValue|stringValue|classValue)=.*")) {
                    throw new IllegalArgumentException("Unsupported CONSTANT argument: " + argument);
                }
                if (discriminator != null) throw new IllegalArgumentException("Multiple constant discriminators are not supported");
                discriminator = argument;
            }
            if (!extraArgs.isEmpty()) values.put("args", extraArgs);
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
