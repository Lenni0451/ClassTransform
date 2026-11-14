package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CLocalCapture;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CSurrogate;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.coprocessor.impl.LocalVariableLookup;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CDescResolver;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.CoerceUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.DescMatcher;
import net.lenni0451.classtransform.utils.InjectionUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.log.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CInject} annotation.
 */
@ParametersAreNonnullByDefault
public class CInjectAnnotationHandler extends InjectionAnnotationHandler<CInject> {

    public CInjectAnnotationHandler() {
        super(CInject.class, CInject::method);
    }

    @Override
    protected Set<MethodNode> getTargetMethods(final CInject annotation, final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method) {
        Set<MethodNode> targets = super.getTargetMethods(annotation, manager, target, transformer, method);
        CDesc[] descs = CDescResolver.resolve(annotation.desc(), manager, transformer, method);
        if (descs.length > 0) DescMatcher.addMethods(targets, target, descs);
        return targets;
    }

    @Override
    public void transform(CInject annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        this.checkConstraints(transformerManager, annotation.constraints(), transformerMethod, transformer);
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        HandlerData handlerData = this.resolveHandler(transformerMethod, transformer, target);
        Type[] handlerArguments = argumentTypes(transformerMethod.desc);
        int extraLocals = handlerArguments.length - handlerData.baseArguments;
        CLocalCapture localCapture = annotation.locals();
        if (extraLocals < 0 || localCapture == CLocalCapture.NO_CAPTURE && extraLocals > 0) {
            throw new TransformerException(transformerMethod, transformer, "must have the same arguments as target method or no arguments with optional InjectionCallback")
                    .help(Codifier.of(target).param(type(InjectionCallback.class)));
        }
        if (!returnType(transformerMethod.desc).equals(Type.VOID_TYPE)) throw TransformerException.mustReturnVoid(transformerMethod, transformer);

        String handlerName = transformerMethod.name;
        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CInject");
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        Map<AbstractInsnNode, CTarget.Shift> points = new LinkedHashMap<>();
        for (CTarget injectTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(injectTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, injectTarget.target(), injectionTargets.keySet());

            List<AbstractInsnNode> targetInstructions = InjectionPlan.select(transformerManager, target, injectTarget, annotation.slice());
            CTarget.Shift shift = injectTarget.shift() == CTarget.Shift.BY ? CTarget.Shift.BEFORE : injectionTarget.getShift(injectTarget);
            if (targetInstructions == null) {
                throw new TransformerException(transformerMethod, transformer, "has invalid " + injectTarget.value() + " member declaration")
                        .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
            }
            for (AbstractInsnNode instruction : targetInstructions) points.putIfAbsent(instruction, shift);
        }

        int frameSize = CoprocessorUtils.getFirstNonArgIndex(target);
        int errorStubIndex = 0;
        for (Map.Entry<AbstractInsnNode, CTarget.Shift> point : points.entrySet()) {
            AbstractInsnNode instruction = point.getKey();
            int position = target.instructions.indexOf(instruction);
            if (localCapture == CLocalCapture.PRINT) {
                LocalVariableLookup.print(transformedClass, target, position, null, false);
                continue;
            }
            List<LocalVariableLookup.Local> capturedLocals = new ArrayList<>();
            MethodNode source = copiedTransformerMethod;
            if (localCapture != CLocalCapture.NO_CAPTURE) {
                capturedLocals = this.captureLocals(transformedClass, target, position, frameSize, extraLocals);
                if (!this.matchLocals(transformerMethod, handlerData.baseArguments, capturedLocals)) {
                    MethodNode surrogate = this.findSurrogate(transformer, handlerName, transformerMethod, target, handlerData, capturedLocals);
                    if (surrogate != null) {
                        source = surrogate;
                    } else if (localCapture == CLocalCapture.CAPTURE_FAILSOFT) {
                        Logger.warn("Local capture failed for {}.{}{} in transformer '{}': expected {} local variable(s) but found {}",
                                transformedClass.name, target.name, target.desc, transformer.name, extraLocals, capturedLocals.size());
                        continue;
                    } else if (localCapture == CLocalCapture.CAPTURE_FAILEXCEPTION) {
                        source = this.createErrorStub(transformedClass, transformerMethod, target, handlerData, capturedLocals, errorStubIndex++);
                    } else {
                        throw new TransformerException(transformerMethod, transformer, "local capture failed: expected " + extraLocals + " local variable(s) but found " + capturedLocals.size())
                                .targetClass(transformedClass.name).targetMethod(target);
                    }
                }
            }

            InsnList instructions;
            if (instruction.getOpcode() >= Opcodes.IRETURN && instruction.getOpcode() <= Opcodes.RETURN || instruction.getOpcode() == Opcodes.ATHROW) {
                instructions = this.getReturnInstructions(transformedClass, target, source, annotation.cancellable(), handlerData.hasArgs, handlerData.hasCallback, this.loadCapturedLocals(capturedLocals), transformerMethodCalls);
            } else {
                instructions = this.getCallInstructions(transformedClass, target, source, annotation.cancellable(), handlerData.hasArgs, handlerData.hasCallback, this.loadCapturedLocals(capturedLocals), transformerMethodCalls);
            }

            if (point.getValue() == CTarget.Shift.BEFORE) target.instructions.insertBefore(instruction, instructions);
            else {
                LabelNode end = new LabelNode();
                instructions.add(end);
                target.instructions.insert(InjectionPlan.after(instruction), instructions);
                InjectionPlan.setAfter(instruction, end);
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private HandlerData resolveHandler(final MethodNode transformerMethod, final ClassNode transformer, final MethodNode target) {
        Type[] arguments = argumentTypes(transformerMethod.desc);
        Type[] targetArguments = argumentTypes(target.desc);
        if (arguments.length == 0) return new HandlerData(false, false, 0);

        boolean targetArgumentsMatch = InjectionUtils.compareTypes(transformerMethod, targetArguments, Arrays.copyOf(arguments, targetArguments.length));
        if (targetArgumentsMatch) {
            boolean hasCallback = arguments.length > targetArguments.length && ASMUtils.compareType(arguments[targetArguments.length], type(InjectionCallback.class));
            return new HandlerData(targetArguments.length > 0, hasCallback, targetArguments.length + (hasCallback ? 1 : 0));
        }
        if (ASMUtils.compareType(arguments[0], type(InjectionCallback.class))) {
            return new HandlerData(false, true, 1);
        }
        throw new TransformerException(transformerMethod, transformer, "must have the same arguments as target method or no arguments with optional InjectionCallback");
    }

    private List<LocalVariableLookup.Local> captureLocals(final ClassNode transformedClass, final MethodNode target, final int position, final int frameSize, final int count) {
        List<LocalVariableLookup.Local> captured = new ArrayList<>();
        if (count <= 0) return captured;
        for (LocalVariableLookup.Local local : LocalVariableLookup.getLocalsInScope(transformedClass, target, position)) {
            if (local.getIndex() < frameSize) continue;
            captured.add(local);
            if (captured.size() == count) break;
        }
        return captured;
    }

    private boolean matchLocals(final MethodNode source, final int baseArguments, final List<LocalVariableLookup.Local> locals) {
        Type[] arguments = argumentTypes(source.desc);
        if (arguments.length != baseArguments + locals.size()) return false;
        for (int i = 0; i < locals.size(); i++) {
            if (!CoerceUtils.canCoerce(locals.get(i).getType(), arguments[baseArguments + i], CoerceUtils.hasCoerce(source, baseArguments + i))) {
                return false;
            }
        }
        return true;
    }

    private MethodNode findSurrogate(final ClassNode transformer, final String handlerName, final MethodNode transformerMethod, final MethodNode target, final HandlerData handlerData, final List<LocalVariableLookup.Local> locals) {
        String descriptor = this.getCaptureDescriptor(target, handlerData, locals);
        for (MethodNode method : transformer.methods) {
            if (!method.name.equals(handlerName)) continue;
            if (!method.desc.equals(descriptor)) continue;
            if (Modifier.isStatic(method.access) != Modifier.isStatic(transformerMethod.access)) continue;
            if (!AnnotationUtils.findAnnotation(method, CSurrogate.class).isPresent()) continue;
            return method;
        }
        return null;
    }

    private MethodNode createErrorStub(final ClassNode transformedClass, final MethodNode transformerMethod, final MethodNode target, final HandlerData handlerData, final List<LocalVariableLookup.Local> locals, final int index) {
        String descriptor = this.getCaptureDescriptor(target, handlerData, locals);
        String name = transformerMethod.name + "$missing$" + index;
        while (ASMUtils.hasMethod(transformedClass, name, descriptor)) name += "$";
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, descriptor, null, null);
        method.maxStack = 3;
        InsnList instructions = method.instructions;
        instructions.add(new TypeInsnNode(Opcodes.NEW, internalName(IllegalStateException.class)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new LdcInsnNode("Local capture failed for " + target.name + target.desc + " in transformer '" + transformerMethod.name + "'"));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(IllegalStateException.class), MN_Init, methodDescriptor(void.class, String.class), false));
        instructions.add(new InsnNode(Opcodes.ATHROW));
        transformedClass.methods.add(method);
        return method;
    }

    private String getCaptureDescriptor(final MethodNode target, final HandlerData handlerData, final List<LocalVariableLookup.Local> locals) {
        List<Type> types = new ArrayList<>();
        if (handlerData.hasArgs) Collections.addAll(types, argumentTypes(target.desc));
        if (handlerData.hasCallback) types.add(type(InjectionCallback.class));
        for (LocalVariableLookup.Local local : locals) types.add(local.getType());
        return Type.getMethodDescriptor(Type.VOID_TYPE, types.toArray(new Type[0]));
    }

    private InsnList loadCapturedLocals(final List<LocalVariableLookup.Local> locals) {
        InsnList instructions = new InsnList();
        for (LocalVariableLookup.Local local : locals) {
            instructions.add(new VarInsnNode(local.getType().getOpcode(Opcodes.ILOAD), local.getIndex()));
        }
        return instructions;
    }

    private InsnList getCallInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final boolean cancellable, final boolean hasArgs, final boolean hasCallback, final InsnList capturedLocals, final List<MethodInsnNode> transformerMethodCalls) {
        Type returnType = returnType(target.desc);
        int callbackVar = ASMUtils.getFreeVarIndex(target);

        InsnList instructions = this.getLoadInstructions(target, hasArgs);
        this.createCallback(instructions, cancellable, hasCallback, callbackVar, returnType, -1);
        instructions.add(capturedLocals);
        this.callInjectionMethod(instructions, classNode, source, transformerMethodCalls);
        this.getCancelInstructions(instructions, cancellable, hasCallback, callbackVar, returnType, -1);
        return instructions;
    }

    private InsnList getReturnInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final boolean cancellable, final boolean hasArgs, final boolean hasCallback, final InsnList capturedLocals, final List<MethodInsnNode> transformerMethodCalls) {
        Type returnType = returnType(target.desc);
        boolean isVoid = returnType.equals(Type.VOID_TYPE);
        int callbackVar = ASMUtils.getFreeVarIndex(target);
        //The return value has to be stored locally. We just take the callbackVar + 1 since callbackVar is the last element on the variable table
        int returnVar = callbackVar + 1;

        InsnList instructions = this.getLoadInstructions(target, hasArgs);
        this.createCallback(instructions, cancellable, hasCallback, callbackVar, returnType, returnVar);
        instructions.add(capturedLocals);
        this.callInjectionMethod(instructions, classNode, source, transformerMethodCalls);
        this.getCancelInstructions(instructions, cancellable, hasCallback, callbackVar, returnType, returnVar);
        if (!isVoid && hasCallback) {
            instructions.insert(new VarInsnNode(ASMUtils.getStoreOpcode(returnType), returnVar)); //If the method is not a void, store the return value
            if (!cancellable) instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
        }
        return instructions;
    }

    private InsnList getLoadInstructions(final MethodNode methodNode, final boolean hasArgs) {
        if (!hasArgs) return new InsnList();
        InsnList instructions = new InsnList();
        Type[] parameter = argumentTypes(methodNode.desc);
        int index = Modifier.isStatic(methodNode.access) ? 0 : 1;
        for (Type type : parameter) {
            if (type.equals(Type.BOOLEAN_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
                index++;
            } else if (type.equals(Type.BYTE_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
                index++;
            } else if (type.equals(Type.SHORT_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
                index++;
            } else if (type.equals(Type.CHAR_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
                index++;
            } else if (type.equals(Type.INT_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
                index++;
            } else if (type.equals(Type.LONG_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.LLOAD, index));
                index += 2;
            } else if (type.equals(Type.FLOAT_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.FLOAD, index));
                index++;
            } else if (type.equals(Type.DOUBLE_TYPE)) {
                instructions.add(new VarInsnNode(Opcodes.DLOAD, index));
                index += 2;
            } else {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, index));
                index++;
            }
        }
        return instructions;
    }

    private void createCallback(final InsnList instructions, final boolean cancellable, final boolean hasCallback, final int callbackVar, final Type returnType, final int returnVar) {
        if (!hasCallback) return;
        //Create the callback instance
        instructions.add(new TypeInsnNode(Opcodes.NEW, internalName(InjectionCallback.class)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        if (!Type.VOID_TYPE.equals(returnType)) {
            if (returnVar >= 0) {
                instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
            } else {
                this.pushDefaultValue(instructions, returnType);
            }
            AbstractInsnNode convertOpcode = ASMUtils.getPrimitiveToObject(returnType);
            if (convertOpcode != null) instructions.add(convertOpcode);
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(InjectionCallback.class), MN_Init, methodDescriptor(void.class, boolean.class, Object.class)));
        } else {
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(InjectionCallback.class), MN_Init, methodDescriptor(void.class, boolean.class)));
        }
        if (cancellable) {
            instructions.add(new VarInsnNode(Opcodes.ASTORE, callbackVar));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
        }
    }

    private void pushDefaultValue(final InsnList instructions, final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                instructions.add(new InsnNode(Opcodes.ICONST_0));
                break;
            case Type.LONG:
                instructions.add(new InsnNode(Opcodes.LCONST_0));
                break;
            case Type.FLOAT:
                instructions.add(new InsnNode(Opcodes.FCONST_0));
                break;
            case Type.DOUBLE:
                instructions.add(new InsnNode(Opcodes.DCONST_0));
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                break;
            default:
                throw new IllegalArgumentException("Unsupported return type " + type);
        }
    }

    private void callInjectionMethod(final InsnList instructions, final ClassNode classNode, final MethodNode source, final List<MethodInsnNode> transformerMethodCalls) {
        boolean isInterface = Modifier.isInterface(classNode.access);
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(source.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, source.name, source.desc, isInterface);
        } else {
            transformerCall = new MethodInsnNode(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, classNode.name, source.name, source.desc, isInterface);
            instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        instructions.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
    }

    /*
     * //returnValue != void
     * if (injectionCallback.isCancelled()) {
     *     return (T) injectionCallback.getReturnValue();
     * }
     *
     * //returnValue == void
     * if (injectionCallback.isCancelled()) {
     *     return;
     * }
     *
     * //returnValue != void, returnVar
     * if (injectionCallback.isCancelled()) {
     *     stack << (T) injectionCallback.getReturnValue();
     * } else {
     *     stack << currentReturnValue;
     * }
     */
    private void getCancelInstructions(final InsnList instructions, final boolean cancellable, final boolean hasCallback, final int callbackVar, final Type returnType, final int returnVar) {
        if (!cancellable || !hasCallback) return; //If the method is cancellable, check if the callback has been cancelled
        boolean willCancel = returnVar >= 0;
        InsnList cancelInstructions = new InsnList();
        //Check if the callback is cancelled
        LabelNode jump = new LabelNode();
        LabelNode end = new LabelNode();
        cancelInstructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
        cancelInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, internalName(InjectionCallback.class), "isCancelled", methodDescriptor(boolean.class)));
        cancelInstructions.add(new JumpInsnNode(Opcodes.IFEQ, jump));
        if (!Type.VOID_TYPE.equals(returnType)) { //If the method has a return value, take the value from the callback
            cancelInstructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
            cancelInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, internalName(InjectionCallback.class), "getReturnValue", methodDescriptor(Object.class)));
            cancelInstructions.add(ASMUtils.getCast(returnType));
            if (willCancel) cancelInstructions.add(new JumpInsnNode(Opcodes.GOTO, end));
            else cancelInstructions.add(new InsnNode(ASMUtils.getReturnOpcode(returnType)));
        } else { //If the method is void, simply return
            if (willCancel) cancelInstructions.clear();
            else cancelInstructions.add(new InsnNode(Opcodes.RETURN));
        }
        cancelInstructions.add(jump);
        if (willCancel && cancelInstructions.size() > 0) {
            cancelInstructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
            cancelInstructions.add(end);
        }
        instructions.add(cancelInstructions);
    }


    private static class HandlerData {
        private final boolean hasArgs;
        private final boolean hasCallback;
        private final int baseArguments;

        private HandlerData(final boolean hasArgs, final boolean hasCallback, final int baseArguments) {
            this.hasArgs = hasArgs;
            this.hasCallback = hasCallback;
            this.baseArguments = baseArguments;
        }
    }

}
