package net.lenni0451.classtransform.transformer.impl.wrapcondition;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The contract for wrapping a single instruction with a boolean condition.
 * <p>
 * Implementations are selected by {@link net.lenni0451.classtransform.transformer.impl.CWrapConditionAnnotationHandler} based on the target instruction type. The condition is evaluated before the wrapped operation; {@code true} keeps the operation and {@code false} skips it.
 *
 * @param <T> The instruction type this target wraps
 */
public interface IWrapConditionTarget<T extends AbstractInsnNode> {

    /**
     * Wraps the given target node.
     * <p>
     * The transformer method must return {@code boolean} and its static modifier must match the method containing the instruction.
     *
     * @param transformedClass  The transformed class
     * @param transformer       The transformer class
     * @param transformerMethod The copied transformer method returning the condition
     * @param target            The method containing the instruction
     * @param insnNode          The instruction to wrap
     * @return The inserted call to the transformer method
     */
    MethodInsnNode inject(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final T insnNode);

}
