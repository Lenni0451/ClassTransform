package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
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
 * The annotation handler for the {@link CInject} annotation.
 */
@ParametersAreNonnullByDefault
public class CInjectAnnotationHandler extends RemovingTargetAnnotationHandler<CInject> {

    private final List<String> captureTargets = new ArrayList<>();

    public CInjectAnnotationHandler() {
        super(CInject.class, CInject::method);

        this.captureTargets.add("RETURN");
        this.captureTargets.add("TAIL");
        this.captureTargets.add("THROW");
    }

    @Override
    public void transform(CInject annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        boolean hasArgs;
        boolean hasCallback;
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        {
            Type[] arguments = argumentTypes(transformerMethod.desc);
            Type[] targetArguments = argumentTypes(target.desc);

            if (arguments.length == 0) {
                hasArgs = false;
                hasCallback = false;
            } else if (arguments.length == 1 && ASMUtils.compareType(arguments[0], type(InjectionCallback.class))) {
                hasArgs = false;
                hasCallback = true;
            } else if (ASMUtils.compareTypes(targetArguments, arguments)) {
                hasArgs = true;
                hasCallback = false;
            } else if (ASMUtils.compareTypes(targetArguments, arguments, false, type(InjectionCallback.class))) {
                hasArgs = true;
                hasCallback = true;
            } else {
                throw new TransformerException(transformerMethod, transformer, "must have the same arguments as target method or no arguments with optional InjectionCallback")
                        .help(Codifier.of(target).param(type(InjectionCallback.class)));
            }
        }
        if (!returnType(transformerMethod.desc).equals(Type.VOID_TYPE)) throw TransformerException.mustReturnVoid(transformerMethod, transformer);

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CInject");
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (CTarget injectTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(injectTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, injectTarget.target(), injectionTargets.keySet());

            List<AbstractInsnNode> targetInstructions = injectionTarget.getTargets(injectionTargets, target, injectTarget, annotation.slice());
            CTarget.Shift shift = injectionTarget.getShift(injectTarget);
            if (targetInstructions == null) {
                throw new TransformerException(transformerMethod, transformer, "has invalid " + injectTarget.value() + " member declaration")
                        .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
            }
            if (targetInstructions.isEmpty() && !injectTarget.optional()) {
                throw new TransformerException(transformerMethod, transformer, "target '" + injectTarget.value() + "' could not be found")
                        .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
            }
            for (AbstractInsnNode instruction : targetInstructions) {
                InsnList instructions;

                if (this.captureTargets.contains(injectTarget.value().toUpperCase(Locale.ROOT))) {
                    instructions = this.getReturnInstructions(transformedClass, target, transformerMethod, annotation.cancellable(), hasArgs, hasCallback, transformerMethodCalls);
                } else {
                    instructions = this.getCallInstructions(transformedClass, target, transformerMethod, annotation.cancellable(), hasArgs, hasCallback, transformerMethodCalls);
                }

                if (shift == CTarget.Shift.BEFORE) target.instructions.insertBefore(instruction, instructions);
                else target.instructions.insert(instruction, instructions);
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

    private InsnList getCallInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final boolean cancellable, final boolean hasArgs, final boolean hasCallback, final List<MethodInsnNode> transformerMethodCalls) {
        Type returnType = returnType(target.desc);
        int callbackVar = ASMUtils.getFreeVarIndex(target);

        InsnList instructions = this.getLoadInstructions(target, hasArgs);
        this.createCallback(instructions, cancellable, hasCallback, callbackVar, Type.VOID_TYPE, 0);
        this.callInjectionMethod(instructions, classNode, target, source, transformerMethodCalls);
        this.getCancelInstructions(instructions, cancellable, hasCallback, callbackVar, returnType);
        return instructions;
    }

    private InsnList getReturnInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final boolean cancellable, final boolean hasArgs, final boolean hasCallback, final List<MethodInsnNode> transformerMethodCalls) {
        Type returnType = returnType(target.desc);
        boolean isVoid = returnType.equals(Type.VOID_TYPE);
        int callbackVar = ASMUtils.getFreeVarIndex(target);
        //The return value has to be stored locally. We just take the callbackVar + 1 since callbackVar is the last element on the variable table
        int returnVar = callbackVar + 1;

        InsnList instructions = this.getLoadInstructions(target, hasArgs);
        this.createCallback(instructions, cancellable, hasCallback, callbackVar, returnType, returnVar);
        this.callInjectionMethod(instructions, classNode, target, source, transformerMethodCalls);
        this.getCancelInstructions(instructions, cancellable, hasCallback, callbackVar, returnType);
        if (!isVoid && hasCallback) {
            instructions.insert(new VarInsnNode(ASMUtils.getStoreOpcode(returnType), returnVar)); //If the method is not a void, store the return value
            instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
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
            instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
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

    private void callInjectionMethod(final InsnList instructions, final ClassNode classNode, final MethodNode target, final MethodNode source, final List<MethodInsnNode> transformerMethodCalls) {
        boolean isInterface = Modifier.isInterface(classNode.access);
        InsnList loadLocals = new InsnList();
        InsnList postExecuteInstructions = new InsnList();

        MethodInsnNode transformerCall;
        if (Modifier.isStatic(target.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, source.name, source.desc, isInterface);
        } else {
            transformerCall = new MethodInsnNode(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, classNode.name, source.name, source.desc, isInterface);
            instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        instructions.add(loadLocals);
        instructions.add(transformerCall);
        instructions.add(postExecuteInstructions);

        transformerMethodCalls.add(transformerCall);
    }

    private void getCancelInstructions(final InsnList instructions, final boolean cancellable, final boolean hasCallback, final int callbackVar, final Type returnType) {
        if (!cancellable || !hasCallback) return; //If the method is cancellable, check if the callback has been cancelled
        //Check if the callback is cancelled
        LabelNode jump = new LabelNode();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, internalName(InjectionCallback.class), "isCancelled", methodDescriptor(boolean.class)));
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, jump));
        if (!Type.VOID_TYPE.equals(returnType)) { //If the method has a return value, take the value from the callback
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, internalName(InjectionCallback.class), "getReturnValue", methodDescriptor(Object.class)));
            instructions.add(ASMUtils.getCast(returnType));
            instructions.add(new InsnNode(ASMUtils.getReturnOpcode(returnType)));
        } else { //If the method is void, simply return
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
        instructions.add(jump);
    }

}
