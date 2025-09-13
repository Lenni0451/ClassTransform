package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
class SliceTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CSlice.class);
        if (values.containsKey("from")) this.dynamicTranslate((AnnotationNode) values.get("from"));
        if (values.containsKey("to")) this.dynamicTranslate((AnnotationNode) values.get("to"));
    }

}
