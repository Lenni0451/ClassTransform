package net.lenni0451.classtransform.transformer.impl.credirect;

import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.type;

/**
 * The interface for a redirect target transformer.
 */
@ParametersAreNonnullByDefault
public interface IRedirectTarget {

    /**
     * Redirect the given target nodes.
     *
     * @param targetClass       The transformed class
     * @param targetMethod      The transformed method
     * @param transformer       The transformer class
     * @param transformerMethod The transformer method
     * @param targetNodes       The instructions to redirect
     */
    void inject(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final List<AbstractInsnNode> targetNodes);

    /**
     * Get the load and store opcodes for the given method invocation.
     *
     * @param owner        The owner of the method
     * @param desc         The descriptor of the method
     * @param freeVarIndex The index of the free variable
     * @return The load and store opcodes
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
