package net.lenni0451.classtransform.transformer.impl.wrapcondition;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.InjectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.type;

/**
 * Condition wrapper for field writes.
 * <p>
 * The written value and, for instance fields, the field owner are stored in free local variables before the condition is evaluated. The field is only written when the condition returns {@code true}. The transformer method receives the field owner followed by the new value for instance fields, or only the value for static fields; captured arguments of the enclosing method are appended after these arguments.
 */
public class CWrapConditionField implements IWrapConditionTarget<FieldInsnNode> {

    @Override
    public MethodInsnNode inject(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, FieldInsnNode insnNode) {
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        Type fieldType = type(insnNode.desc);
        boolean isStatic = insnNode.getOpcode() == Opcodes.PUTSTATIC;
        int capturedArguments = 0;
        if (hasArgs) {
            Type[] expectedArgs;
            if (isStatic) expectedArgs = new Type[]{fieldType};
            else expectedArgs = new Type[]{type(insnNode.owner), fieldType};
            capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, expectedArgs);
        }

        int instanceIndex = ASMUtils.getFreeVarIndex(target);
        int valueIndex = instanceIndex + (isStatic ? 0 : 1);
        LabelNode end = new LabelNode();
        InsnList insns = new InsnList();
        insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ISTORE), valueIndex));
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ASTORE, instanceIndex));
        if (!Modifier.isStatic(target.access)) insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (hasArgs) {
            if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
            insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ILOAD), valueIndex));
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
        insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ILOAD), valueIndex));

        target.instructions.insertBefore(insnNode, insns);
        target.instructions.insert(insnNode, end);
        return transformerCall;
    }

}
