package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.transformer.InjectionPhase;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class WrapWithConditionTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        if (annotation.desc.equals("Lcom/llamalad7/mixinextras/injector/WrapWithCondition;")) {
            values.put("phase", new String[]{Type.getDescriptor(InjectionPhase.class), "REGULAR"});
        }
        annotation.desc = Type.getDescriptor(CWrapCondition.class);
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
        }
        this.translateInjection(values);
    }

}
