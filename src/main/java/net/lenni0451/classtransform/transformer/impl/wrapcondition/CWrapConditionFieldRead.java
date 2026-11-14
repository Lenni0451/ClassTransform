package net.lenni0451.classtransform.transformer.impl.wrapcondition;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.InjectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.type;

public class CWrapConditionFieldRead implements IWrapConditionTarget<FieldInsnNode> {

    @Override
    public MethodInsnNode inject(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, FieldInsnNode insnNode) {
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        Type fieldType = type(insnNode.desc);
        boolean isStatic = insnNode.getOpcode() == Opcodes.GETSTATIC;
        int capturedArguments = 0;
        if (hasArgs) {
            Type[] expectedArgs = isStatic ? new Type[0] : new Type[]{type(insnNode.owner)};
            capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, expectedArgs);
        }

        int instanceIndex = ASMUtils.getFreeVarIndex(target);
        LabelNode end = new LabelNode();
        InsnList insns = new InsnList();
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ASTORE, instanceIndex));
        if (!Modifier.isStatic(target.access)) insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (hasArgs && !isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
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

        target.instructions.insertBefore(insnNode, insns);
        LabelNode after = new LabelNode();
        InsnList afterInsns = new InsnList();
        afterInsns.add(new JumpInsnNode(Opcodes.GOTO, after));
        afterInsns.add(end);
        afterInsns.add(ASMUtils.getDefaultValue(fieldType));
        afterInsns.add(after);
        target.instructions.insert(insnNode, afterInsns);
        return transformerCall;
    }

}
