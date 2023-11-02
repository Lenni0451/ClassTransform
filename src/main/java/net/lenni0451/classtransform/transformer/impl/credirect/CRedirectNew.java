package net.lenni0451.classtransform.transformer.impl.credirect;

import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The redirect target transformer for {@link Opcodes#NEW} instructions.
 */
@ParametersAreNonnullByDefault
public class CRedirectNew implements IRedirectTarget {

    @Override
    public void inject(ClassNode targetClass, MethodNode targetMethod, ClassNode transformer, MethodNode transformerMethod, List<AbstractInsnNode> targetNodes, List<MethodInsnNode> transformerMethodCalls) {
        for (AbstractInsnNode instruction : targetNodes) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;

            boolean cast;
            {
                Type returnType = returnType(transformerMethod.desc);
                Type[] argumentTypes = argumentTypes(transformerMethod.desc);
                Type originalReturnType = type(methodInsnNode.owner);
                Type[] originalArgumentTypes = argumentTypes(methodInsnNode.desc);
                if (!ASMUtils.compareType(originalReturnType, returnType)) {
                    throw new TransformerException(transformerMethod, transformer, "does not have same return type as original object")
                            .help(Codifier.of(transformerMethod).returnType(originalReturnType).param(null).params(originalArgumentTypes));
                }
                cast = !originalReturnType.equals(returnType);
                if (!ASMUtils.compareTypes(originalArgumentTypes, argumentTypes)) {
                    throw new TransformerException(transformerMethod, transformer, "does not have same argument types as original constructor")
                            .help(Codifier.of(transformerMethod).param(null).params(originalArgumentTypes));
                }
            }

            int freeVarIndex = ASMUtils.getFreeVarIndex(targetMethod);
            InsnList[] loadStoreOpcodes = getLoadStoreOpcodes(null, methodInsnNode.desc, freeVarIndex);
            InsnList storeOpcodes = loadStoreOpcodes[0];
            InsnList loadOpcodes = loadStoreOpcodes[1];

            targetMethod.instructions.insertBefore(methodInsnNode, storeOpcodes);
            targetMethod.instructions.insertBefore(methodInsnNode, new InsnNode(Opcodes.POP2));
            if (cast) targetMethod.instructions.insert(methodInsnNode, new TypeInsnNode(Opcodes.CHECKCAST, returnType(methodInsnNode.desc).getInternalName()));
            MethodInsnNode transformerCall;
            if (!Modifier.isStatic(transformerMethod.access)) {
                targetMethod.instructions.insertBefore(methodInsnNode, new VarInsnNode(Opcodes.ALOAD, 0));
                targetMethod.instructions.insertBefore(methodInsnNode, loadOpcodes);
                transformerCall = new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc);
            } else {
                targetMethod.instructions.insertBefore(methodInsnNode, loadOpcodes);
                transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
            }
            targetMethod.instructions.set(methodInsnNode, transformerCall);
            transformerMethodCalls.add(transformerCall);
        }
    }

}
