package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CWrapCondition} annotation.
 */
@ParametersAreNonnullByDefault
public class CWrapConditionAnnotationHandler extends RemovingTargetAnnotationHandler<CWrapCondition> {

    public CWrapConditionAnnotationHandler() {
        super(CWrapCondition.class, CWrapCondition::method);
    }

    @Override
    public void transform(CWrapCondition annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        if (!returnType(transformerMethod).equals(Type.BOOLEAN_TYPE)) throw new TransformerException(transformerMethod, transformer, "must return 'boolean'");

        MethodNode copiedTransformerMethod = null;
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (CTarget wrapTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(wrapTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, wrapTarget.target(), injectionTargets.keySet());
            List<AbstractInsnNode> targetInstructions = injectionTarget.getTargets(injectionTargets, target, wrapTarget, annotation.slice());
            if (targetInstructions.isEmpty()) throw new TransformerException(transformerMethod, transformer, "No valid method/field target found");

            if (copiedTransformerMethod == null) copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CWrapCondition");
            for (AbstractInsnNode targetInstruction : targetInstructions) {
                if (targetInstruction instanceof FieldInsnNode) {
                    if (targetInstruction.getOpcode() != Opcodes.PUTSTATIC && targetInstruction.getOpcode() != Opcodes.PUTFIELD) {
                        throw new TransformerException(transformerMethod, transformer, "target must be a field getter");
                    }
                    transformerMethodCalls.add(this.wrapField(transformedClass, transformer, copiedTransformerMethod, target, (FieldInsnNode) targetInstruction));
                } else if (targetInstruction instanceof MethodInsnNode) {
                    if (!returnType(((MethodInsnNode) targetInstruction).desc).equals(Type.VOID_TYPE)) {
                        throw new TransformerException(transformerMethod, transformer, "target method must have void as return type");
                    }
                    transformerMethodCalls.add(this.wrapMethod(transformedClass, transformer, copiedTransformerMethod, target, (MethodInsnNode) targetInstruction));
                } else {
                    throw new TransformerException(transformerMethod, transformer, "Target is not a valid method/field invocation");
                }
            }
        }
        if (copiedTransformerMethod == null) throw new TransformerException(transformerMethod, transformer, "No valid target found");
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

    private MethodInsnNode wrapField(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, FieldInsnNode fieldInsnNode) {
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        Type fieldType = type(fieldInsnNode.desc);
        boolean isStatic = fieldInsnNode.getOpcode() == Opcodes.PUTSTATIC;
        if (hasArgs) {
            Type[] expectedArgs;
            if (isStatic) expectedArgs = new Type[]{fieldType};
            else expectedArgs = new Type[]{type(fieldInsnNode.owner), fieldType};
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

        target.instructions.insertBefore(fieldInsnNode, insns);
        target.instructions.insert(fieldInsnNode, end);
        return transformerCall;
    }

    private MethodInsnNode wrapMethod(ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, MethodInsnNode methodInsnNode) {
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        Type[] argumentTypes = argumentTypes(methodInsnNode.desc);
        boolean isStatic = methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC;

        if (hasArgs) {
            Type[] expectedArgs;
            if (isStatic) {
                expectedArgs = argumentTypes;
            } else {
                expectedArgs = new Type[argumentTypes.length + 1];
                expectedArgs[0] = type(methodInsnNode.owner);
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

        target.instructions.insertBefore(methodInsnNode, insns);
        target.instructions.insert(methodInsnNode, end);
        return transformerCall;
    }

}
