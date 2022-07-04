package net.lenni0451.classtransform.transformer.impl.credirect;

import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.List;

public class CRedirectGetField implements IRedirectTarget {

    @Override
    public void inject(ClassNode targetClass, MethodNode targetMethod, ClassNode transformer, MethodNode transformerMethod, List<AbstractInsnNode> targetNodes) {
        for (AbstractInsnNode instruction : targetNodes) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;

            Type returnType = Type.getReturnType(transformerMethod.desc);
            Type[] argumentTypes = Type.getArgumentTypes(transformerMethod.desc);
            Type originalType = Type.getType(fieldInsnNode.desc);
            Type originalOwnerType = Type.getObjectType(fieldInsnNode.owner);
            if (!ASMUtils.compareType(originalType, returnType)) {
                throw new TransformerException(transformerMethod, transformer, "does not have same return type as field")
                        .help(Codifier.of(transformerMethod).returnType(originalType));
            }
            if (fieldInsnNode.getOpcode() != Opcodes.GETSTATIC) {
                if (!ASMUtils.compareTypes(new Type[]{originalOwnerType}, argumentTypes)) {
                    throw new TransformerException(transformerMethod, transformer, "does not have first argument type as field owner")
                            .help(Codifier.of(transformerMethod).param(null).param(originalOwnerType));
                }
            } else {
                if (argumentTypes.length != 0) {
                    throw new TransformerException(transformerMethod, transformer, "does not have no arguments")
                            .help(Codifier.of(transformerMethod).param(null));
                }
            }

            if (!Modifier.isStatic(transformerMethod.access)) {
                targetMethod.instructions.insertBefore(fieldInsnNode, new VarInsnNode(Opcodes.ALOAD, 0));
            }
            if (fieldInsnNode.getOpcode() != Opcodes.GETSTATIC && !Modifier.isStatic(transformerMethod.access)) {
                targetMethod.instructions.insertBefore(fieldInsnNode, new InsnNode(Opcodes.SWAP));
            }
            if (Modifier.isStatic(transformerMethod.access)) {
                targetMethod.instructions.set(fieldInsnNode, new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access)));
            } else {
                targetMethod.instructions.set(fieldInsnNode, new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc));
            }
            if (!originalType.equals(returnType)) {
                targetMethod.instructions.insert(fieldInsnNode, new TypeInsnNode(Opcodes.CHECKCAST, originalType.getInternalName()));
            }
        }
    }

}
