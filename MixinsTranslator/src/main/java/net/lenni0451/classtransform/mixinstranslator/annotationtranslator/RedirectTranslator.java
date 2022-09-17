package net.lenni0451.classtransform.mixinstranslator.annotationtranslator;

import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.mixinstranslator.IAnnotationTranslator;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public class RedirectTranslator implements IAnnotationTranslator {

    @Override
    public void translate(Map<String, IAnnotationTranslator> translators, AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CRedirect.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        Boolean optional = null;
        if (values.containsKey("require")) optional = ((int) values.get("require")) <= 0;
        if (values.containsKey("at")) values.put("target", values.remove("at"));
        if (values.containsKey("target")) {
            AnnotationNode target = (AnnotationNode) values.get("target");
            this.dynamicTranslate(translators, target);
            if (optional != null) {
                target.values.add("optional");
                target.values.add(optional);
            }
        }
        if (values.containsKey("slice")) this.dynamicTranslate(translators, (AnnotationNode) values.get("slice"));
        annotation.values = AnnotationParser.mapToList(values);
    }

}
