package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CUpgrade;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The annotation handler for the {@link CUpgrade} annotation.
 */
@ParametersAreNonnullByDefault
public class CUpgradeAnnotationHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        CUpgrade annotation = this.getAnnotation(CUpgrade.class, transformer, transformerManager);
        if (annotation == null) return;

        int version;
        if (((IParsedAnnotation) annotation).wasSet("value")) version = annotation.value();
        else version = transformer.version;
        transformedClass.version = Math.max(transformedClass.version, version);
    }

}
