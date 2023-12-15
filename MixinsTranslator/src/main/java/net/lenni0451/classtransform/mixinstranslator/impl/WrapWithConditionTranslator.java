package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class WrapWithConditionTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CWrapCondition.class);
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
