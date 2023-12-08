package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CTargetImpl;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
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
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (String wrapTarget : annotation.target()) {
            MemberDeclaration member = ASMUtils.splitMemberDeclaration(wrapTarget);
            if (member == null) throw new TransformerException(transformerMethod, transformer, "Target is not a valid method/field declaration");
            if (copiedTransformerMethod == null) copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CWrapCondition");
            if (member.isFieldMapping()) {
                this.wrapField(annotation, transformerManager, transformedClass, transformer, copiedTransformerMethod, target, member, transformerMethodCalls);
            } else {
                this.wrapMethod(annotation, transformerManager, transformedClass, transformer, copiedTransformerMethod, target, member, transformerMethodCalls);
            }
        }
        if (copiedTransformerMethod == null) throw new TransformerException(transformerMethod, transformer, "No valid target found");
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

    private void wrapField(CWrapCondition annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, MemberDeclaration member, List<MethodInsnNode> transformerMethodCalls) {
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<AbstractInsnNode> targetInstructions = injectionTargets.get("PUTFIELD").getTargets(injectionTargets, target, CTargetImpl.putfield(member.toString(), annotation.ordinal()), annotation.slice());
        if (targetInstructions.isEmpty()) throw new TransformerException(transformerMethod, transformer, "No valid field target found");
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        for (AbstractInsnNode instruction : targetInstructions) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
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
            transformerMethodCalls.add(transformerCall);
            insns.add(transformerCall);
            insns.add(new JumpInsnNode(Opcodes.IFEQ, end));
            if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
            insns.add(new VarInsnNode(fieldType.getOpcode(Opcodes.ILOAD), valueIndex));

            target.instructions.insertBefore(instruction, insns);
            target.instructions.insert(instruction, end);
        }
    }

    private void wrapMethod(CWrapCondition annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target, MemberDeclaration member, List<MethodInsnNode> transformerMethodCalls) {
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<AbstractInsnNode> targetInstructions = injectionTargets.get("INVOKE").getTargets(injectionTargets, target, CTargetImpl.invoke(member.toString(), annotation.ordinal()), annotation.slice());
        if (targetInstructions.isEmpty()) throw new TransformerException(transformerMethod, transformer, "No valid method target found");
        boolean hasArgs = argumentTypes(transformerMethod).length > 0;
        for (AbstractInsnNode instruction : targetInstructions) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
            Type[] argumentTypes = argumentTypes(methodInsnNode.desc);
            boolean isStatic = methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC;
            if (!returnType(methodInsnNode.desc).equals(Type.VOID_TYPE)) throw new TransformerException(transformerMethod, transformer, "target must have void as return type");
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
            transformerMethodCalls.add(transformerCall);
            insns.add(transformerCall);
            insns.add(new JumpInsnNode(Opcodes.IFEQ, end));
            if (!isStatic) insns.add(new VarInsnNode(Opcodes.ALOAD, instanceIndex));
            for (int i = 0; i < argumentTypes.length; i++) insns.add(new VarInsnNode(argumentTypes[i].getOpcode(Opcodes.ILOAD), argIndices[i]));

            target.instructions.insertBefore(instruction, insns);
            target.instructions.insert(instruction, end);
        }
    }

}
