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

public class CWrapConditionField implements IWrapConditionTarget<FieldInsnNode> {

    @Override
    public MethodInsnNode inject(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, FieldInsnNode insnNode) {
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        Type fieldType = type(insnNode.desc);
        boolean isStatic = insnNode.getOpcode() == Opcodes.PUTSTATIC;
        if (hasArgs) {
            Type[] expectedArgs;
            if (isStatic) expectedArgs = new Type[]{fieldType};
            else expectedArgs = new Type[]{type(insnNode.owner), fieldType};
            if (!ASMUtils.compareTypes(expectedArgs, argumentTypes(transformerMethod))) {
                String message = "must have ";
                if (expectedArgs.length == 1) message += "the new value for the field";
                else message += "the instance of the field owner and the new value for the field";
                throw new TransformerException(transformerMethod, transformer, message + " as arguments")
                        .help(Codifier.of(transformerMethod).param(null).params(expectedArgs));
            }
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
            transformerCall = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc);
        }
        insns.add(transformerCall);
        insns.add(new JumpInsnNode(Opcodes.IFEQ, end));
        if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
        insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ILOAD), valueIndex));

        target.instructions.insertBefore(insnNode, insns);
        target.instructions.insert(insnNode, end);
        return transformerCall;
    }

}
