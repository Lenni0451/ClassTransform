package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
class ModifyConstantTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyConstant.class);
        if (values.containsKey("slice")) {
            AnnotationNode slice = this.getSingleAnnotation("slice", values, "CModifyConstant");
            if (slice != null) this.dynamicTranslate(slice);
        }
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
                this.move(constantValues, values, "ordinal");
            }
        }
    }

}
