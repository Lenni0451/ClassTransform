package net.lenni0451.classtransform.transformer.impl.redirect;

import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.type;

/**
 * The contract for a redirect target transformer.
 * <p>
 * Implementations are selected by {@link net.lenni0451.classtransform.transformer.impl.CRedirectAnnotationHandler} for a matched target instruction and replace it with a call to the transformer method, which is copied into the transformed class beforehand. A transformer method whose shape does not fit the target causes a {@link net.lenni0451.classtransform.exceptions.TransformerException}.
 */
@ParametersAreNonnullByDefault
public interface IRedirectTarget {

    /**
     * Redirects the given target nodes.
     * <p>
     * Implementations replace the target instruction with a call to the transformer method and add that call to the given list, which is used for the coprocessor postprocessing.
     *
     * @param targetClass            The transformed class
     * @param targetMethod           The transformed method
     * @param transformer            The transformer class
     * @param transformerMethod      The transformer method
     * @param targetNode             The instruction to redirect
     * @param transformerMethodCalls The list of calls to the transformer method
     */
    void inject(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final AbstractInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls);

    /**
     * Gets the load and store opcodes for the given method invocation.
     * <p>
     * The store instructions are returned in reverse order, so they can be inserted in front of an invocation which already pushed the receiver and arguments, and the load instructions restore them in the original order. The free variable index is advanced by the size of every stored value.
     *
     * @param owner        The owner of the method, or {@code null} for a static invocation
     * @param desc         The descriptor of the method
     * @param freeVarIndex The local variable index of the first free variable
     * @return The store instructions in reverse order followed by the load instructions
     */
    default InsnList[] getLoadStoreOpcodes(@Nullable final String owner, final String desc, int freeVarIndex) {
        InsnList storeOpcodes = new InsnList();
        InsnList loadOpcodes = new InsnList();

        if (owner != null) {
            Type ownerType = type(owner);
            storeOpcodes.add(new VarInsnNode(ASMUtils.getStoreOpcode(ownerType), freeVarIndex));
            loadOpcodes.add(new VarInsnNode(ASMUtils.getLoadOpcode(ownerType), freeVarIndex));
            freeVarIndex += ownerType.getSize();
        }

        Type[] argumentTypes = argumentTypes(desc);
        for (Type argumentType : argumentTypes) {
            int storeOpcode = ASMUtils.getStoreOpcode(argumentType);
            int loadOpcode = ASMUtils.getLoadOpcode(argumentType);

            storeOpcodes.add(new VarInsnNode(storeOpcode, freeVarIndex));
            loadOpcodes.add(new VarInsnNode(loadOpcode, freeVarIndex));
            freeVarIndex += argumentType.getSize();
        }

        InsnList reversedStoreOpcodes = new InsnList();
        for (int i = storeOpcodes.size() - 1; i >= 0; i--) reversedStoreOpcodes.add(storeOpcodes.get(i));
        return new InsnList[]{reversedStoreOpcodes, loadOpcodes};
    }

}
