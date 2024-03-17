package net.lenni0451.classtransform.transformer.impl.wrapcondition;

import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.type;

public class CWrapConditionMethod implements IWrapConditionTarget<MethodInsnNode> {

    @Override
    public MethodInsnNode inject(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, MethodInsnNode insnNode) {
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        Type[] argumentTypes = argumentTypes(insnNode.desc);
        boolean isStatic = insnNode.getOpcode() == Opcodes.INVOKESTATIC;

        if (hasArgs) {
            Type[] expectedArgs;
            if (isStatic) {
                expectedArgs = argumentTypes;
            } else {
                expectedArgs = new Type[argumentTypes.length + 1];
                expectedArgs[0] = type(insnNode.owner);
                System.arraycopy(argumentTypes, 0, expectedArgs, 1, argumentTypes.length);
            }
            if (!ASMUtils.compareTypes(expectedArgs, argumentTypes(transformerMethod))) {
                String message = "must have ";
                if (expectedArgs.length == 1) message += "the arguments of the target method";
                else message += "the instance of the method owner and the arguments of the target method";
                throw new TransformerException(transformerMethod, transformer, message + " as arguments")
                        .help(Codifier.of(transformerMethod).param(null).params(expectedArgs));
            }
        }

        int instanceIndex = ASMUtils.getFreeVarIndex(target);
        int[] argIndices = new int[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; i++) {
            int last = i == 0 ? instanceIndex : argIndices[i - 1];
            argIndices[i] = last + argumentTypes[i].getSize();
        }
        LabelNode end = new LabelNode();
        InsnList insns = new InsnList();
        for (int i = argumentTypes.length - 1; i >= 0; i--) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ISTORE), argIndices[i]));
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ASTORE, instanceIndex));
        if (!Modifier.isStatic(target.access)) insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (hasArgs) {
            if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
            for (int i = 0; i < argumentTypes.length; i++) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ILOAD), argIndices[i]));
        }
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(target.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc);
        }
        insns.add(transformerCall);
        insns.add(new JumpInsnNode(Opcodes.IFEQ, end));
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
        for (int i = 0; i < argumentTypes.length; i++) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ILOAD), argIndices[i]));

        target.instructions.insertBefore(insnNode, insns);
        target.instructions.insert(insnNode, end);
        return transformerCall;
    }

}
