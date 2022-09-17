package net.lenni0451.classtransform.mixinstranslator.annotationtranslator;

import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.mixinstranslator.IAnnotationTranslator;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.Map;

public class ShadowTranslator implements IAnnotationTranslator {

    @Override
    public void translate(Map<String, IAnnotationTranslator> translators, AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CShadow.class);
    }

}
