package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.operation.COperationRuntime;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoerceUtils;
import net.lenni0451.classtransform.utils.InjectionUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CWrapOperation} annotation.
 */
@ParametersAreNonnullByDefault
public class CWrapOperationAnnotationHandler extends InjectionAnnotationHandler<CWrapOperation> {

    private static final Handle LAMBDA_METAFACTORY = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
    );
    private static final Type COPERATION_TYPE = Type.getType(COperation.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);
    private static final Type SAM_TYPE = Type.getMethodType(OBJECT_TYPE, OBJECT_ARRAY_TYPE);

    public CWrapOperationAnnotationHandler() {
        super(CWrapOperation.class, CWrapOperation::method);
    }

    @Override
    public void transform(CWrapOperation annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) && !Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, true);
        }

        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        Map<AbstractInsnNode, Boolean> selected = new LinkedHashMap<>();
        for (CTarget wrapTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(wrapTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, wrapTarget.value(), injectionTargets.keySet());
            List<AbstractInsnNode> instructions = InjectionPlan.select(transformerManager, target, wrapTarget, annotation.slice());
            if (instructions == null) throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, wrapTarget.target());
            boolean newOperation = wrapTarget.value().equalsIgnoreCase("NEW");
            boolean expressionTarget = wrapTarget.value().equalsIgnoreCase("EXPRESSION");
            for (AbstractInsnNode instruction : instructions) {
                AbstractInsnNode original = InjectionPlan.original(instruction);
                boolean originalIsConstructor = original instanceof MethodInsnNode && ((MethodInsnNode) original).name.equals("<init>");
                if (newOperation && !originalIsConstructor) {
                    throw new TransformerException(transformerMethod, transformer, "NEW target must select an object creation");
                }
                if (!newOperation && originalIsConstructor && !expressionTarget) {
                    throw new TransformerException(transformerMethod, transformer, "constructor calls must be targeted with NEW");
                }
                selected.put(instruction, newOperation || expressionTarget && originalIsConstructor);
            }
        }

        List<WrapperPlan> wrapperPlans = new ArrayList<>();
        for (Map.Entry<AbstractInsnNode, Boolean> entry : selected.entrySet()) {
            AbstractInsnNode instruction = entry.getKey();
            boolean newOperation = entry.getValue();
            OperationSite site = (OperationSite) InjectionPlan.getOperationContext(instruction);
            if (site == null) {
                site = OperationSite.of(transformedClass, InjectionPlan.original(instruction), newOperation, InjectionPlan.getExpressionMatch(instruction));
                InjectionPlan.setOperationContext(instruction, site);
            } else if (site.isNewOperation() != newOperation) {
                throw new TransformerException(transformerMethod, transformer, "operation was already wrapped with a different operation type");
            }

            boolean hasExtraThis = false;
            Type[] currentArgs;
            if (instruction == site.getOriginal() && site.isNewOperation()) {
                currentArgs = site.getOriginalArgs();
            } else {
                OperationSite.OperationInfo currentInfo = OperationSite.describe(transformedClass, instruction);
                currentArgs = currentInfo.arguments;
                hasExtraThis = instruction != site.getOriginal() && instruction.getOpcode() != Opcodes.INVOKESTATIC;
            }

            Type[] requiredArguments = new Type[site.getOriginalArgs().length + 1];
            System.arraycopy(site.getOriginalArgs(), 0, requiredArguments, 0, site.getOriginalArgs().length);
            requiredArguments[requiredArguments.length - 1] = COPERATION_TYPE;
            int capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, requiredArguments);
            Type handlerReturnType = returnType(transformerMethod.desc);
            if (!CoerceUtils.canCoerceReturn(site.getReturnType(), handlerReturnType, CoerceUtils.hasCoerceReturn(transformerMethod))) {
                throw new TransformerException(transformerMethod, transformer, "must return the operation return type")
                        .help("Expected " + site.getReturnType().getClassName());
            }
            wrapperPlans.add(new WrapperPlan(instruction, site, currentArgs, hasExtraThis, capturedArguments, handlerReturnType));
        }

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CWrapOperation");
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (WrapperPlan plan : wrapperPlans) {
            this.wrap(transformedClass, target, transformer, copiedTransformerMethod, plan, transformerMethodCalls);
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private void wrap(final ClassNode targetClass, final MethodNode targetMethod, final ClassNode transformer, final MethodNode transformerMethod, final WrapperPlan plan, final List<MethodInsnNode> transformerMethodCalls) {
        Type[] currentArgs = plan.currentArgs;
        int[] indices = new int[currentArgs.length];
        int nextIndex = ASMUtils.getFreeVarIndex(targetMethod);
        for (int i = 0; i < currentArgs.length; i++) {
            indices[i] = nextIndex;
            nextIndex += currentArgs[i].getSize();
        }

        InsnList instructions = new InsnList();
        for (int i = currentArgs.length - 1; i >= 0; i--) {
            instructions.add(new VarInsnNode(ASMUtils.getStoreOpcode(currentArgs[i]), indices[i]));
        }
        if (plan.site.isNewOperation() && plan.instruction == plan.site.getOriginal()) {
            instructions.add(new InsnNode(Opcodes.POP2)); //Remove the uninitialized object and its duplicate
        }

        if (!Modifier.isStatic(transformerMethod.access)) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        int offset = plan.hasExtraThis ? 1 : 0;
        for (int i = 0; i < plan.site.getOriginalArgs().length; i++) {
            Type argument = plan.site.getOriginalArgs()[i];
            instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), indices[offset + i]));
        }

        int trailingStart = offset + plan.site.getOriginalArgs().length;
        Type[] trailingTypes = Arrays.copyOfRange(currentArgs, trailingStart, currentArgs.length);
        if (plan.hasExtraThis) {
            Type receiverType = Type.getObjectType(targetClass.name);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, indices[0]));
            if (!currentArgs[0].equals(receiverType)) instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, receiverType.getInternalName()));
        }
        for (int i = trailingStart; i < currentArgs.length; i++) {
            instructions.add(new VarInsnNode(currentArgs[i].getOpcode(Opcodes.ILOAD), indices[i]));
        }
        instructions.add(this.createOperation(targetClass, targetMethod, plan, trailingTypes, plan.hasExtraThis));

        instructions.add(InjectionUtils.loadCapturedArguments(targetMethod, plan.capturedArguments));
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(transformerMethod.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(targetClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(targetClass.access));
        }
        instructions.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
        if (!plan.site.getReturnType().equals(plan.handlerReturnType) && !plan.site.getReturnType().equals(Type.VOID_TYPE) && plan.site.getReturnType().getSort() >= Type.ARRAY) {
            instructions.add(ASMUtils.getCast(plan.site.getReturnType()));
        }

        targetMethod.instructions.insertBefore(plan.instruction, instructions);
        targetMethod.instructions.remove(plan.instruction);
        Object inheritedContext = InjectionPlan.getContext(plan.instruction);
        InjectionPlan.replace(plan.instruction, transformerCall);
        if (inheritedContext != null) InjectionPlan.setContext(transformerCall, inheritedContext);
    }

    private InvokeDynamicInsnNode createOperation(final ClassNode targetClass, final MethodNode targetMethod, final WrapperPlan plan, final Type[] trailingTypes, final boolean virtual) {
        MethodNode bridge = this.createBridge(targetClass, targetMethod, plan, trailingTypes, virtual);
        Type[] factoryArguments;
        int offset;
        if (virtual) {
            factoryArguments = new Type[trailingTypes.length + 1];
            factoryArguments[0] = Type.getObjectType(targetClass.name);
            offset = 1;
        } else {
            factoryArguments = new Type[trailingTypes.length];
            offset = 0;
        }
        System.arraycopy(trailingTypes, 0, factoryArguments, offset, trailingTypes.length);
        Handle implementation = new Handle(virtual ? Opcodes.H_INVOKESPECIAL : Opcodes.H_INVOKESTATIC, targetClass.name, bridge.name, bridge.desc, Modifier.isInterface(targetClass.access));
        return new InvokeDynamicInsnNode("call", Type.getMethodDescriptor(COPERATION_TYPE, factoryArguments), LAMBDA_METAFACTORY, SAM_TYPE, implementation, SAM_TYPE);
    }

    private MethodNode createBridge(final ClassNode targetClass, final MethodNode targetMethod, final WrapperPlan plan, final Type[] trailingTypes, final boolean virtual) {
        String name;
        int index = 0;
        do {
            name = "ct$operation$" + targetMethod.name.replaceAll("[^a-zA-Z0-9_$]", "_") + "$" + plan.site.nextBridgeIndex() + "$" + index++;
        } while (this.hasMethod(targetClass, name));

        Type[] descriptorArguments = new Type[trailingTypes.length + 1];
        System.arraycopy(trailingTypes, 0, descriptorArguments, 0, trailingTypes.length);
        descriptorArguments[descriptorArguments.length - 1] = OBJECT_ARRAY_TYPE;
        MethodNode bridge = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (virtual ? 0 : Opcodes.ACC_STATIC),
                name,
                Type.getMethodDescriptor(OBJECT_TYPE, descriptorArguments),
                null, null
        );

        InsnList instructions = bridge.instructions;
        int arrayIndex = (virtual ? 1 : 0) + this.getSize(trailingTypes);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
        instructions.add(ASMUtils.intPush(plan.site.getOriginalArgs().length));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(COperationRuntime.class), "checkArgumentCount", Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_ARRAY_TYPE, Type.INT_TYPE), false));

        if (virtual) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (plan.site.isNewOperation() && plan.instruction == plan.site.getOriginal()) {
            instructions.add(new TypeInsnNode(Opcodes.NEW, plan.site.getNewOwner()));
            instructions.add(new InsnNode(Opcodes.DUP));
        }
        for (int i = 0; i < plan.site.getOriginalArgs().length; i++) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
            instructions.add(ASMUtils.intPush(i));
            instructions.add(new InsnNode(Opcodes.AALOAD));
            instructions.add(ASMUtils.getCast(plan.site.getOriginalArgs()[i]));
        }
        int trailingIndex = virtual ? 1 : 0;
        for (Type trailingType : trailingTypes) {
            instructions.add(new VarInsnNode(trailingType.getOpcode(Opcodes.ILOAD), trailingIndex));
            trailingIndex += trailingType.getSize();
        }

        if (plan.site.isNewOperation() && plan.instruction == plan.site.getOriginal()) {
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, plan.site.getNewOwner(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, plan.site.getOriginalArgs()), false));
        } else {
            AbstractInsnNode current = plan.instruction;
            if (current instanceof MethodInsnNode) {
                MethodInsnNode method = (MethodInsnNode) current;
                instructions.add(new MethodInsnNode(method.getOpcode(), method.owner, method.name, method.desc, method.itf));
            } else if (current instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) current;
                instructions.add(new FieldInsnNode(field.getOpcode(), field.owner, field.name, field.desc));
            } else if (current.getOpcode() == Opcodes.INSTANCEOF) {
                instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, ((TypeInsnNode) current).desc));
            } else {
                instructions.add(current.clone(Collections.emptyMap()));
            }
        }

        if (plan.site.getReturnType().equals(Type.VOID_TYPE)) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            AbstractInsnNode box = ASMUtils.getPrimitiveToObject(plan.site.getReturnType());
            if (box != null) instructions.add(box);
        }
        instructions.add(new InsnNode(Opcodes.ARETURN));
        targetClass.methods.add(bridge);
        return bridge;
    }

    private int getSize(final Type[] types) {
        int size = 0;
        for (Type type : types) size += type.getSize();
        return size;
    }

    private boolean hasMethod(final ClassNode classNode, final String name) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name)) return true;
        }
        return false;
    }


    private static class WrapperPlan {
        private final AbstractInsnNode instruction;
        private final OperationSite site;
        private final Type[] currentArgs;
        private final boolean hasExtraThis;
        private final int capturedArguments;
        private final Type handlerReturnType;

        private WrapperPlan(final AbstractInsnNode instruction, final OperationSite site, final Type[] currentArgs, final boolean hasExtraThis, final int capturedArguments, final Type handlerReturnType) {
            this.instruction = instruction;
            this.site = site;
            this.currentArgs = currentArgs;
            this.hasExtraThis = hasExtraThis;
            this.capturedArguments = capturedArguments;
            this.handlerReturnType = handlerReturnType;
        }
    }

}
