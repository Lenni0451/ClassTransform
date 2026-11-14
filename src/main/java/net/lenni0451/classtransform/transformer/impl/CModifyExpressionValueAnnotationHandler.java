package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.expression.ExpressionMatch;
import net.lenni0451.classtransform.expression.ExpressionTypes;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoerceUtils;
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
public class CModifyExpressionValueAnnotationHandler extends InjectionAnnotationHandler<CModifyExpressionValue> {

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
            List<AbstractInsnNode> instructions = InjectionPlan.select(transformerManager, target, expressionTarget, annotation.slice());
            if (instructions == null) throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, expressionTarget.target());
            injectionInstructions.addAll(instructions);
        }

        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyExpressionValue");
        for (AbstractInsnNode injectionInstruction : injectionInstructions) {
            ExpressionMatch expressionMatch = InjectionPlan.getExpressionMatch(injectionInstruction);
            if (expressionMatch != null) {
                Type wrappedType = expressionMatch.getValueType();
                if (wrappedType.equals(ExpressionTypes.INT_LIKE_TYPE)) {
                    wrappedType = returnType(transformerMethod);
                    if (!ExpressionTypes.isIntLike(wrappedType)) {
                        throw new TransformerException(transformerMethod, transformer, "must return an int-like type when modifying an int-like expression");
                    }
                }
                if (!wrappedType.equals(Type.VOID_TYPE)) {
                    boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, target, wrappedType);
                    boolean insertedDup = false;
                    if (injectionInstruction instanceof MethodInsnNode && ((MethodInsnNode) injectionInstruction).name.equals("<init>")) {
                        insertedDup = this.ensureNewDup(target, (MethodInsnNode) injectionInstruction);
                    }
                    InsnList insns = this.getModifyInstructions(transformedClass, target, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
                    if (insertedDup) insns.add(new InsnNode(Opcodes.POP));
                    this.insertAfter(target, injectionInstruction, insns);
                }
                continue;
            }
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
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private void modifyInvoke(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final MethodInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = returnType(targetNode);
        AbstractInsnNode original = InjectionPlan.original(targetNode);
        if (original instanceof MethodInsnNode) {
            MethodInsnNode method = (MethodInsnNode) original;
            wrappedType = method.name.equals("<init>") ? type(method.owner) : returnType(method);
        } else if (original instanceof FieldInsnNode) {
            wrappedType = type(((FieldInsnNode) original).desc);
        }
        if (wrappedType.equals(Type.VOID_TYPE)) throw new TransformerException(transformerMethod, transformer, "target method returns void");
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        this.insertAfter(targetMethod, targetNode, insns);
    }

    private void modifyField(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final FieldInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = type(targetNode.desc);
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        if (targetNode.getOpcode() == Opcodes.PUTFIELD || targetNode.getOpcode() == Opcodes.PUTSTATIC) {
            targetMethod.instructions.insertBefore(targetNode, insns);
        } else {
            this.insertAfter(targetMethod, targetNode, insns);
        }
    }

    private void modifyNew(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final MethodInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type wrappedType = type(targetNode.owner);
        boolean castReturnValue = this.checkDescriptor(transformer, transformerMethod, targetMethod, wrappedType);
        boolean insertedDup = this.ensureNewDup(targetMethod, targetNode);
        InsnList insns = this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls);
        if (insertedDup) insns.add(new InsnNode(Opcodes.POP));
        this.insertAfter(targetMethod, targetNode, insns);
    }

    private boolean ensureNewDup(final MethodNode targetMethod, final MethodInsnNode initNode) {
        AbstractInsnNode instruction = initNode.getPrevious();
        while (instruction != null) {
            if (instruction instanceof TypeInsnNode && instruction.getOpcode() == Opcodes.NEW && ((TypeInsnNode) instruction).desc.equals(initNode.owner)) {
                AbstractInsnNode next = instruction.getNext();
                while (next != null && next.getOpcode() < 0) next = next.getNext();
                if (next != null && next.getOpcode() == Opcodes.DUP) return false;
                targetMethod.instructions.insert(instruction, new InsnNode(Opcodes.DUP));
                return true;
            }
            instruction = instruction.getPrevious();
        }
        return false;
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
        this.insertAfter(targetMethod, targetNode, this.getModifyInstructions(targetClass, targetMethod, transformerMethod, wrappedType, castReturnValue, transformerMethodCalls));
    }

    private void insertAfter(final MethodNode method, final AbstractInsnNode target, final InsnList instructions) {
        LabelNode end = new LabelNode();
        instructions.add(end);
        method.instructions.insert(InjectionPlan.after(target), instructions);
        InjectionPlan.setAfter(target, end);
    }

    private boolean checkDescriptor(final ClassNode transformer, final MethodNode transformerMethod, final MethodNode targetMethod, final Type expectedType) {
        Type returnType = returnType(transformerMethod);
        InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, targetMethod, expectedType);
        if (!CoerceUtils.canCoerceReturn(expectedType, returnType, CoerceUtils.hasCoerceReturn(transformerMethod))) {
            throw new TransformerException(transformerMethod, transformer, "does not have the right return type")
                    .help(Codifier.of(transformerMethod).returnType(expectedType).params(null, expectedType));
        }
        return !returnType.equals(expectedType) && expectedType.getSort() >= Type.ARRAY;
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
