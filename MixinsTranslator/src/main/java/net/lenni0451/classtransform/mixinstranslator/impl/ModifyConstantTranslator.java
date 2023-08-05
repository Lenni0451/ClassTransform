package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
class ModifyConstantTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CModifyConstant.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        if (values.containsKey("slice")) this.dynamicTranslate((AnnotationNode) values.get("slice"));
        if (values.containsKey("constant")) {
            AnnotationNode constant = (AnnotationNode) values.remove("constant");
            Map<String, Object> constantValues = AnnotationParser.listToMap(constant.values);
            if (constantValues.containsKey("nullValue")) values.put("nullValue", constantValues.get("nullValue"));
            if (constantValues.containsKey("intValue")) values.put("intValue", constantValues.get("intValue"));
            if (constantValues.containsKey("floatValue")) values.put("floatValue", constantValues.get("floatValue"));
            if (constantValues.containsKey("longValue")) values.put("longValue", constantValues.get("longValue"));
            if (constantValues.containsKey("doubleValue")) values.put("doubleValue", constantValues.get("doubleValue"));
            if (constantValues.containsKey("stringValue")) values.put("stringValue", constantValues.get("stringValue"));
            if (constantValues.containsKey("classValue")) values.put("typeValue", constantValues.get("classValue"));
        }
        annotation.values = AnnotationParser.mapToList(values);
    }

}
