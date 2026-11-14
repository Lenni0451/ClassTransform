package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Map;

public class ModifyExpressionValueTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyExpressionValue.class);
        if (values.containsKey("at")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.remove("at");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
            if (targets.size() == 1) values.put("target", targets.get(0));
            else values.put("targets", targets);
        }
        this.translateInjection(values);
    }

}
