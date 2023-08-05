package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CShadow;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class ShadowTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CShadow.class);
    }

}
