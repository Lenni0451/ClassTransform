package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.annotations.injection.CWrapMethod;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.operation.COperationRuntime;
import net.lenni0451.classtransform.transformer.IAnnotationHandlerFinalizer;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.attributes.SharedVariableAttribute;
import net.lenni0451.classtransform.utils.local.CLocalBooleanRef;
import net.lenni0451.classtransform.utils.local.CLocalByteRef;
import net.lenni0451.classtransform.utils.local.CLocalCharRef;
import net.lenni0451.classtransform.utils.local.CLocalDoubleRef;
import net.lenni0451.classtransform.utils.local.CLocalFloatRef;
import net.lenni0451.classtransform.utils.local.CLocalIntRef;
import net.lenni0451.classtransform.utils.local.CLocalLongRef;
import net.lenni0451.classtransform.utils.local.CLocalRef;
import net.lenni0451.classtransform.utils.local.CLocalShortRef;
import net.lenni0451.classtransform.utils.local.LocalRefUtils;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.returnType;

/**
 * The annotation handler for the {@link CWrapMethod} annotation.<br>
 * The original method body is moved to a private synthetic method after all other injections ran.
 * Every wrapper then gets a {@link COperation} which calls the next level of the wrapper chain.
 */
@ParametersAreNonnullByDefault
public class CWrapMethodAnnotationHandler extends RemovingTargetAnnotationHandler<CWrapMethod> implements IAnnotationHandlerFinalizer {

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

    private final Map<ClassNode, List<WrapRequest>> pending = new IdentityHashMap<>();
    private int bridgeCounter;

    /**
     * Creates a handler that applies {@link CWrapMethod} annotations.
     */
    public CWrapMethodAnnotationHandler() {
        super(CWrapMethod.class, CWrapMethod::method);
    }

    @Override
    public void transform(CWrapMethod annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        if (target.name.startsWith("<")) {
            throw new TransformerException(transformerMethod, transformer, "must not target constructors or initializers");
        }
        if ((target.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || target.instructions.size() == 0) {
            throw new TransformerException(transformerMethod, transformer, "can not wrap an abstract or native method");
        }
        boolean targetStatic = (target.access & Opcodes.ACC_STATIC) != 0;
        boolean handlerStatic = (transformerMethod.access & Opcodes.ACC_STATIC) != 0;
        if (targetStatic != handlerStatic) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, targetStatic);
        }

        Type targetReturnType = returnType(target.desc);
        Type handlerReturnType = returnType(transformerMethod.desc);
        if (!targetReturnType.equals(handlerReturnType)) {
            throw new TransformerException(transformerMethod, transformer, "must return the target method return type")
                    .help("Expected " + targetReturnType.getClassName());
        }

        Type[] targetArguments = argumentTypes(target.desc);
        Type[] handlerArguments = argumentTypes(transformerMethod.desc);
        if (handlerArguments.length < targetArguments.length + 1) {
            throw new TransformerException(transformerMethod, transformer, "must have the target method arguments followed by a COperation")
                    .help("Expected " + Arrays.toString(targetArguments) + " and COperation");
        }
        for (int i = 0; i < targetArguments.length; i++) {
            if (!ASMUtils.compareType(handlerArguments[i], targetArguments[i])) {
                throw TransformerException.wrongArguments(transformerMethod, transformer, targetArguments);
            }
        }
        if (!ASMUtils.compareType(handlerArguments[targetArguments.length], COPERATION_TYPE)) {
            throw new TransformerException(transformerMethod, transformer, "must have a COperation parameter after the target method arguments");
        }

        CoprocessorUtils.AnnotatedParameter[] parameters = CoprocessorUtils.getAnnotatedParameters(transformerMethod, CShared.class);
        if (parameters != null) {
            for (int i = 0; i <= targetArguments.length; i++) {
                if (parameters[i] != null) {
                    throw new TransformerException(transformerMethod, transformer, "only allows shared parameters after the COperation parameter");
                }
            }
        }
        List<ShareSpec> shares = new ArrayList<>();
        for (int i = targetArguments.length + 1; i < handlerArguments.length; i++) {
            CoprocessorUtils.AnnotatedParameter parameter = parameters == null ? null : parameters[i];
            if (parameter == null) {
                throw new TransformerException(transformerMethod, transformer, "has an excess parameter at index " + i)
                        .help("Only shared reference parameters are allowed after the COperation");
            }
            Type parameterType = handlerArguments[i];
            if (!LocalRefUtils.isRef(parameterType)) {
                throw new TransformerException(transformerMethod, transformer, "shared parameter " + i + " must be a reference type")
                        .help("e.g. CLocalIntRef or CLocalRef<String>");
            }
            Type innerType = LocalRefUtils.getTargetType(parameterType, CoprocessorUtils.getTypeArgument(transformerMethod, i));
            if (innerType == null) {
                throw new TransformerException(transformerMethod, transformer, "shared parameter " + i + " must have a concrete type argument")
                        .help("e.g. CLocalRef<String>");
            }
            Map<String, Object> sharedValues = AnnotationUtils.listToMap(parameter.getAnnotation().values);
            Object nameValue = sharedValues.get("value");
            if (!(nameValue instanceof String)) {
                throw new TransformerException(transformerMethod, transformer, "shared parameter " + i + " must specify a name");
            }
            String namespace = sharedValues.get("namespace") instanceof String ? (String) sharedValues.get("namespace") : "";
            boolean global = Boolean.TRUE.equals(sharedValues.get("global"));
            shares.add(new ShareSpec(new ShareKey(transformer.name, (String) nameValue, namespace, global), innerType, parameterType));
        }

        String handlerKey = transformer.name + " " + transformerMethod.name + transformerMethod.desc;
        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CWrapMethod");
        WrapRequest request = new WrapRequest(target, copiedTransformerMethod, annotation.order(), annotation.require(), annotation.expect(), annotation.allow(), handlerKey, transformer, shares);
        this.pending.computeIfAbsent(transformedClass, k -> new ArrayList<>()).add(request);
    }

    @Override
    public void finish(TransformerManager transformerManager, ClassNode transformedClass) {
        List<WrapRequest> requests = this.pending.remove(transformedClass);
        if (requests == null || requests.isEmpty()) return;

        Map<MethodNode, List<WrapRequest>> byMethod = new LinkedHashMap<>();
        for (WrapRequest request : requests) byMethod.computeIfAbsent(request.target, k -> new ArrayList<>()).add(request);

        Map<String, HandlerStats> stats = new LinkedHashMap<>();
        for (WrapRequest request : requests) stats.computeIfAbsent(request.handlerKey, k -> new HandlerStats(request)).count++;

        for (Map.Entry<MethodNode, List<WrapRequest>> entry : byMethod.entrySet()) {
            List<WrapRequest> methodRequests = entry.getValue();
            methodRequests.sort(Comparator.comparingInt(request -> request.order));
            this.apply(transformedClass, entry.getKey(), methodRequests);
        }
        for (HandlerStats handlerStats : stats.values()) {
            this.validateCounts(transformerManager, handlerStats);
        }
    }

    private void validateCounts(final TransformerManager transformerManager, final HandlerStats stats) {
        WrapRequest request = stats.request;
        int required = request.require;
        if (required < 0) required = Math.max(1, transformerManager.getDefaultRequiredInjections());
        if (stats.count < required) {
            throw new TransformerException(request.handler, request.transformer, "required at least " + required + " injections but performed " + stats.count);
        }
        if (transformerManager.isCheckInjectionCounts() && stats.count < request.expect) {
            throw new TransformerException(request.handler, request.transformer, "expected at least " + request.expect + " injections but performed " + stats.count);
        }
        if (request.allow >= 1 && stats.count > Math.max(required, request.allow)) {
            throw new TransformerException(request.handler, request.transformer, "allowed at most " + Math.max(required, request.allow) + " injections but performed " + stats.count);
        }
    }

    private void apply(final ClassNode targetClass, final MethodNode targetMethod, final List<WrapRequest> requests) {
        Map<ShareKey, ShareInfo> shareInfos = new LinkedHashMap<>();
        for (WrapRequest request : requests) {
            request.resolvedShares = new ArrayList<>();
            for (ShareSpec spec : request.shares) {
                ShareInfo info = shareInfos.get(spec.key);
                if (info == null) {
                    info = new ShareInfo(spec.key, spec.innerType, true, spec.referenceType, -1);
                    shareInfos.put(spec.key, info);
                } else if (!info.innerType.equals(spec.innerType) || !info.reference || !info.slotType.equals(spec.referenceType)) {
                    throw new TransformerException(request.handler, request.transformer, "uses shared variable '" + spec.key.name + "' with an incompatible type");
                }
                request.resolvedShares.add(info);
            }
        }

        List<ShareInfo> bodyShares = new ArrayList<>();
        SharedVariableAttribute attribute = this.getAttribute(targetMethod);
        if (attribute != null) {
            for (SharedVariableAttribute.SharedVariable variable : attribute.getVariables()) {
                ShareKey key = new ShareKey(variable.getTransformer(), variable.getName(), variable.getNamespace(), variable.isGlobal());
                ShareInfo info = shareInfos.get(key);
                if (info == null) {
                    Type slotType = variable.isReference() ? getReferenceType(variable.getType()) : variable.getType();
                    info = new ShareInfo(key, variable.getType(), variable.isReference(), slotType, variable.getVariableIndex());
                    info.initialization.addAll(variable.getInitialization());
                    shareInfos.put(key, info);
                    bodyShares.add(info);
                } else {
                    Type variableSlotType = variable.isReference() ? getReferenceType(variable.getType()) : variable.getType();
                    if (!info.reference || !info.innerType.equals(variable.getType()) || !info.slotType.equals(variableSlotType)) {
                        throw new TransformerException(requests.get(0).handler, requests.get(0).transformer, "uses shared variable '" + key.name + "' with an incompatible type");
                    }
                    info.oldIndex = variable.getVariableIndex();
                    info.initialization.addAll(variable.getInitialization());
                }
            }
        }

        //The shares are appended in outermost to innermost order. Body shares are allocated by the outermost wrapper.
        List<WrapRequest> outerToInner = new ArrayList<>(requests);
        Collections.reverse(outerToInner);
        List<ShareInfo> allShares = new ArrayList<>();
        for (int i = 0; i < outerToInner.size(); i++) {
            addIfMissing(allShares, outerToInner.get(i).resolvedShares);
            if (i == 0) addIfMissing(allShares, bodyShares);
        }

        WrapStage stage = new Vanilla(targetMethod, allShares);
        for (int i = outerToInner.size() - 1; i >= 0; i--) {
            WrapRequest request = outerToInner.get(i);
            List<ShareInfo> sharesToPass = new ArrayList<>(request.resolvedShares);
            if (i == 0) {
                for (ShareInfo bodyShare : bodyShares) if (!sharesToPass.contains(bodyShare)) sharesToPass.add(bodyShare);
            }
            stage = new Wrapper(stage, request, sharesToPass);
        }
        stage.apply(targetClass, new ArrayList<>());
    }

    @Nullable
    private SharedVariableAttribute getAttribute(final MethodNode methodNode) {
        if (methodNode.attrs == null) return null;
        for (Attribute attribute : methodNode.attrs) {
            if (attribute instanceof SharedVariableAttribute) return (SharedVariableAttribute) attribute;
            if (attribute.type.equals(SharedVariableAttribute.NAME)) {
                try {
                    SharedVariableAttribute parsed = new SharedVariableAttribute(attribute);
                    methodNode.attrs.remove(attribute);
                    methodNode.attrs.add(parsed);
                    return parsed;
                } catch (Throwable t) {
                    return null;
                }
            }
        }
        return null;
    }


    private static void addIfMissing(final List<ShareInfo> target, final List<ShareInfo> shares) {
        for (ShareInfo share : shares) {
            if (!target.contains(share)) target.add(share);
        }
    }

    private static AbstractInsnNode defaultValue(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return new InsnNode(Opcodes.ICONST_0);
            case Type.LONG:
                return new InsnNode(Opcodes.LCONST_0);
            case Type.FLOAT:
                return new InsnNode(Opcodes.FCONST_0);
            case Type.DOUBLE:
                return new InsnNode(Opcodes.DCONST_0);
            case Type.ARRAY:
            case Type.OBJECT:
                return new InsnNode(Opcodes.ACONST_NULL);
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

    private static Type getReferenceType(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return Type.getType(CLocalBooleanRef.class);
            case Type.BYTE:
                return Type.getType(CLocalByteRef.class);
            case Type.CHAR:
                return Type.getType(CLocalCharRef.class);
            case Type.SHORT:
                return Type.getType(CLocalShortRef.class);
            case Type.INT:
                return Type.getType(CLocalIntRef.class);
            case Type.LONG:
                return Type.getType(CLocalLongRef.class);
            case Type.FLOAT:
                return Type.getType(CLocalFloatRef.class);
            case Type.DOUBLE:
                return Type.getType(CLocalDoubleRef.class);
            case Type.ARRAY:
            case Type.OBJECT:
                return Type.getType(CLocalRef.class);
            default:
                throw new IllegalArgumentException("Unsupported shared type " + type);
        }
    }

    private static int getSize(final List<ShareInfo> shares) {
        int size = 0;
        for (ShareInfo share : shares) size += share.slotType.getSize();
        return size;
    }

    private static void fixDesc(final MethodNode wrapper, final int shareCount) {
        if (shareCount == 0) return;
        Type[] arguments = argumentTypes(wrapper.desc);
        wrapper.desc = Type.getMethodDescriptor(returnType(wrapper.desc), Arrays.copyOf(arguments, arguments.length - shareCount));
        wrapper.signature = null;
        wrapper.parameters = null;
    }

    private static MethodNode move(final ClassNode targetClass, final MethodNode original) {
        MethodNode moved = new MethodNode(
                (original.access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PRIVATE,
                uniqueMethodName(targetClass, original.name + "$ct$wrapped"),
                original.desc,
                null,
                original.exceptions == null ? null : original.exceptions.toArray(new String[0])
        );
        moved.instructions = original.instructions;
        moved.tryCatchBlocks = original.tryCatchBlocks;
        moved.localVariables = original.localVariables;
        original.instructions = new InsnList();
        original.tryCatchBlocks = null;
        original.localVariables = null;
        stripLocalVariableReferences(original.visibleTypeAnnotations);
        stripLocalVariableReferences(original.invisibleTypeAnnotations);
        original.visibleLocalVariableAnnotations = null;
        original.invisibleLocalVariableAnnotations = null;
        targetClass.methods.add(moved);
        return moved;
    }

    private static void stripLocalVariableReferences(@Nullable final List<TypeAnnotationNode> annotations) {
        if (annotations == null) return;
        annotations.removeIf(annotation -> new TypeReference(annotation.typeRef).getSort() == TypeReference.LOCAL_VARIABLE);
    }

    private static String uniqueMethodName(final ClassNode targetClass, final String name) {
        String uniqueName = name;
        int index = 0;
        while (hasMethod(targetClass, uniqueName)) uniqueName = name + "$" + index++;
        return uniqueName;
    }

    private static boolean hasMethod(final ClassNode targetClass, final String name) {
        for (MethodNode method : targetClass.methods) {
            if (method.name.equals(name)) return true;
        }
        return false;
    }


    private abstract static class WrapStage {

        protected abstract MethodNode getVanillaMethod();

        public abstract MethodNode apply(final ClassNode targetClass, final List<ShareInfo> gatheredShares);

    }

    private static class Vanilla extends WrapStage {

        private final MethodNode original;
        private final List<ShareInfo> shares;

        private Vanilla(final MethodNode original, final List<ShareInfo> shares) {
            this.original = original;
            this.shares = shares;
        }

        @Override
        protected MethodNode getVanillaMethod() {
            return this.original;
        }

        @Override
        public MethodNode apply(final ClassNode targetClass, final List<ShareInfo> gatheredShares) {
            if (this.shares.isEmpty()) return this.original;

            for (ShareInfo share : this.shares) {
                for (AbstractInsnNode instruction : share.initialization) {
                    if (this.original.instructions.contains(instruction)) this.original.instructions.remove(instruction);
                }
            }

            int shareStartIndex = CoprocessorUtils.getFirstNonArgIndex(this.original);
            Map<Integer, Integer> oldToNew = new HashMap<>();
            int shareSlots = 0;
            for (ShareInfo share : this.shares) {
                share.index = shareStartIndex + shareSlots;
                if (share.oldIndex >= 0) oldToNew.put(share.oldIndex, share.index);
                shareSlots += share.slotType.getSize();
            }

            Type[] arguments = argumentTypes(this.original.desc);
            Type[] newArguments = new Type[arguments.length + this.shares.size()];
            System.arraycopy(arguments, 0, newArguments, 0, arguments.length);
            for (int i = 0; i < this.shares.size(); i++) newArguments[arguments.length + i] = this.shares.get(i).slotType;
            this.original.desc = Type.getMethodDescriptor(returnType(this.original.desc), newArguments);
            this.original.signature = null;
            this.original.parameters = null;

            for (AbstractInsnNode instruction : this.original.instructions.toArray()) {
                if (instruction instanceof VarInsnNode) {
                    VarInsnNode var = (VarInsnNode) instruction;
                    Integer newIndex = oldToNew.get(var.var);
                    if (newIndex != null) var.var = newIndex;
                    else if (var.var >= shareStartIndex) var.var += shareSlots;
                } else if (instruction instanceof IincInsnNode) {
                    IincInsnNode iinc = (IincInsnNode) instruction;
                    Integer newIndex = oldToNew.get(iinc.var);
                    if (newIndex != null) iinc.var = newIndex;
                    else if (iinc.var >= shareStartIndex) iinc.var += shareSlots;
                }
            }
            if (this.original.localVariables != null) {
                for (LocalVariableNode localVariable : this.original.localVariables) {
                    Integer newIndex = oldToNew.get(localVariable.index);
                    if (newIndex != null) localVariable.index = newIndex;
                    else if (localVariable.index >= shareStartIndex) localVariable.index += shareSlots;
                }
            }
            return this.original;
        }

    }

    private class Wrapper extends WrapStage {

        private final WrapStage inner;
        private final WrapRequest request;
        private final List<ShareInfo> sharesToPass;
        private final boolean staticMethod;

        private Wrapper(final WrapStage inner, final WrapRequest request, final List<ShareInfo> sharesToPass) {
            this.inner = inner;
            this.request = request;
            this.sharesToPass = sharesToPass;
            this.staticMethod = ((request.target.access & Opcodes.ACC_STATIC) != 0);
        }

        @Override
        protected MethodNode getVanillaMethod() {
            return this.inner.getVanillaMethod();
        }

        @Override
        public MethodNode apply(final ClassNode targetClass, final List<ShareInfo> gatheredShares) {
            List<ShareInfo> newShares = new ArrayList<>(gatheredShares);
            List<ShareInfo> sharesToAllocate = new ArrayList<>();
            for (ShareInfo share : this.sharesToPass) {
                if (!newShares.contains(share)) {
                    newShares.add(share);
                    sharesToAllocate.add(share);
                }
            }

            Type[] operationArguments = argumentTypes(this.getVanillaMethod().desc);
            Type operationReturnType = returnType(this.getVanillaMethod().desc);
            MethodNode wrapper = this.inner.apply(targetClass, newShares);
            MethodNode innerMethod = move(targetClass, wrapper);
            fixDesc(wrapper, sharesToAllocate.size());

            InsnList instructions = new InsnList();
            this.allocateShares(sharesToAllocate, instructions);
            if (!this.staticMethod) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            this.loadArguments(operationArguments, instructions, this.staticMethod ? 0 : 1);
            if (!this.staticMethod) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            for (ShareInfo share : newShares) {
                instructions.add(new VarInsnNode(share.slotType.getOpcode(Opcodes.ILOAD), share.index));
            }
            instructions.add(this.createOperation(targetClass, innerMethod, operationArguments, operationReturnType, new ArrayList<>(newShares)));
            for (ShareInfo share : this.request.resolvedShares) {
                instructions.add(new VarInsnNode(share.slotType.getOpcode(Opcodes.ILOAD), share.index));
            }
            instructions.add(this.createHandlerCall(targetClass));
            instructions.add(new InsnNode(operationReturnType.getOpcode(Opcodes.IRETURN)));

            wrapper.instructions.add(instructions);
            return wrapper;
        }

        private InvokeDynamicInsnNode createOperation(final ClassNode targetClass, final MethodNode innerMethod, final Type[] operationArguments, final Type operationReturnType, final List<ShareInfo> shares) {
            boolean virtual = !this.staticMethod;
            MethodNode bridge = this.createBridge(targetClass, innerMethod, operationArguments, operationReturnType, shares, virtual);
            Type[] factoryArguments;
            int offset;
            if (virtual) {
                factoryArguments = new Type[shares.size() + 1];
                factoryArguments[0] = Type.getObjectType(targetClass.name);
                offset = 1;
            } else {
                factoryArguments = new Type[shares.size()];
                offset = 0;
            }
            for (int i = 0; i < shares.size(); i++) factoryArguments[offset + i] = shares.get(i).slotType;
            Handle implementation = new Handle(virtual ? Opcodes.H_INVOKESPECIAL : Opcodes.H_INVOKESTATIC, targetClass.name, bridge.name, bridge.desc, ((targetClass.access & Opcodes.ACC_INTERFACE) != 0));
            return new InvokeDynamicInsnNode("call", Type.getMethodDescriptor(COPERATION_TYPE, factoryArguments), LAMBDA_METAFACTORY, SAM_TYPE, implementation, SAM_TYPE);
        }

        private MethodNode createBridge(final ClassNode targetClass, final MethodNode innerMethod, final Type[] operationArguments, final Type operationReturnType, final List<ShareInfo> shares, final boolean virtual) {
            String name;
            do {
                name = "ct$wrapmethod$" + innerMethod.name.replaceAll("[^a-zA-Z0-9_$]", "_") + "$" + CWrapMethodAnnotationHandler.this.bridgeCounter++;
            } while (hasMethod(targetClass, name));

            Type[] descriptorArguments = new Type[shares.size() + 1];
            for (int i = 0; i < shares.size(); i++) descriptorArguments[i] = shares.get(i).slotType;
            descriptorArguments[descriptorArguments.length - 1] = OBJECT_ARRAY_TYPE;
            MethodNode bridge = new MethodNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (virtual ? 0 : Opcodes.ACC_STATIC),
                    name,
                    Type.getMethodDescriptor(OBJECT_TYPE, descriptorArguments),
                    null, null
            );

            int arrayIndex = (virtual ? 1 : 0) + getSize(shares);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
            bridge.instructions.add(ASMUtils.intPush(operationArguments.length));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(COperationRuntime.class), "checkArgumentCount", Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_ARRAY_TYPE, Type.INT_TYPE), false));

            if (virtual) bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            for (int i = 0; i < operationArguments.length; i++) {
                bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                bridge.instructions.add(ASMUtils.intPush(i));
                bridge.instructions.add(new InsnNode(Opcodes.AALOAD));
                bridge.instructions.add(ASMUtils.getCast(operationArguments[i]));
            }
            int shareIndex = virtual ? 1 : 0;
            for (ShareInfo share : shares) {
                bridge.instructions.add(new VarInsnNode(share.slotType.getOpcode(Opcodes.ILOAD), shareIndex));
                shareIndex += share.slotType.getSize();
            }
            bridge.instructions.add(new MethodInsnNode(virtual ? Opcodes.INVOKESPECIAL : Opcodes.INVOKESTATIC, targetClass.name, innerMethod.name, innerMethod.desc, ((targetClass.access & Opcodes.ACC_INTERFACE) != 0)));

            if (operationReturnType.equals(Type.VOID_TYPE)) {
                bridge.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            } else {
                AbstractInsnNode box = ASMUtils.getPrimitiveToObject(operationReturnType);
                if (box != null) bridge.instructions.add(box);
            }
            bridge.instructions.add(new InsnNode(Opcodes.ARETURN));
            targetClass.methods.add(bridge);
            return bridge;
        }

        private MethodInsnNode createHandlerCall(final ClassNode targetClass) {
            boolean isInterface = ((targetClass.access & Opcodes.ACC_INTERFACE) != 0);
            MethodInsnNode call;
            if (this.staticMethod) {
                call = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, this.request.handler.name, this.request.handler.desc, isInterface);
            } else if (((this.request.handler.access & Opcodes.ACC_PRIVATE) != 0)) {
                call = new MethodInsnNode(Opcodes.INVOKESPECIAL, targetClass.name, this.request.handler.name, this.request.handler.desc, isInterface);
            } else {
                call = new MethodInsnNode(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, targetClass.name, this.request.handler.name, this.request.handler.desc, isInterface);
            }
            return call;
        }

        private void allocateShares(final List<ShareInfo> shares, final InsnList instructions) {
            for (ShareInfo share : shares) {
                if (share.reference) {
                    instructions.add(new TypeInsnNode(Opcodes.NEW, share.slotType.getInternalName()));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, share.slotType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false));
                    instructions.add(new VarInsnNode(Opcodes.ASTORE, share.index));
                } else {
                    instructions.add(defaultValue(share.innerType));
                    instructions.add(new VarInsnNode(ASMUtils.getStoreOpcode(share.innerType), share.index));
                }
            }
        }

        private void loadArguments(final Type[] arguments, final InsnList instructions, final int startIndex) {
            int index = startIndex;
            for (Type argument : arguments) {
                instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), index));
                index += argument.getSize();
            }
        }

    }


    private static class WrapRequest {
        private final MethodNode target;
        private final MethodNode handler;
        private final int order;
        private final int require;
        private final int expect;
        private final int allow;
        private final String handlerKey;
        private final ClassNode transformer;
        private final List<ShareSpec> shares;
        private List<ShareInfo> resolvedShares = Collections.emptyList();

        private WrapRequest(final MethodNode target, final MethodNode handler, final int order, final int require, final int expect, final int allow, final String handlerKey, final ClassNode transformer, final List<ShareSpec> shares) {
            this.target = target;
            this.handler = handler;
            this.order = order;
            this.require = require;
            this.expect = expect;
            this.allow = allow;
            this.handlerKey = handlerKey;
            this.transformer = transformer;
            this.shares = shares;
        }
    }

    private static class HandlerStats {
        private final WrapRequest request;
        private int count;

        private HandlerStats(final WrapRequest request) {
            this.request = request;
        }
    }

    private static class ShareSpec {
        private final ShareKey key;
        private final Type innerType;
        private final Type referenceType;

        private ShareSpec(final ShareKey key, final Type innerType, final Type referenceType) {
            this.key = key;
            this.innerType = innerType;
            this.referenceType = referenceType;
        }
    }

    private static class ShareInfo {
        private final ShareKey key;
        private final Type innerType;
        private final boolean reference;
        private final Type slotType;
        private final List<AbstractInsnNode> initialization = new ArrayList<>();
        private int oldIndex;
        private int index;

        private ShareInfo(final ShareKey key, final Type innerType, final boolean reference, final Type slotType, final int oldIndex) {
            this.key = key;
            this.innerType = innerType;
            this.reference = reference;
            this.slotType = slotType;
            this.oldIndex = oldIndex;
        }
    }

    private static class ShareKey {
        private final String transformer;
        private final String name;
        private final String namespace;
        private final boolean global;

        private ShareKey(final String transformer, final String name, final String namespace, final boolean global) {
            this.transformer = transformer;
            this.name = name;
            this.namespace = namespace;
            this.global = global;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof ShareKey)) return false;
            ShareKey shareKey = (ShareKey) o;
            if (!this.name.equals(shareKey.name)) return false;
            if (!this.namespace.isEmpty() || !shareKey.namespace.isEmpty()) return this.namespace.equals(shareKey.namespace);
            if (this.global != shareKey.global) return false;
            return this.global || this.transformer.equals(shareKey.transformer);
        }

        @Override
        public int hashCode() {
            if (!this.namespace.isEmpty()) return Objects.hash(this.name, this.namespace);
            if (this.global) return Objects.hash(this.name, "", true);
            return Objects.hash(this.transformer, this.name, "", false);
        }
    }

}
