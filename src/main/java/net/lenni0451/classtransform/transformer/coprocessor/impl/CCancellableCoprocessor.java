package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CCancellable;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

import static net.lenni0451.classtransform.utils.Types.returnType;

/**
 * The coprocessor for {@link CCancellable} parameters.<br>
 * It creates a callback for every handler call and returns from the target method if the callback was cancelled.
 */
public class CCancellableCoprocessor implements IAnnotationCoprocessor {

    private CoprocessorUtils.AnnotatedParameter[] parameters;

    @Override
    public MethodNode preprocess(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, ClassNode transformer, MethodNode transformerMethod) {
        this.parameters = CoprocessorUtils.getAnnotatedParameters(transformerMethod, CCancellable.class);
        if (this.parameters == null) return transformerMethod; //No annotated parameters found
        for (CoprocessorUtils.AnnotatedParameter parameter : this.parameters) {
            if (parameter == null) continue;
            if (!parameter.getType().equals(Type.getType(InjectionCallback.class))) {
                throw new TransformerException(transformerMethod, transformer, "cancellable parameter must be of type " + InjectionCallback.class.getName());
            }
        }
        transformedMethod.signature = null; //Remove the signature as it is no longer valid
        CoprocessorUtils.mergeParametersToArray(transformerMethod, this.parameters);
        return transformerMethod;
    }

    @Override
    public MethodNode transform(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, ClassNode transformer, MethodNode transformerMethod) {
        if (this.parameters == null) return transformerMethod;
        ASMUtils.cutParameters(transformerMethod, 1); //Remove the object array parameter again
        return transformerMethod;
    }

    @Override
    public void postprocess(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, List<MethodInsnNode> transformerMethodCalls, ClassNode transformer, MethodNode transformerMethod) {
        if (this.parameters == null) return;
        if (transformedMethod.name.equals("<init>") || transformedMethod.name.equals("<clinit>")) {
            throw new TransformerException(transformerMethod, transformer, "cancellation is not supported in constructors");
        }
        ASMUtils.addParameters(transformerMethod, Types.type(Object[].class)); //Add the object array parameter again

        Type targetReturnType = returnType(transformedMethod.desc);
        Type handlerReturnType = returnType(transformerMethod.desc);
        int parameterCount = 0;
        for (CoprocessorUtils.AnnotatedParameter parameter : this.parameters) {
            if (parameter != null) parameterCount++;
        }

        for (MethodInsnNode transformerCall : transformerMethodCalls) {
            Object inheritedContext = InjectionPlan.getContext(transformerCall);
            CancellableContext context = inheritedContext instanceof CancellableContext ? (CancellableContext) inheritedContext : null;
            int callbackIndex;
            InsnList before = new InsnList();
            if (context == null) {
                callbackIndex = ASMUtils.getFreeVarIndex(transformedMethod);
                this.createCallback(before, targetReturnType, callbackIndex);
            } else {
                callbackIndex = context.callbackIndex;
            }
            before.add(ASMUtils.intPush(parameterCount));
            before.add(new TypeInsnNode(Opcodes.ANEWARRAY, Types.internalName(Object.class)));
            for (CoprocessorUtils.AnnotatedParameter parameter : this.parameters) {
                if (parameter == null) continue;
                before.add(new InsnNode(Opcodes.DUP));
                before.add(ASMUtils.intPush(parameter.getAnnotationIndex()));
                before.add(new VarInsnNode(Opcodes.ALOAD, callbackIndex));
                before.add(new InsnNode(Opcodes.AASTORE));
            }
            transformedMethod.instructions.insertBefore(transformerCall, before);

            InsnList after = new InsnList();
            this.createCancellation(after, targetReturnType, handlerReturnType, callbackIndex);
            transformedMethod.instructions.insert(transformerCall, after);
            transformerCall.desc = transformerMethod.desc;
            InjectionPlan.setContext(transformerCall, new CancellableContext(callbackIndex));
        }
    }

    private void createCallback(final InsnList instructions, final Type targetReturnType, final int callbackIndex) {
        instructions.add(new TypeInsnNode(Opcodes.NEW, Types.internalName(InjectionCallback.class)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new InsnNode(Opcodes.ICONST_1));
        if (targetReturnType.equals(Type.VOID_TYPE)) {
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Types.internalName(InjectionCallback.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE), false));
        } else {
            this.pushDefaultValue(instructions, targetReturnType);
            AbstractInsnNode box = ASMUtils.getPrimitiveToObject(targetReturnType);
            if (box != null) instructions.add(box);
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Types.internalName(InjectionCallback.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE, Type.getType(Object.class)), false));
        }
        instructions.add(new VarInsnNode(Opcodes.ASTORE, callbackIndex));
    }

    private void createCancellation(final InsnList instructions, final Type targetReturnType, final Type handlerReturnType, final int callbackIndex) {
        LabelNode notCancelled = new LabelNode();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackIndex));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Types.internalName(InjectionCallback.class), "isCancelled", Type.getMethodDescriptor(Type.BOOLEAN_TYPE), false));
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, notCancelled));
        if (!handlerReturnType.equals(Type.VOID_TYPE)) {
            instructions.add(new InsnNode(handlerReturnType.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        }
        if (targetReturnType.equals(Type.VOID_TYPE)) {
            instructions.add(new InsnNode(Opcodes.RETURN));
        } else {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, callbackIndex));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Types.internalName(InjectionCallback.class), "getReturnValue", Type.getMethodDescriptor(Type.getType(Object.class)), false));
            instructions.add(ASMUtils.getCast(targetReturnType));
            instructions.add(new InsnNode(ASMUtils.getReturnOpcode(targetReturnType)));
        }
        instructions.add(notCancelled);
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


    /**
     * The callback local shared by an operation wrapper chain.
     */
    private static class CancellableContext {
        private final int callbackIndex;

        private CancellableContext(final int callbackIndex) {
            this.callbackIndex = callbackIndex;
        }
    }

}
