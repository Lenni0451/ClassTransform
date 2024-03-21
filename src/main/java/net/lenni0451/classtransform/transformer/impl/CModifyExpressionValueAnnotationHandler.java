package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
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
import java.util.*;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyExpressionValue} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyExpressionValueAnnotationHandler extends RemovingTargetAnnotationHandler<CModifyExpressionValue> {

    public CModifyExpressionValueAnnotationHandler() {
        super(CModifyExpressionValue.class, CModifyExpressionValue::method);
    }

    @Override
    public void transform(CModifyExpressionValue annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        IInjectionTarget iInjectionTarget = injectionTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));

        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }

        List<AbstractInsnNode> injectionInstructions = iInjectionTarget.getTargets(injectionTargets, target, annotation.target(), annotation.slice());
        if (injectionInstructions == null) {
            throw new TransformerException(transformerMethod, transformer, "has invalid member declaration '" + annotation.target().target() + "'")
                    .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
        }
        if (injectionInstructions.isEmpty() && !annotation.target().optional()) {
            throw new TransformerException(transformerMethod, transformer, "target '" + annotation.target().target() + "' could not be found")
                    .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
        }

        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyExpressionValue");
        for (AbstractInsnNode injectionInstruction : injectionInstructions) {
            if (injectionInstruction instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) injectionInstruction;
                if (injectionInstruction.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) injectionInstruction).name.equals("<init>")) {
                    this.modifyNew(transformedClass, target, transformer, transformerMethod, methodInsnNode, transformerMethodCalls);
                } else {
                    this.modifyInvoke(transformedClass, target, transformer, transformerMethod, methodInsnNode, transformerMethodCalls);
                }
            } else if (injectionInstruction instanceof FieldInsnNode) {
                this.modifyField(transformedClass, target, transformer, transformerMethod, (FieldInsnNode) injectionInstruction, transformerMethodCalls);
            } else {
                throw new InvalidTargetException(transformerMethod, transformer, annotation.target().value(), Arrays.asList("INVOKE", "FIELD", "NEW"));
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

    private void modifyInvoke(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final MethodInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = returnType(targetNode);
        if (wrappedType.equals(Type.VOID_TYPE)) throw new TransformerException(transformerMethod, transformer, "target method returns void");
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        targetMethod.instructions.insert(targetNode, insns);
    }

    private void modifyField(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final FieldInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = type(targetNode.desc);
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        if (targetNode.getOpcode() == Opcodes.PUTFIELD || targetNode.getOpcode() == Opcodes.PUTSTATIC) {
            targetMethod.instructions.insertBefore(targetNode, insns);
        } else {
            targetMethod.instructions.insert(targetNode, insns);
        }
    }

    private void modifyNew(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final MethodInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = type(targetNode.owner);
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        targetMethod.instructions.insert(targetNode, insns);
    }

    private boolean checkDescriptor(final ClassNode transformer, final MethodNode transformerMethod, final Type expectedType) {
        Type returnType = returnType(transformerMethod);
        Type[] argumentTypes = argumentTypes(transformerMethod);
        if (argumentTypes.length != 1) {
            throw new TransformerException(transformerMethod, transformer, "does not have exactly one argument")
                    .help(Codifier.of(transformerMethod).returnType(expectedType).params(null, expectedType));
        }
        if (!ASMUtils.compareType(expectedType, argumentTypes[0])) {
            throw new TransformerException(transformerMethod, transformer, "does not have the right argument type")
                    .help(Codifier.of(transformerMethod).returnType(expectedType).params(null, expectedType));
        }
        if (!ASMUtils.compareType(expectedType, returnType)) {
            throw new TransformerException(transformerMethod, transformer, "does not have the right return type")
                    .help(Codifier.of(transformerMethod).returnType(expectedType).params(null, expectedType));
        }
        return !returnType.equals(expectedType);
    }

    private InsnList getModifyInstructions(final ClassNode targetClass, final MethodNode targetMethod, final MethodNode transformerMethod, final Type wrappedType, final boolean castReturnValue, final List<MethodInsnNode> transformerMethodCalls) {
        InsnList insns = new InsnList();
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(targetMethod.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insns.add(ASMUtils.swap(wrappedType, T_Object));
            transformerCall = new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc);
        }
        insns.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
        if (castReturnValue) insns.add(ASMUtils.getCast(wrappedType));
        return insns;
    }

}
