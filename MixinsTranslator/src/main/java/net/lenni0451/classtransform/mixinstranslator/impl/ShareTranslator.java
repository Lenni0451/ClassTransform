package net.lenni0451.classtransform.mixinstranslator.impl;

import jdk.internal.org.objectweb.asm.Type;
import net.lenni0451.classtransform.annotations.CShared;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ShareTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CShared.class);
    }

}
