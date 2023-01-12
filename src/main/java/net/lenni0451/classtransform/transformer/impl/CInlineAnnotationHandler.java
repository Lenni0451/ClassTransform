package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CInline;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.MethodInliner;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

public class CInlineAnnotationHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer) {
        List<MethodNode> methodsToInline = transformedClass.methods
                .stream()
                .filter(methodNode -> methodNode.invisibleAnnotations != null)
                .filter(methodNode -> methodNode.invisibleAnnotations
                        .stream()
                        .anyMatch(annotation -> annotation.desc.equals(typeDescriptor(CInline.class))))
                .collect(Collectors.toList());
        for (MethodNode methodNode : methodsToInline) MethodInliner.wrappedInline(transformedClass, methodNode, transformedClass.name);
    }

}
