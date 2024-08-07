package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CRedirect;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
class RedirectTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CRedirect.class);
        Boolean optional = null;
        if (values.containsKey("require")) optional = ((int) values.get("require")) <= 0;
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            AnnotationNode target = (AnnotationNode) values.get("target");
            this.dynamicTranslate(target);
            if (optional != null) {
                target.values.add("optional");
                target.values.add(optional);
            }
        }
        if (values.containsKey("slice")) this.dynamicTranslate((AnnotationNode) values.get("slice"));
    }

}
