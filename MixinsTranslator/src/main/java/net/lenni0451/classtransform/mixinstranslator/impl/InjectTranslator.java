package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CInject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
class InjectTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CInject.class);
        Boolean optional = null;
        if (values.containsKey("require")) optional = ((int) values.get("require")) <= 0;
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) {
                this.dynamicTranslate(target);
                if (optional != null) {
                    target.values.add("optional");
                    target.values.add(optional);
                }
            }
        }
        if (values.containsKey("slice")) {
            AnnotationNode slice = this.getSingleAnnotation("slice", values, "CInject");
            if (slice != null) this.dynamicTranslate(slice);
        }
    }

}
