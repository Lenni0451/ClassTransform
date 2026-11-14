package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Information about a lambda metafactory invocation.
 */
@ParametersAreNonnullByDefault
public class LMFInfo {

    /**
     * Get the lambda metafactory information of a flow value.
     *
     * @param node        The flow value
     * @param targetClass The name of the transformed class
     * @return The information or null if the value is not a lambda
     */
    @Nullable
    public static LMFInfo get(final ExpressionFlow.FlowValue node, final String targetClass) {
        if (node.isComplex() || !(node.getInsn() instanceof InvokeDynamicInsnNode)) return null;
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node.getInsn();
        if (!"java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner())) return null;
        if (!indy.bsm.getName().equals("metafactory") && !indy.bsm.getName().equals("altMetafactory")) return null;
        if (indy.bsmArgs.length < 2 || !(indy.bsmArgs[1] instanceof Handle)) return null;
        Handle impl = (Handle) indy.bsmArgs[1];
        Type type = getType(node.inputCount() != 0, impl, targetClass);
        if (type == null) return null;
        return new LMFInfo(impl, type);
    }

    @Nullable
    private static Type getType(final boolean bound, final Handle impl, final String targetClass) {
        switch (impl.getTag()) {
            case Opcodes.H_NEWINVOKESPECIAL:
                return bound ? null : Type.INSTANTIATION;
            case Opcodes.H_INVOKESPECIAL:
                if (!impl.getOwner().equals(targetClass)) return null;
                // fall through
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKEINTERFACE:
                return bound ? Type.BOUND_METHOD : Type.FREE_METHOD;
            case Opcodes.H_INVOKESTATIC:
                return Type.FREE_METHOD;
            default:
                return null;
        }
    }


    public final Handle impl;
    public final Type type;

    private LMFInfo(final Handle impl, final Type type) {
        this.impl = impl;
        this.type = type;
    }


    public enum Type {
        FREE_METHOD, BOUND_METHOD, INSTANTIATION
    }

}
