package net.lenni0451.classtransform.mixinstranslator.classtranslator;

import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.mixinstranslator.IAnnotationTranslator;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Map;

public class InjectTranslator implements IAnnotationTranslator {

    @Override
    public void translate(Map<String, IAnnotationTranslator> translators, AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CInject.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        Boolean optional = null;
        if (values.containsKey("require")) optional = ((int) values.get("require")) <= 0;
        if (values.containsKey("at")) values.put("target", values.remove("at"));
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) {
                this.dynamicTranslate(translators, target);
                if (optional != null) {
                    target.values.add("optional");
                    target.values.add(optional);
                }
            }
        }
        if (values.containsKey("slice")) this.dynamicTranslate(translators, (AnnotationNode) values.get("slice"));
        annotation.values = AnnotationParser.mapToList(values);
    }

}
