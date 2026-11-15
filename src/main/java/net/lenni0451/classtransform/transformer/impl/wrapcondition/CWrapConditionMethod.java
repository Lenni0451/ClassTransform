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

/**
 * Condition wrapper for method invocations.
 * <p>
 * The condition is evaluated before the invocation. When it returns {@code false}, the invocation is skipped and, for a non-{@code void} return type, the default value of that type is pushed instead. The transformer method receives the invocation instance for non-static invocations followed by the invocation arguments; captured arguments of the enclosing method are appended after these arguments. Constructors are not supported.
 */
public class CWrapConditionMethod implements IWrapConditionTarget<MethodInsnNode> {

    @Override
    public MethodInsnNode inject(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, MethodInsnNode insnNode) {
        return this.inject(transformedClass, transformer, transformerMethod, target, insnNode, false, Type.VOID_TYPE);
    }

    /**
     * Wraps an invocation and optionally replaces a skipped non-{@code void} result with a default value.
     *
     * @param transformedClass  The transformed class
     * @param transformer       The transformer class
     * @param transformerMethod The copied transformer method returning the condition
     * @param target            The method containing the invocation
     * @param insnNode          The invocation to wrap
     * @param pushDummy         Whether to push the default value when the invocation is skipped
     * @param returnType        The return type of the invocation, used for the default value
     * @return The inserted call to the transformer method
     */
    public MethodInsnNode inject(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final MethodInsnNode insnNode, final boolean pushDummy, final Type returnType) {
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
        if (pushDummy) {
            LabelNode after = new LabelNode();
            InsnList afterInsns = new InsnList();
            afterInsns.add(new JumpInsnNode(Opcodes.GOTO, after));
            afterInsns.add(end);
            afterInsns.add(ASMUtils.getDefaultValue(returnType));
            afterInsns.add(after);
            target.instructions.insert(insnNode, afterInsns);
        } else {
            target.instructions.insert(insnNode, end);
        }
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
