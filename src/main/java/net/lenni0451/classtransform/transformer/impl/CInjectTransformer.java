package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.types.ARemovingTargetTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.*;

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
        boolean hasArgs;
        boolean hasCallback;
        List<CLocalVariable> localVariables = new ArrayList<>();
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            boolean isStatic = Modifier.isStatic(target.access);
            throw new TransformerException(transformerMethod, transformer, "must " + (isStatic ? "" : "not ") + "be static")
                    .help(Codifier.of(transformerMethod).access(isStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
        }
        {
            Type[] arguments = Type.getArgumentTypes(transformerMethod.desc);
            Type[] targetArguments = Type.getArgumentTypes(target.desc);

            if (transformerMethod.invisibleParameterAnnotations != null) {
                List<AnnotationNode>[] annotations = transformerMethod.invisibleParameterAnnotations;
                if (annotations.length == arguments.length) {
                    for (int i = annotations.length - 1; i >= 0; i--) {
                        List<AnnotationNode> paramAnnotations = annotations[i];
                        if (paramAnnotations != null) {
                            for (AnnotationNode paramAnnotation : paramAnnotations) {
                                if (paramAnnotation.desc.equals(Type.getDescriptor(CLocalVariable.class))) {
                                    CLocalVariable localVariable = AnnotationParser.parse(CLocalVariable.class, classProvider, AnnotationParser.listToMap(paramAnnotation.values));
                                    localVariables.add(localVariable);
                                }
                            }
                        }
                    }
                    Collections.reverse(localVariables);
                    arguments = Arrays.copyOfRange(arguments, 0, arguments.length - localVariables.size());
                }
            }

            if (arguments.length == 0) {
                hasArgs = false;
                hasCallback = false;
            } else if (arguments.length == 1 && ASMUtils.compareType(arguments[0], Type.getType(InjectionCallback.class))) {
                hasArgs = false;
                hasCallback = true;
            } else if (ASMUtils.compareTypes(targetArguments, arguments)) {
                hasArgs = true;
                hasCallback = false;
            } else if (ASMUtils.compareTypes(targetArguments, arguments, false, Type.getType(InjectionCallback.class))) {
                hasArgs = true;
                hasCallback = true;
            } else {
                throw new TransformerException(transformerMethod, transformer, "must have the same arguments as target method or no arguments with optional InjectionCallback")
                        .help(Codifier.of(target).param(Type.getType(InjectionCallback.class)));
            }
        }
        if (!Type.getReturnType(transformerMethod.desc).equals(Type.VOID_TYPE)) {
            throw new TransformerException(transformerMethod, transformer, "must have void return type")
                    .help(Codifier.of(target).returnType(Type.VOID_TYPE));
        }

        this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CInject");
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
                    instructions = this.getReturnInstructions(transformedClass, target, transformerMethod, localVariables, annotation.cancellable(), hasArgs, hasCallback);
                } else {
                    instructions = this.getCallInstructions(transformedClass, target, transformerMethod, localVariables, annotation.cancellable(), hasArgs, hasCallback);
                }

                if (shift == CTarget.Shift.BEFORE) target.instructions.insertBefore(instruction, instructions);
                else target.instructions.insert(instruction, instructions);
            }
        }
    }

    private InsnList getCallInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final List<CLocalVariable> localVariables, final boolean cancellable, final boolean hasArgs, final boolean hasCallback) {
        Type returnType = Type.getReturnType(target.desc);
        int callbackVar = ASMUtils.getFreeVarIndex(target);

        InsnList instructions = this.getLoadInstructions(target, hasArgs);
        instructions.add(this.createCallback(cancellable, hasCallback, callbackVar, Type.VOID_TYPE, 0));
        instructions.add(this.callInjectionMethod(classNode, target, source, localVariables));
        instructions.add(this.getCancelInstructions(cancellable, hasCallback, callbackVar, returnType));
        return instructions;
    }

    private InsnList getReturnInstructions(final ClassNode classNode, final MethodNode target, final MethodNode source, final List<CLocalVariable> localVariables, final boolean cancellable, final boolean hasArgs, final boolean hasCallback) {
        Type returnType = Type.getReturnType(target.desc);
        boolean isVoid = returnType.equals(Type.VOID_TYPE);
        int callbackVar = ASMUtils.getFreeVarIndex(target);
        //The return value has to be stored locally. We just take the callbackVar + 1 since callbackVar is the last element on the variable table
        int returnVar = callbackVar + 2;

        InsnList instructions = this.getLoadInstructions(target, hasArgs);
        instructions.add(this.createCallback(cancellable, hasCallback, callbackVar, returnType, returnVar));
        instructions.add(this.callInjectionMethod(classNode, target, source, localVariables));
        instructions.add(this.getCancelInstructions(cancellable, hasCallback, callbackVar, returnType));
        if (!isVoid && hasCallback) {
            instructions.insert(new VarInsnNode(ASMUtils.getStoreOpcode(returnType), returnVar)); //If the method is not a void, store the return value
            instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
        }
        return instructions;
    }

    private InsnList getLoadInstructions(final MethodNode methodNode, final boolean hasArgs) {
        if (!hasArgs) return new InsnList();
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

    private InsnList createCallback(final boolean cancellable, final boolean hasCallback, final int callbackVar, final Type returnType, final int returnVar) {
        InsnList instructions = new InsnList();
        if (hasCallback) { //Create the callback instance
            instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(InjectionCallback.class)));
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            if (!Type.VOID_TYPE.equals(returnType)) {
                instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(returnType), returnVar));
                AbstractInsnNode convertOpcode = ASMUtils.getPrimitiveToObject(returnType);
                if (convertOpcode != null) instructions.add(convertOpcode);
                instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InjectionCallback.class), "<init>", "(ZLjava/lang/Object;)V"));
            } else {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(InjectionCallback.class), "<init>", "(Z)V"));
            }
            instructions.add(new VarInsnNode(Opcodes.ASTORE, callbackVar));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
        }
        return instructions;
    }

    private InsnList callInjectionMethod(final ClassNode classNode, final MethodNode target, final MethodNode source, final List<CLocalVariable> localVariables) {
        boolean isInterface = Modifier.isInterface(classNode.access);
        InsnList instructions = new InsnList();

        InsnList loadLocals = new InsnList();
        if (!localVariables.isEmpty()) {
            Type[] parameter = Type.getArgumentTypes(source.desc);
            for (int i = 0; i < localVariables.size(); i++) {
                InsnList loadInsns = this.resolveLocalVariable(target, source, localVariables.get(i), parameter[parameter.length - localVariables.size() + i]);
                loadLocals.add(loadInsns);
            }
        }

        if (Modifier.isStatic(target.access)) {
            instructions.add(loadLocals);
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, source.name, source.desc, isInterface));
        } else {
            instructions.insert(new VarInsnNode(Opcodes.ALOAD, 0));
            instructions.add(loadLocals);
            instructions.add(new MethodInsnNode(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, classNode.name, source.name, source.desc, isInterface));
        }
        return instructions;
    }

    private InsnList getCancelInstructions(final boolean cancellable, final boolean hasCallback, final int callbackVar, final Type returnType) {
        InsnList instructions = new InsnList();
        if (cancellable && hasCallback) { //If the method is cancellable, check if the callback has been cancelled
            //Get if the callback is cancelled
            LabelNode jump = new LabelNode();
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InjectionCallback.class), "isCancelled", "()Z"));
            instructions.add(new JumpInsnNode(Opcodes.IFEQ, jump));
            if (!Type.VOID_TYPE.equals(returnType)) { //If the method has a return value, take the value from the callback
                instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackVar));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(InjectionCallback.class), "getReturnValue", "()Ljava/lang/Object;"));
                instructions.add(ASMUtils.getCast(returnType));
                instructions.add(new InsnNode(ASMUtils.getReturnOpcode(returnType)));
            } else { //If the method is void, simply return
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
            instructions.add(jump);
        }
        return instructions;
    }

    private InsnList resolveLocalVariable(final MethodNode target, final MethodNode source, final CLocalVariable localVariable, final Type parameter) {
        IParsedAnnotation parsedAnnotation = (IParsedAnnotation) localVariable;
        if (parsedAnnotation.wasSet("name") == parsedAnnotation.wasSet("index")) throw new IllegalStateException("Local variable needs name or index");
        if (parsedAnnotation.wasSet("name") && target.localVariables == null) {
            throw new IllegalArgumentException("The target method does not have a local variable table. The variable name cannot be used to identify the variable index");
        }
        Integer varIndex = null;
        Integer loadOpcode = parsedAnnotation.wasSet("loadOpcode") ? localVariable.loadOpcode() : null;
        if (parsedAnnotation.wasSet("name")) {
            for (LocalVariableNode localVar : target.localVariables) {
                if (localVar.name.equals(localVariable.name())) {
                    varIndex = localVar.index;
                    break;
                }
            }
        } else {
            varIndex = localVariable.index();
        }
        if (varIndex == null) throw new IllegalStateException("Local variable could not be found by name");
        if (loadOpcode == null) {
            for (AbstractInsnNode instruction : target.instructions) {
                if (instruction instanceof VarInsnNode && ((VarInsnNode) instruction).var == varIndex) {
                    int newLoadOpcode = this.getLoadOpcode(instruction.getOpcode());
                    if (loadOpcode == null) loadOpcode = newLoadOpcode;
                    else if (loadOpcode != newLoadOpcode) throw new IllegalStateException("Local variable index conflicts with another variable type. Please define a load opcode");
                }
            }
        }
        if (loadOpcode == null) throw new IllegalStateException("Local variable type could not be found by index. Please define a load opcode");

        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(loadOpcode, varIndex));
        if (parameter.equals(Type.BOOLEAN_TYPE)) {
            if (loadOpcode == Opcodes.LLOAD) instructions.add(new InsnNode(Opcodes.L2I));
            else if (loadOpcode == Opcodes.FLOAD) instructions.add(new InsnNode(Opcodes.F2I));
            else if (loadOpcode == Opcodes.DLOAD) instructions.add(new InsnNode(Opcodes.D2I));
            else if (loadOpcode == Opcodes.ALOAD) instructions.add(ASMUtils.getCast(parameter));
        } else if (parameter.equals(Type.BYTE_TYPE)) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2B));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(new InsnNode(Opcodes.I2B));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(new InsnNode(Opcodes.I2B));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(new InsnNode(Opcodes.I2B));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.SHORT_TYPE)) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2S));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(new InsnNode(Opcodes.I2S));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(new InsnNode(Opcodes.I2S));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(new InsnNode(Opcodes.I2S));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.CHAR_TYPE)) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2C));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(new InsnNode(Opcodes.I2C));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(new InsnNode(Opcodes.I2C));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(new InsnNode(Opcodes.I2C));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.INT_TYPE)) {
            if (loadOpcode == Opcodes.LLOAD) instructions.add(new InsnNode(Opcodes.L2I));
            else if (loadOpcode == Opcodes.FLOAD) instructions.add(new InsnNode(Opcodes.F2I));
            else if (loadOpcode == Opcodes.DLOAD) instructions.add(new InsnNode(Opcodes.D2I));
            else if (loadOpcode == Opcodes.ALOAD) instructions.add(ASMUtils.getCast(parameter));
        } else if (parameter.equals(Type.LONG_TYPE)) {
            if (loadOpcode == Opcodes.ILOAD) instructions.add(new InsnNode(Opcodes.I2L));
            else if (loadOpcode == Opcodes.FLOAD) instructions.add(new InsnNode(Opcodes.F2L));
            else if (loadOpcode == Opcodes.DLOAD) instructions.add(new InsnNode(Opcodes.D2L));
            else if (loadOpcode == Opcodes.ALOAD) instructions.add(ASMUtils.getCast(parameter));
        } else if (parameter.equals(Type.FLOAT_TYPE)) {
            if (loadOpcode == Opcodes.ILOAD) instructions.add(new InsnNode(Opcodes.I2F));
            else if (loadOpcode == Opcodes.LLOAD) instructions.add(new InsnNode(Opcodes.L2F));
            else if (loadOpcode == Opcodes.DLOAD) instructions.add(new InsnNode(Opcodes.D2F));
            else if (loadOpcode == Opcodes.ALOAD) instructions.add(ASMUtils.getCast(parameter));
        } else if (parameter.equals(Type.DOUBLE_TYPE)) {
            if (loadOpcode == Opcodes.ILOAD) instructions.add(new InsnNode(Opcodes.I2D));
            else if (loadOpcode == Opcodes.LLOAD) instructions.add(new InsnNode(Opcodes.L2D));
            else if (loadOpcode == Opcodes.FLOAD) instructions.add(new InsnNode(Opcodes.F2D));
            else if (loadOpcode == Opcodes.ALOAD) instructions.add(ASMUtils.getCast(parameter));
        } else if (parameter.equals(Type.getType(Boolean.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Byte.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2B));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BYTE_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(new InsnNode(Opcodes.I2B));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BYTE_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(new InsnNode(Opcodes.I2B));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BYTE_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(new InsnNode(Opcodes.I2B));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.BYTE_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Short.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2S));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.SHORT_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(new InsnNode(Opcodes.I2S));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.SHORT_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(new InsnNode(Opcodes.I2S));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.SHORT_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(new InsnNode(Opcodes.I2S));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.SHORT_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Character.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2C));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.CHAR_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(new InsnNode(Opcodes.I2C));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.CHAR_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(new InsnNode(Opcodes.I2C));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.CHAR_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(new InsnNode(Opcodes.I2C));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.CHAR_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Integer.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(ASMUtils.getPrimitiveToObject(Type.INT_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2I));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.INT_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2I));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.INT_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2I));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.INT_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Long.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2L));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.LONG_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(ASMUtils.getPrimitiveToObject(Type.LONG_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2L));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.LONG_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2L));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.LONG_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Float.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2F));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.FLOAT_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2F));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.FLOAT_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(ASMUtils.getPrimitiveToObject(Type.FLOAT_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(new InsnNode(Opcodes.D2F));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.FLOAT_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else if (parameter.equals(Type.getType(Double.class))) {
            if (loadOpcode == Opcodes.ILOAD) {
                instructions.add(new InsnNode(Opcodes.I2D));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.DOUBLE_TYPE));
            } else if (loadOpcode == Opcodes.LLOAD) {
                instructions.add(new InsnNode(Opcodes.L2D));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.DOUBLE_TYPE));
            } else if (loadOpcode == Opcodes.FLOAD) {
                instructions.add(new InsnNode(Opcodes.F2D));
                instructions.add(ASMUtils.getPrimitiveToObject(Type.DOUBLE_TYPE));
            } else if (loadOpcode == Opcodes.DLOAD) {
                instructions.add(ASMUtils.getPrimitiveToObject(Type.DOUBLE_TYPE));
            } else if (loadOpcode == Opcodes.ALOAD) {
                instructions.add(ASMUtils.getCast(parameter));
            }
        } else {
            if (loadOpcode == Opcodes.ILOAD) instructions.add(ASMUtils.getPrimitiveToObject(Type.INT_TYPE));
            else if (loadOpcode == Opcodes.LLOAD) instructions.add(ASMUtils.getPrimitiveToObject(Type.LONG_TYPE));
            else if (loadOpcode == Opcodes.FLOAD) instructions.add(ASMUtils.getPrimitiveToObject(Type.FLOAT_TYPE));
            else if (loadOpcode == Opcodes.DLOAD) instructions.add(ASMUtils.getPrimitiveToObject(Type.DOUBLE_TYPE));
            instructions.add(ASMUtils.getCast(parameter));
        }
        return instructions;
    }

    private int getLoadOpcode(final int opcode) {
        switch (opcode) {
            case Opcodes.ISTORE:
                return Opcodes.ILOAD;
            case Opcodes.LSTORE:
                return Opcodes.LLOAD;
            case Opcodes.FSTORE:
                return Opcodes.FLOAD;
            case Opcodes.DSTORE:
                return Opcodes.DLOAD;
            case Opcodes.ASTORE:
                return Opcodes.ALOAD;
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
                return opcode;
        }
        throw new IllegalStateException("Unknown opcode " + opcode);
    }

}
