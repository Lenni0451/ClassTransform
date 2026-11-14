package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.InjectionUtils;
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

        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }

        List<CTarget> targets = new ArrayList<>();
        if (!annotation.target().value().isEmpty()) targets.add(annotation.target());
        Collections.addAll(targets, annotation.targets());
        if (targets.isEmpty()) throw new TransformerException(transformerMethod, transformer, "does not have any expression targets");
        Set<AbstractInsnNode> injectionInstructions = new LinkedHashSet<>();
        for (CTarget expressionTarget : targets) {
            IInjectionTarget injectionTarget = injectionTargets.get(expressionTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, expressionTarget.value(), injectionTargets.keySet());
            List<AbstractInsnNode> instructions = injectionTarget.getTargets(injectionTargets, target, expressionTarget, annotation.slice());
            if (instructions == null) throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, expressionTarget.target());
            if (instructions.isEmpty() && !expressionTarget.optional()) {
                throw new TransformerException(transformerMethod, transformer, "target '" + expressionTarget.target() + "' could not be found")
                        .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
            }
            injectionInstructions.addAll(instructions);
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
                this.modifyConstant(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

    private void modifyInvoke(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final MethodInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = returnType(targetNode);
        if (wrappedType.equals(Type.VOID_TYPE)) throw new TransformerException(transformerMethod, transformer, "target method returns void");
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        targetMethod.instructions.insert(targetNode, insns);
    }

    private void modifyField(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final FieldInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = type(targetNode.desc);
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        if (targetNode.getOpcode() == Opcodes.PUTFIELD || targetNode.getOpcode() == Opcodes.PUTSTATIC) {
            targetMethod.instructions.insertBefore(targetNode, insns);
        } else {
            targetMethod.instructions.insert(targetNode, insns);
        }
    }

    private void modifyNew(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final MethodInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = type(targetNode.owner);
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        targetMethod.instructions.insert(targetNode, insns);
    }

    private void modifyConstant(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final AbstractInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType;
        Number number = ASMUtils.getNumber(targetNode);
        if (targetNode.getOpcode() == Opcodes.ACONST_NULL) {
            wrappedType = returnType(transformerMethod);
            if (wrappedType.getSort() != Type.OBJECT && wrappedType.getSort() != Type.ARRAY) {
                throw new TransformerException(transformerMethod, transformer, "must return a reference type when modifying null");
            }
        } else if (number instanceof Long) {
            wrappedType = Type.LONG_TYPE;
        } else if (number instanceof Float) {
            wrappedType = Type.FLOAT_TYPE;
        } else if (number instanceof Double) {
            wrappedType = Type.DOUBLE_TYPE;
        } else if (number != null) {
            wrappedType = Type.INT_TYPE;
            Type handlerType = returnType(transformerMethod);
            if (handlerType.getSort() >= Type.BOOLEAN && handlerType.getSort() <= Type.INT) wrappedType = handlerType;
        } else if (targetNode instanceof LdcInsnNode && ((LdcInsnNode) targetNode).cst instanceof String) {
            wrappedType = type(String.class);
        } else if (targetNode instanceof LdcInsnNode && ((LdcInsnNode) targetNode).cst instanceof Type && ((Type) ((LdcInsnNode) targetNode).cst).getSort() != Type.METHOD) {
            wrappedType = type(Class.class);
        } else {
            throw new TransformerException(transformerMethod, transformer, "target must be a method invocation, field access, object creation or constant");
        }
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        targetMethod.instructions.insert(targetNode, this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls));
    }

    private boolean checkDescriptor(final ClassNode transformer, final MethodNode transformerMethod, final MethodNode targetMethod, final Type expectedType) {
        Type returnType = returnType(transformerMethod);
        InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, targetMethod, expectedType);
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
            transformerCall = new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
        }
        insns.add(InjectionUtils.loadCapturedArguments(targetMethod, argumentTypes(transformerMethod).length - 1));
        insns.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
        if (castReturnValue) insns.add(ASMUtils.getCast(wrappedType));
        return insns;
    }

}
