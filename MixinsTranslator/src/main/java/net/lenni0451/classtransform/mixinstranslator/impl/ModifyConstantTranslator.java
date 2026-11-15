package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates Mixin's {@code @ModifyConstant} into {@link CModifyConstant}.
 * <p>
 * A single source constant is flattened onto the target annotation. When more than one constant is
 * given, each is translated to a {@link CModifyConstant.CConstant} entry in {@code constants},
 * preserving the source order. The constant's {@code classValue} becomes {@code typeValue},
 * {@code expandZeroConditions} is retargeted to {@link CModifyConstant.Condition}, and a constant-level
 * {@code slice} becomes {@code sliceId} for a flattened constant or stays as {@code slice} inside a
 * {@code CConstant}. The source {@code slice} array is normalized to {@code slices} by
 * {@link AnnotationTranslator#translateInjection(Map)}.
 *
 * @see CModifyConstant
 */
@ParametersAreNonnullByDefault
class ModifyConstantTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyConstant.class);
        this.translateInjection(values);
        Object rawConstants = values.remove("constant");
        if (rawConstants instanceof AnnotationNode) {
            this.translateSingleConstant((AnnotationNode) rawConstants, values);
        } else if (rawConstants instanceof List) {
            List<AnnotationNode> constants = (List<AnnotationNode>) rawConstants;
            if (constants.size() <= 1) {
                for (AnnotationNode constant : constants) this.translateSingleConstant(constant, values);
            } else {
                List<AnnotationNode> translated = new ArrayList<>();
                for (AnnotationNode constant : constants) translated.add(this.translateConstant(constant));
                values.put("constants", translated);
            }
        }
    }

    private void translateSingleConstant(final AnnotationNode constant, final Map<String, Object> values) {
        Map<String, Object> constantValues = AnnotationUtils.listToMap(constant.values);
        this.move(constantValues, values, "nullValue");
        this.move(constantValues, values, "intValue");
        this.move(constantValues, values, "floatValue");
        this.move(constantValues, values, "longValue");
        this.move(constantValues, values, "doubleValue");
        this.move(constantValues, values, "stringValue");
        this.move(constantValues, values, "classValue");
        this.move(values, "classValue", "typeValue");
        this.move(constantValues, values, "ordinal");
        this.move(constantValues, values, "log");
        Object expandZeroConditions = constantValues.remove("expandZeroConditions");
        if (expandZeroConditions != null) values.put("expandZeroConditions", this.translateExpandZeroConditions(expandZeroConditions));
        if (constantValues.containsKey("slice")) values.put("sliceId", constantValues.get("slice"));
    }

    private AnnotationNode translateConstant(final AnnotationNode constant) {
        AnnotationNode translated = new AnnotationNode(Type.getDescriptor(CModifyConstant.CConstant.class));
        Map<String, Object> constantValues = AnnotationUtils.listToMap(constant.values);
        Map<String, Object> values = new HashMap<>();
        this.move(constantValues, values, "nullValue");
        this.move(constantValues, values, "intValue");
        this.move(constantValues, values, "floatValue");
        this.move(constantValues, values, "longValue");
        this.move(constantValues, values, "doubleValue");
        this.move(constantValues, values, "stringValue");
        this.move(constantValues, values, "classValue");
        this.move(values, "classValue", "typeValue");
        this.move(constantValues, values, "ordinal");
        this.move(constantValues, values, "slice");
        this.move(constantValues, values, "log");
        Object expandZeroConditions = constantValues.remove("expandZeroConditions");
        if (expandZeroConditions != null) values.put("expandZeroConditions", this.translateExpandZeroConditions(expandZeroConditions));
        translated.values = AnnotationUtils.mapToList(values);
        return translated;
    }

    private List<String[]> translateExpandZeroConditions(final Object expandZeroConditions) {
        List<String[]> conditions = new ArrayList<>();
        for (Object condition : (List<?>) expandZeroConditions) {
            conditions.add(new String[]{Type.getDescriptor(CModifyConstant.Condition.class), ((String[]) condition)[1]});
        }
        return conditions;
    }

}
