package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CInline;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.MethodInliner;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The annotation handler for the {@link CInline} annotation.
 */
@ParametersAreNonnullByDefault
public class CInlineAnnotationHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        List<MethodNode> methodsToInline = transformedClass.methods
                .stream()
                .filter(methodNode -> AnnotationUtils.findInvisibleAnnotation(methodNode, CInline.class).isPresent())
                .collect(Collectors.toList());
        for (MethodNode methodNode : methodsToInline) MethodInliner.wrappedInline(transformedClass, methodNode, transformedClass.name);
    }

}
