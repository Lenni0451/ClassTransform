package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Map;

public class ModifyExpressionValueTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyExpressionValue.class);
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
        }
        if (values.containsKey("slice")) {
            AnnotationNode slice = this.getSingleAnnotation("slice", values, "CWrapCondition");
            if (slice != null) this.dynamicTranslate(slice);
        }
    }

}
