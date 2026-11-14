package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                Map<String, Object> constantValues = AnnotationUtils.listToMap(constant.values);
                Object classValue = constantValues.get("classValue");
                if (!(classValue instanceof Type)) throw new IllegalArgumentException("@WrapOperation constant target must specify classValue");
                AnnotationNode target = new AnnotationNode(Type.getDescriptor(CTarget.class));
                target.values = new ArrayList<>(Arrays.asList("value", "INSTANCEOF", "target", ((Type) classValue).getInternalName()));
                targets.add(target);
            }
        }
        values.put("target", targets);
        this.translateInjection(values);
    }

}
