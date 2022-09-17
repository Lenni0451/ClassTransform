package net.lenni0451.classtransform.mixinstranslator.annotationtranslator;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.mixinstranslator.IAnnotationTranslator;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public class MixinTranslator implements IAnnotationTranslator {

    @Override
    public void translate(Map<String, IAnnotationTranslator> translators, AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CTransformer.class);
        Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
        if (values.containsKey("targets")) values.put("name", values.remove("targets"));
        annotation.values = AnnotationParser.mapToList(values);
    }

}
