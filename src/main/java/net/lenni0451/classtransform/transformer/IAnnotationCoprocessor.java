package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.annotations.injection.COverride;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Coprocessor for annotation handlers.<br>
 * Used to add support for special annotations like {@link CLocalVariable}.<br>
 * Annotation handlers which do not inject calls to the target method will not need this (e.g. {@link CASM}, {@link COverride}).<br>
 * <b>If you are adding your own annotation handler you need to add support for this manually <i>(unless you don't inject calls to the target method)</i>.</b>
 */
public interface IAnnotationCoprocessor {

    /**
     * Preprocess the target method before the annotation handler injects calls to the target method.<br>
     * This happens before the transformer method is verified by the annotation handler.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The target class node
     * @param transformedMethod  The target method node
     * @param transformer        The transformer class node
     * @param transformerMethod  The transformer method node
     * @return The preprocessed method node
     */
    MethodNode preprocess(final TransformerManager transformerManager, final ClassNode transformedClass, final MethodNode transformedMethod, final ClassNode transformer, final MethodNode transformerMethod);

    /**
     * Process the target method before the annotation handler injects calls to the target method.<br>
     * This happens before the transformer method is verified by the annotation handler but after {@link #preprocess(TransformerManager, ClassNode, MethodNode, ClassNode, MethodNode)}.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The target class node
     * @param transformedMethod  The target method node
     * @param transformer        The transformer class node
     * @param transformerMethod  The transformer method node
     * @return The preprocessed method node
     */
    MethodNode transform(final TransformerManager transformerManager, final ClassNode transformedClass, final MethodNode transformedMethod, final ClassNode transformer, final MethodNode transformerMethod);

    /**
     * Postprocess the transformer and target method after the annotation handler injected calls to the target method.<br>
     * The {@code transformerMethodCalls} list only contains direct calls to the transformer method.
     *
     * @param transformerManager     The transformer manager
     * @param transformedClass       The target class node
     * @param transformedMethod      The target method node
     * @param transformerMethodCalls The list of calls to the transformer method
     * @param transformer            The transformer class node
     * @param transformerMethod      The transformer method node
     */
    void postprocess(final TransformerManager transformerManager, final ClassNode transformedClass, final MethodNode transformedMethod, final List<MethodInsnNode> transformerMethodCalls, final ClassNode transformer, final MethodNode transformerMethod);

}
