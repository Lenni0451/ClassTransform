package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
class ModifyConstantTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyConstant.class);
        this.translateInjection(values);
        if (values.containsKey("constant")) {
            AnnotationNode constant = this.getSingleAnnotation("constant", values, "CModifyConstant");
            if (constant != null) {
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
                if (expandZeroConditions instanceof List) {
                    List<String[]> conditions = new ArrayList<>();
                    for (Object condition : (List<?>) expandZeroConditions) {
                        conditions.add(new String[]{Type.getDescriptor(CModifyConstant.Condition.class), ((String[]) condition)[1]});
                    }
                    values.put("expandZeroConditions", conditions);
                }
                if (constantValues.containsKey("slice")) values.put("sliceId", constantValues.get("slice"));
            }
        }
    }

}
