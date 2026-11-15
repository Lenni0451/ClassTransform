package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @WrapOperation} into {@link CWrapOperation}.
 * <p>
 * Exactly one of the source {@code at} and {@code constant} members must be present. {@code at} entries
 * are translated to targets. Each {@code constant} becomes a {@link CTarget}: a {@code classValue}
 * produces an {@code INSTANCEOF} target, while any other discriminator produces a {@code CONSTANT}
 * target with text such as {@code int 5} or {@code string x}. Ordinals and non-empty slices are copied
 * for every constant; {@code log} and {@code expandZeroConditions} are carried as target arguments for
 * non-class constants.
 * <p>
 * All generated targets are stored in {@code target}, even a single one. Remaining members are copied
 * and slices are normalized by {@link AnnotationTranslator#translateInjection(Map)}. Translation fails
 * with {@link IllegalArgumentException} unless exactly one of {@code at} and {@code constant} is set,
 * or when a constant has no supported discriminator.
 *
 * @see CWrapOperation
 * @see CTarget
 */
@ParametersAreNonnullByDefault
class WrapOperationTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CWrapOperation.class);
        boolean hasAt = values.containsKey("at");
        boolean hasConstant = values.containsKey("constant");
        if (hasAt == hasConstant) throw new IllegalArgumentException("@WrapOperation must specify exactly one of at and constant");
        List<AnnotationNode> targets = new ArrayList<>();
        if (hasAt) {
            for (AnnotationNode at : (List<AnnotationNode>) values.remove("at")) {
                this.dynamicTranslate(at);
                targets.add(at);
            }
        } else {
            for (AnnotationNode constant : (List<AnnotationNode>) values.remove("constant")) {
                targets.add(this.translateConstant(AnnotationUtils.listToMap(constant.values)));
            }
        }
        values.put("target", targets);
        this.translateInjection(values);
    }

    private AnnotationNode translateConstant(final Map<String, Object> constant) {
        AnnotationNode target = new AnnotationNode(Type.getDescriptor(CTarget.class));
        List<Object> targetValues = new ArrayList<>();
        Object classValue = constant.get("classValue");
        if (classValue instanceof Type) {
            targetValues.add("value");
            targetValues.add("INSTANCEOF");
            targetValues.add("target");
            targetValues.add(((Type) classValue).getInternalName());
        } else {
            targetValues.add("value");
            targetValues.add("CONSTANT");
            targetValues.add("target");
            targetValues.add(this.getConstantTarget(constant));
            List<String> args = new ArrayList<>();
            Object expandZeroConditions = constant.get("expandZeroConditions");
            if (expandZeroConditions instanceof List) {
                StringBuilder conditions = new StringBuilder();
                for (Object condition : (List<?>) expandZeroConditions) {
                    if (conditions.length() > 0) conditions.append(',');
                    conditions.append(((String[]) condition)[1]);
                }
                args.add("expandZeroConditions=" + conditions);
            }
            if (Boolean.TRUE.equals(constant.get("log"))) args.add("log=true");
            if (!args.isEmpty()) {
                targetValues.add("args");
                targetValues.add(args);
            }
        }
        Object ordinal = constant.get("ordinal");
        if (ordinal instanceof Integer) {
            targetValues.add("ordinal");
            targetValues.add(ordinal);
        }
        Object slice = constant.get("slice");
        if (slice instanceof String && !((String) slice).isEmpty()) {
            targetValues.add("slice");
            targetValues.add(slice);
        }
        target.values = targetValues;
        return target;
    }

    private String getConstantTarget(final Map<String, Object> constant) {
        if (constant.containsKey("nullValue")) return Boolean.TRUE.equals(constant.get("nullValue")) ? "null" : "null false";
        if (constant.containsKey("intValue")) return "int " + constant.get("intValue");
        if (constant.containsKey("longValue")) return "long " + constant.get("longValue");
        if (constant.containsKey("floatValue")) return "float " + constant.get("floatValue");
        if (constant.containsKey("doubleValue")) return "double " + constant.get("doubleValue");
        if (constant.containsKey("stringValue")) return "string " + constant.get("stringValue");
        throw new IllegalArgumentException("@WrapOperation constant target must specify a constant discriminator");
    }

}
