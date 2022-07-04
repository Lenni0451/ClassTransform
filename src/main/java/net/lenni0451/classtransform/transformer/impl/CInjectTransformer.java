package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.types.ARemovingTargetTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CInjectTransformer extends ARemovingTargetTransformer<CInject> {

    private final List<String> captureTargets = new ArrayList<>();

    public CInjectTransformer() {
        super(CInject.class, CInject::method);

        this.captureTargets.add("RETURN");
        this.captureTargets.add("TAIL");
        this.captureTargets.add("THROW");
    }

    @Override
    public void transform(CInject annotation, TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        boolean hasCallback;
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            boolean isStatic = Modifier.isStatic(target.access);
            throw new TransformerException(transformerMethod, transformer, "must " + (isStatic ? "" : "not ") + "be static")
                    .help(Codifier.of(transformerMethod).access(isStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
        }
        {
            Type[] arguments = Type.getArgumentTypes(transformerMethod.desc);
            Type[] targetArguments = Type.getArgumentTypes(target.desc);

            boolean directMatch = ASMUtils.compareTypes(targetArguments, arguments);
            boolean callbackMatch = ASMUtils.compareTypes(targetArguments, arguments, false, Type.getType(InjectionCallback.class));
            if (!directMatch && !callbackMatch) {
                throw new TransformerException(transformerMethod, transformer, "must have the same arguments as target method with optional InjectionCallback")
                        .help(Codifier.of(target).param(Type.getType(InjectionCallback.class)));
            }
            hasCallback = callbackMatch;
        }
        if (!Type.getReturnType(transformerMethod.desc).equals(Type.VOID_TYPE)) {
            throw new TransformerException(transformerMethod, transformer, "must have void return type")
                    .help(Codifier.of(target).returnType(Type.VOID_TYPE));
        }

        this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CInject");
        for (CTarget injectTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(injectTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, injectTarget.target(), injectionTargets.keySet());

            {
                List<AbstractInsnNode> targetInstructions = injectionTarget.getTargets(injectionTargets, target, injectTarget, annotation.slice());
                CTarget.Shift shift = injectionTarget.getShift(injectTarget);
                if (targetInstructions == null) {
                    throw new TransformerException(transformerMethod, transformer, "has invalid " + injectTarget.value() + " member declaration")
                            .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
                }
                for (AbstractInsnNode instruction : targetInstructions) {
                    InsnList instructions;
                    if (this.captureTargets.contains(injectTarget.value().toUpperCase(Locale.ROOT))) {
                        instructions = this.getReturnInstructions(transformedClass, target, transformerMethod, annotation.cancellable(), !hasCallback);
                    } else {
                        instructions = this.getCallInstructions(transformedClass, target, transformerMethod, annotation.cancellable(), !hasCallback);
                    }

                    if (shift == CTarget.Shift.BEFORE) target.instructions.insertBefore(instruction, instructions);
                    else target.instructions.insert(instruction, instructions);
                }
            }
        }
    }

    private InsnList getCallInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final boolean cancellable, final boolean noCallback) {
        boolean isVoid = Type.getReturnType(target.desc).equals(Type.VOID_TYPE);
        boolean isInterface = Modifier.isInterface(classNode.access);
        int callbackVar = ASMUtils.getFreeVarIndex(target);

        InsnList instructions = this.getLoadInstructions(target);
        if (!noCallback) { //Create callback instance with cancellable set to the annotation value
            instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(InjectionCallback.class)));
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InjectionCallback.class), "<init>", "(Z)V"));
            instructions.add(new VarInsnNode(Opcodes.ASTORE, callbackVar));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
        }
        { //Call the actual injection method
            if (Modifier.isStatic(target.access)) {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, source.name, source.desc, isInterface));
            } else {
                instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new MethodInsnNode(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, classNode.name, source.name, source.desc, isInterface));
            }
        }
        if (cancellable && !noCallback) { //If the callback is cancellable
            //Get isCancelled boolean
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InjectionCallback.class), "isCancelled", "()Z"));
            //If isCancelled is true, return
            LabelNode jump = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFEQ, jump));
            if (!isVoid) {
                //If the method has a return value, take the value from the callback
                instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InjectionCallback.class), "getReturnValue", "()Ljava/lang/Object;"));
                instructions.add(ASMUtils.getCast(Type.getReturnType(target.desc)));
                instructions.add(new InsnNode(ASMUtils.getReturnOpcode(Type.getReturnType(target.desc))));
            } else {
                //If the method is void, simply return
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
            instructions.add(jump);
        }
        return instructions;
    }

    private InsnList getReturnInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final boolean cancellable, final boolean noCallback) {
        Type returnType = Type.getReturnType(target.desc);
        boolean isVoid = returnType.equals(Type.VOID_TYPE);
        boolean isInterface = Modifier.isInterface(classNode.access);
        int callbackVar = ASMUtils.getFreeVarIndex(target);
        //The return value has to be stored locally. We just take the callbackVar + 1 since callbackVar is the last element on the variable table
        int returnVar = callbackVar + 2;
        int returnTypeStoreOpcode = ASMUtils.getStoreOpcode(returnType);
        int returnTypeLoadOpcode = ASMUtils.getLoadOpcode(returnType);

        InsnList instructions = this.getLoadInstructions(target);
        if (!isVoid && !noCallback) { //If the method is not a void, store the return value
            instructions.insert(new VarInsnNode(returnTypeStoreOpcode, returnVar));
        }
        if (!noCallback) { //Create the callback instance
            instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(InjectionCallback.class)));
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            if (!isVoid) {
                instructions.add(new VarInsnNode(returnTypeLoadOpcode, returnVar));
                AbstractInsnNode convertOpcode = ASMUtils.getPrimitiveToObject(returnType);
                if (convertOpcode != null) instructions.add(convertOpcode);
                instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InjectionCallback.class), "<init>", "(ZLjava/lang/Object;)V"));
            } else {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InjectionCallback.class), "<init>", "(Z)V"));
            }
            instructions.add(new VarInsnNode(Opcodes.ASTORE, callbackVar));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
        }
        { //Call the callback
            if (Modifier.isStatic(target.access)) {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, source.name, source.desc, isInterface));
            } else {
                if (!isVoid && !noCallback) instructions.insert(instructions.getFirst(), new VarInsnNode(Opcodes.ALOAD, 0));
                else instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new MethodInsnNode(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, classNode.name, source.name, source.desc, isInterface));
            }
        }
        if (cancellable && !noCallback) { //If the method is cancellable, check if the callback has been cancelled
            //Get if the callback is cancelled
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InjectionCallback.class), "isCancelled", "()Z"));
            LabelNode jump = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFEQ, jump));
            if (!isVoid) {
                //If the method has a return value, take the value from the callback
                instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InjectionCallback.class), "getReturnValue", "()Ljava/lang/Object;"));
                instructions.add(ASMUtils.getCast(returnType));
                instructions.add(new InsnNode(ASMUtils.getReturnOpcode(Type.getReturnType(target.desc))));
            } else { //If the method is void, simply return
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
            instructions.add(jump);
        }
        if (!isVoid && !noCallback) instructions.add(new VarInsnNode(returnTypeLoadOpcode, returnVar));
        return instructions;
    }

    private InsnList getLoadInstructions(final MethodNode methodNode) {
        InsnList instructions = new InsnList();
        Type[] parameter = Type.getArgumentTypes(methodNode.desc);
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

}
