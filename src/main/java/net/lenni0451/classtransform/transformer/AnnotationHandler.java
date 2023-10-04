package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.InjectionInfo;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

/**
 * The abstract base for all annotation handlers.
 */
@ParametersAreNonnullByDefault
public abstract class AnnotationHandler {

    /**
     * Handle all annotations in the transformer class.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The target class node
     * @param transformer        The transformer class node
     */
    public abstract void transform(final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer);

    /**
     * Get a parsed annotation from a class node.
     *
     * @param <T>                The annotation type
     * @param annotationClass    The annotation class
     * @param classNode          The class node
     * @param transformerManager The transformer manager
     * @return The parsed annotation or null if not found
     */
    @Nullable
    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final ClassNode classNode, final TransformerManager transformerManager) {
        T annotation = this.getAnnotation(annotationClass, classNode.visibleAnnotations, transformerManager);
        if (annotation == null) annotation = this.getAnnotation(annotationClass, classNode.invisibleAnnotations, transformerManager);
        return annotation;
    }

    /**
     * Get a parsed annotation from a field node.
     *
     * @param <T>                The annotation type
     * @param annotationClass    The annotation class
     * @param field              The field node
     * @param transformerManager The transformer manager
     * @return The parsed annotation or null if not found
     */
    @Nullable
    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final FieldNode field, final TransformerManager transformerManager) {
        T annotation = this.getAnnotation(annotationClass, field.visibleAnnotations, transformerManager);
        if (annotation == null) annotation = this.getAnnotation(annotationClass, field.invisibleAnnotations, transformerManager);
        return annotation;
    }

    /**
     * Get a parsed annotation from a method node.
     *
     * @param <T>                The annotation type
     * @param annotationClass    The annotation class
     * @param method             The method node
     * @param transformerManager The transformer manager
     * @return The parsed annotation or null if not found
     */
    @Nullable
    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, final MethodNode method, final TransformerManager transformerManager) {
        T annotation = this.getAnnotation(annotationClass, method.visibleAnnotations, transformerManager);
        if (annotation == null) annotation = this.getAnnotation(annotationClass, method.invisibleAnnotations, transformerManager);
        return annotation;
    }

    /**
     * Get a parsed annotation from a list of annotation nodes.
     *
     * @param <T>                The annotation type
     * @param annotationClass    The annotation class
     * @param annotations        The annotation nodes
     * @param transformerManager The transformer manager
     * @return The parsed annotation or null if not found
     */
    @Nullable
    protected <T extends Annotation> T getAnnotation(final Class<T> annotationClass, @Nullable final List<AnnotationNode> annotations, final TransformerManager transformerManager) {
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(typeDescriptor(annotationClass))) {
                    return AnnotationParser.parse(annotationClass, transformerManager, AnnotationUtils.listToMap(annotation.values));
                }
            }
        }
        return null;
    }

    /**
     * Add the {@link InjectionInfo} annotation to the given method.
     *
     * @param transformer The transformer class node
     * @param method      The method node
     */
    protected void prepareForCopy(final ClassNode transformer, final MethodNode method) {
        AnnotationNode injectionInfo = new AnnotationNode(typeDescriptor(InjectionInfo.class));
        injectionInfo.values = Arrays.asList(
                "transformer", transformer.name,
                "originalName", method.name + method.desc
        );
        if (method.invisibleAnnotations == null) method.invisibleAnnotations = new ArrayList<>();
        method.invisibleAnnotations.add(injectionInfo);
    }

    /**
     * Rename a method and add it to the target class node.
     *
     * @param injectionMethod  The transformer method node
     * @param targetMethod     The target method node
     * @param transformer      The transformer class node
     * @param transformedClass The target class node
     * @param extra            Extra data for the generated method name
     */
    protected void renameAndCopy(final MethodNode injectionMethod, final MethodNode targetMethod, final ClassNode transformer, final ClassNode transformedClass, final String extra) {
        this.prepareForCopy(transformer, injectionMethod);
        int i = 0;
        String baseName = injectionMethod.name + "$" + targetMethod.name.replaceAll("[<>]", "") + "$" + extra;
        do {
            injectionMethod.name = baseName + i++;
        } while (this.hasMethod(transformedClass, injectionMethod.name));
        Remapper.remapAndAdd(transformer, transformedClass, injectionMethod);
    }


    private boolean hasMethod(final ClassNode node, final String name) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) return true;
        }
        return false;
    }

}
