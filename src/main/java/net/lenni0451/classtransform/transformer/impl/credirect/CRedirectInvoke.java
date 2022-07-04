package net.lenni0451.classtransform.transformer.impl.credirect;

import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.List;

public class CRedirectInvoke implements IRedirectTarget {

    @Override
    public void inject(ClassNode targetClass, MethodNode targetMethod, ClassNode transformer, MethodNode transformerMethod, List<AbstractInsnNode> targetNodes) {
        for (AbstractInsnNode instruction : targetNodes) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;

            boolean cast;
            {
                Type returnType = Type.getReturnType(transformerMethod.desc);
                Type[] argumentTypes = Type.getArgumentTypes(transformerMethod.desc);
                Type originalReturnType = Type.getReturnType(methodInsnNode.desc);
                Type[] originalArgumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
                if (!ASMUtils.compareType(originalReturnType, returnType)) {//TODO: Cast
                    throw new TransformerException(transformerMethod, transformer, "does not have same return type as original invoke")
                            .help(Codifier.of(transformerMethod).returnType(originalReturnType).param(null).params(originalArgumentTypes));
                }
                cast = !originalReturnType.equals(returnType);
                if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC) {
                    if (!ASMUtils.compareTypes(originalArgumentTypes, argumentTypes)) {
                        throw new TransformerException(transformerMethod, transformer, "does not have same argument types as original invoke")
                                .help(Codifier.of(transformerMethod).param(null).params(originalArgumentTypes));
                    }
                } else {
                    if (!ASMUtils.compareTypes(originalArgumentTypes, argumentTypes, true, Type.getObjectType(methodInsnNode.owner))) {
                        throw new TransformerException(transformerMethod, transformer, "does not have same argument types as original invoke with instance")
                                .help(Codifier.of(transformerMethod).param(null).params(Type.getObjectType(methodInsnNode.owner)).params(originalArgumentTypes));
                    }
                }
            }

            int freeVarIndex = ASMUtils.getFreeVarIndex(targetMethod);
            InsnList[] loadStoreOpcodes = getLoadStoreOpcodes(methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC ? null : methodInsnNode.owner, methodInsnNode.desc, freeVarIndex);
            InsnList storeOpcodes = loadStoreOpcodes[0];
            InsnList loadOpcodes = loadStoreOpcodes[1];

            if (cast) targetMethod.instructions.insert(methodInsnNode, new TypeInsnNode(Opcodes.CHECKCAST, Type.getReturnType(methodInsnNode.desc).getInternalName()));
            if (!Modifier.isStatic(transformerMethod.access)) {
                targetMethod.instructions.insertBefore(methodInsnNode, storeOpcodes);
                targetMethod.instructions.insertBefore(methodInsnNode, new VarInsnNode(Opcodes.ALOAD, 0));
                targetMethod.instructions.insertBefore(methodInsnNode, loadOpcodes);
                targetMethod.instructions.set(methodInsnNode, new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc));
            } else {
                targetMethod.instructions.set(methodInsnNode, new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access)));
            }
        }
    }

    private InsnList[] getLoadStoreOpcodes(final String owner, final String desc, int freeVarIndex) {
        InsnList storeOpcodes = new InsnList();
        InsnList loadOpcodes = new InsnList();

        if (owner != null) {
            Type ownerType = Type.getObjectType(owner);
            storeOpcodes.add(new VarInsnNode(ASMUtils.getStoreOpcode(ownerType), freeVarIndex));
            loadOpcodes.add(new VarInsnNode(ASMUtils.getLoadOpcode(ownerType), freeVarIndex));
            freeVarIndex += ownerType.getSize();
        }

        Type[] argumentTypes = Type.getArgumentTypes(desc);
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
