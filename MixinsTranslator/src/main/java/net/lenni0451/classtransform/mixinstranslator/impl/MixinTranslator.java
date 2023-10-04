package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
class MixinTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CTransformer.class);
        Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
        if (values.containsKey("targets")) values.put("name", values.remove("targets"));
        annotation.values = AnnotationUtils.mapToList(values);
    }

}
