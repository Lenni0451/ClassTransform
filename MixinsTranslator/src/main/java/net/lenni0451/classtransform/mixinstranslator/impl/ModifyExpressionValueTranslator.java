package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public class ModifyExpressionValueTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyExpressionValue.class);
        if (values.containsKey("at")) {
            AnnotationNode at = this.getSingleAnnotation("at", values, "At");
            if (at != null) {
                this.dynamicTranslate(at);
                values.put("target", at);
            }
        }
        if (values.containsKey("slice")) {
            AnnotationNode slice = this.getSingleAnnotation("slice", values, "CWrapCondition");
            if (slice != null) this.dynamicTranslate(slice);
        }
    }

}
