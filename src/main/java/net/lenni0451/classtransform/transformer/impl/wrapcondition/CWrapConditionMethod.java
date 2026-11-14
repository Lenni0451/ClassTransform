package net.lenni0451.classtransform.transformer.impl.wrapcondition;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.InjectionUtils;
import net.lenni0451.classtransform.transformer.InjectionPlan;
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
        int capturedArguments = 0;

        if (hasArgs) {
            Type[] expectedArgs = this.getOperationArguments(InjectionPlan.original(insnNode));
            capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, expectedArgs);
        }

        int instanceIndex = ASMUtils.getFreeVarIndex(target);
        int[] argIndices = new int[argumentTypes.length];
        int nextIndex = instanceIndex + (isStatic ? 0 : 1);
        for (int i = 0; i < argumentTypes.length; i++) {
            argIndices[i] = nextIndex;
            nextIndex += argumentTypes[i].getSize();
        }
        LabelNode end = new LabelNode();
        InsnList insns = new InsnList();
        for (int i = argumentTypes.length - 1; i >= 0; i--) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ISTORE), argIndices[i]));
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ASTORE, instanceIndex));
        if (!Modifier.isStatic(target.access)) insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (hasArgs) {
            if (!isStatic && InjectionPlan.original(insnNode) == insnNode) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
            for (int i = 0; i < argumentTypes.length; i++) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ILOAD), argIndices[i]));
        }
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(target.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        }
        insns.add(InjectionUtils.loadCapturedArguments(target, capturedArguments));
        insns.add(transformerCall);
        insns.add(new JumpInsnNode(Opcodes.IFEQ, end));
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
        for (int i = 0; i < argumentTypes.length; i++) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ILOAD), argIndices[i]));

        target.instructions.insertBefore(insnNode, insns);
        target.instructions.insert(insnNode, end);
        return transformerCall;
    }

    private Type[] getOperationArguments(final AbstractInsnNode instruction) {
        if (instruction instanceof FieldInsnNode) {
            FieldInsnNode field = (FieldInsnNode) instruction;
            if (field.getOpcode() == Opcodes.PUTSTATIC) return new Type[]{type(field.desc)};
            return new Type[]{type(field.owner), type(field.desc)};
        }
        MethodInsnNode method = (MethodInsnNode) instruction;
        Type[] arguments = argumentTypes(method.desc);
        if (method.getOpcode() == Opcodes.INVOKESTATIC) return arguments;
        Type[] result = new Type[arguments.length + 1];
        result[0] = type(method.owner);
        System.arraycopy(arguments, 0, result, 1, arguments.length);
        return result;
    }

}
