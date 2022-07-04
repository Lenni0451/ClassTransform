package net.lenni0451.classtransform.transformer.impl.credirect;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public interface IRedirectTarget {

    void inject(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final List<AbstractInsnNode> targetNodes);

}
