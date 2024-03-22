package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Map;

public class ModifyExpressionValueTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CModifyExpressionValue.class);
        Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
        if (values.containsKey("at")) values.put("target", values.remove("at"));
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
        }
        if (values.containsKey("slice")) {
            AnnotationNode slice = this.getSingleAnnotation("slice", values, "CWrapCondition");
            if (slice != null) this.dynamicTranslate(slice);
        }
        annotation.values = AnnotationUtils.mapToList(values);
    }

}
