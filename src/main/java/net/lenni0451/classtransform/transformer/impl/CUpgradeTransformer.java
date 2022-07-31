package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CUpgrade;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.ATransformer;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

public class CUpgradeTransformer extends ATransformer {

    @Override
    public void transform(TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer) {
        CUpgrade annotation = this.getAnnotation(CUpgrade.class, transformer, classProvider);
        if (annotation == null) return;

        int version;
        if (((IParsedAnnotation) annotation).wasSet("value")) version = annotation.value();
        else version = transformer.version;
        transformedClass.version = Math.max(transformedClass.version, version);
    }

}
