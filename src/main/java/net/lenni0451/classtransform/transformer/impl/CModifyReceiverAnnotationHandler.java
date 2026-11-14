package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyReceiver;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.InjectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;
import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyReceiver} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyReceiverAnnotationHandler extends InjectionAnnotationHandler<CModifyReceiver> {

    public CModifyReceiverAnnotationHandler() {
        super(CModifyReceiver.class, CModifyReceiver::method);
    }

    @Override
    public void transform(CModifyReceiver annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) && !Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, true);
        }
        if (annotation.target().length == 0) throw new TransformerException(transformerMethod, transformer, "does not have any receiver targets");

        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        Set<AbstractInsnNode> instructions = new LinkedHashSet<>();
        for (CTarget receiverTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(receiverTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, receiverTarget.value(), injectionTargets.keySet());
            List<AbstractInsnNode> targetInstructions = InjectionPlan.select(transformerManager, target, receiverTarget, annotation.slice());
            if (targetInstructions == null) throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, receiverTarget.target());
            for (AbstractInsnNode instruction : targetInstructions) {
                this.validateTarget(transformerManager, transformedClass, transformer, transformerMethod, instruction);
                instructions.add(instruction);
            }
        }

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyReceiver");
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (AbstractInsnNode instruction : instructions) {
            target.instructions.insertBefore(instruction, this.getModifyInstructions(transformedClass, target, transformer, transformerMethod, instruction, transformerMethodCalls));
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private void validateTarget(final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final AbstractInsnNode target) {
        if (InjectionPlan.original(target) != target) {
            throw new TransformerException(transformerMethod, transformer, "target was replaced by an earlier injection")
                    .help("Use a lower order for this receiver modifier");
        }
        switch (target.getOpcode()) {
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.GETFIELD:
            case Opcodes.PUTFIELD:
                return;
            case Opcodes.INVOKESPECIAL:
                MethodInsnNode methodInsnNode = (MethodInsnNode) target;
                if (methodInsnNode.name.equals("<init>")) throw new TransformerException(transformerMethod, transformer, "target must not be a constructor invocation");
                if (!methodInsnNode.owner.equals(transformedClass.name) && !this.isSuperType(transformerManager, transformedClass, transformer, transformerMethod, methodInsnNode.owner)) {
                    throw new TransformerException(transformerMethod, transformer, "target must be a call on the transformed class or one of its super types");
                }
                return;
            default:
                throw new TransformerException(transformerMethod, transformer, "target must be a non-static method call or field access");
        }
    }

    private boolean isSuperType(final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final String owner) {
        try {
            return transformerManager.getClassTree().getTreePart(transformerManager.getClassProvider(), transformedClass.name).getSuperClasses().contains(dot(owner));
        } catch (ClassNotFoundException e) {
            throw new TransformerException(transformerMethod, transformer, "unable to resolve the class hierarchy of '" + transformedClass.name + "'").cause(e);
        }
    }

    private InsnList getModifyInstructions(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final AbstractInsnNode targetNode, final List<MethodInsnNode> transformerMethodCalls) {
        Type receiverType;
        Type[] operationArguments;
        switch (targetNode.getOpcode()) {
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
                receiverType = type(((MethodInsnNode) targetNode).owner);
                operationArguments = argumentTypes(targetNode);
                break;
            case Opcodes.INVOKESPECIAL:
                receiverType = type(targetClass.name);
                operationArguments = argumentTypes(targetNode);
                break;
            case Opcodes.GETFIELD:
                receiverType = type(((FieldInsnNode) targetNode).owner);
                operationArguments = new Type[0];
                break;
            case Opcodes.PUTFIELD:
                receiverType = type(((FieldInsnNode) targetNode).owner);
                operationArguments = new Type[]{type(((FieldInsnNode) targetNode).desc)};
                break;
            default:
                throw new TransformerException(transformerMethod, transformer, "target must be a non-static method call or field access");
        }

        Type[] handlerArguments = new Type[operationArguments.length + 1];
        handlerArguments[0] = receiverType;
        System.arraycopy(operationArguments, 0, handlerArguments, 1, operationArguments.length);
        int capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, targetMethod, handlerArguments);
        Type handlerReturnType = returnType(transformerMethod);
        if (!ASMUtils.compareType(receiverType, handlerReturnType)) {
            throw new TransformerException(transformerMethod, transformer, "must accept and return the receiver type")
                    .help(Codifier.of(transformerMethod).returnType(receiverType).params(handlerArguments));
        }

        int[] indices = new int[handlerArguments.length];
        int nextIndex = ASMUtils.getFreeVarIndex(targetMethod);
        for (int i = 0; i < handlerArguments.length; i++) {
            indices[i] = nextIndex;
            nextIndex += handlerArguments[i].getSize();
        }

        InsnList insns = new InsnList();
        for (int i = handlerArguments.length - 1; i >= 0; i--) {
            insns.add(new VarInsnNode(ASMUtils.getStoreOpcode(handlerArguments[i]), indices[i]));
        }
        if (!Modifier.isStatic(transformerMethod.access)) insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        for (int i = 0; i < handlerArguments.length; i++) {
            insns.add(new VarInsnNode(ASMUtils.getLoadOpcode(handlerArguments[i]), indices[i]));
        }
        insns.add(InjectionUtils.loadCapturedArguments(targetMethod, capturedArguments));
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(transformerMethod.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
        }
        insns.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
        if (!receiverType.equals(handlerReturnType)) insns.add(ASMUtils.getCast(receiverType));
        for (int i = 1; i < handlerArguments.length; i++) {
            insns.add(new VarInsnNode(ASMUtils.getLoadOpcode(handlerArguments[i]), indices[i]));
        }
        return insns;
    }

}
