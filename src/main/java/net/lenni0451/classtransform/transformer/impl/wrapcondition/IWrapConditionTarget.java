package net.lenni0451.classtransform.transformer.impl.wrapcondition;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public interface IWrapConditionTarget<T extends AbstractInsnNode> {

    /**
     * Wrap the given target node.
     *
     * @param transformedClass  The transformed class
     * @param transformer       The transformer class
     * @param transformerMethod The transformer method
     * @param target            The transformed method
     * @param insnNode          The instructions to wrap
     */
    MethodInsnNode inject(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final T insnNode);

}
