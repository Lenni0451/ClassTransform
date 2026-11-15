package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.Types;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.local.LocalRefUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The coprocessor for {@link CLocalVariable} parameters.<br>
 * Annotated parameters are removed from the transformer method and passed through an object array.
 * <p>
 * Annotated parameters are resolved at the injection position of every call by name, ordinal or index, falling back
 * to type inference when no local variable table is present. Values are read before the call and, unless the parameter
 * is a reference or not modifiable, written back afterwards. Reference parameters (such as {@code CLocalRef}) share a
 * boxed slot so changes become visible immediately.
 * <p>
 * Setting {@code print = true} logs the available local variables and aborts the transformation.
 */
public class CLocalVariableCoprocessor implements IAnnotationCoprocessor {

    private CoprocessorUtils.AnnotatedParameter[] parameters;
    private ParameterInfo[] parameterInfos;

    @Override
    public MethodNode preprocess(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, ClassNode transformer, MethodNode transformerMethod) {
        this.parameters = CoprocessorUtils.getAnnotatedParameters(transformerMethod, CLocalVariable.class);
        if (this.parameters == null) return transformerMethod; //No annotated parameters found
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (int i = 0; i < this.parameters.length; i++) {
            CoprocessorUtils.AnnotatedParameter parameter = this.parameters[i];
            if (parameter == null) continue;
            CLocalVariable annotation = AnnotationParser.parse(CLocalVariable.class, transformerManager, AnnotationUtils.listToMap(parameter.getAnnotation().values));
            Type targetType = parameter.getType();
            boolean reference = LocalRefUtils.isRef(parameter.getType());
            if (reference) {
                Type typeArgument = CoprocessorUtils.getTypeArgument(transformerMethod, i);
                targetType = LocalRefUtils.getTargetType(parameter.getType(), typeArgument);
                if (targetType == null) {
                    throw new TransformerException(transformerMethod, transformer, "reference parameter " + parameter.getAnnotationIndex() + " must have a concrete type argument")
                            .help("e.g. CLocalRef<String>");
                }
            }
            parameterInfos.add(new ParameterInfo(parameter, annotation, targetType, reference));
        }
        this.parameterInfos = parameterInfos.toArray(new ParameterInfo[0]);
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
        ASMUtils.addParameters(transformerMethod, Types.type(Object[].class)); //Add the object array parameter again

        for (MethodInsnNode transformerCall : transformerMethodCalls) {
            transformerCall.desc = transformerMethod.desc;
            int position = transformedMethod.instructions.indexOf(transformerCall);
            ResolvedParameter[] resolvedParameters = this.resolveParameters(transformedClass, transformer, transformerMethod, transformedMethod, position);
            InsnList before = new InsnList();
            InsnList after = new InsnList();
            this.buildInstructions(transformedMethod, resolvedParameters, before, after);
            transformedMethod.instructions.insertBefore(transformerCall, before);
            transformedMethod.instructions.insert(transformerCall, after);
        }
    }

    private ResolvedParameter[] resolveParameters(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode transformedMethod, final int position) {
        ResolvedParameter[] resolvedParameters = new ResolvedParameter[this.parameterInfos.length];
        for (int i = 0; i < this.parameterInfos.length; i++) {
            ParameterInfo parameterInfo = this.parameterInfos[i];
            if (parameterInfo.annotation.print()) {
                LocalVariableLookup.print(transformedClass, transformedMethod, position, parameterInfo.targetType, parameterInfo.annotation.argsOnly());
                throw new TransformerException(transformerMethod, transformer, "injection aborted because local variables are being printed")
                        .help("Set print = false after choosing the local variable");
            }
            LocalVariableLookup.Local local = LocalVariableLookup.find(transformedClass, transformer, transformerMethod, transformedMethod, position, parameterInfo.annotation, parameterInfo.parameter.getType(), parameterInfo.targetType, parameterInfo.parameter.getName());
            resolvedParameters[i] = new ResolvedParameter(parameterInfo, local);
        }
        return resolvedParameters;
    }

    private void buildInstructions(final MethodNode transformedMethod, final ResolvedParameter[] resolvedParameters, final InsnList before, final InsnList after) {
        Map<Integer, RefSlot> refSlots = new LinkedHashMap<>();
        int nextSlot = resolvedParameters.length;
        for (ResolvedParameter resolvedParameter : resolvedParameters) {
            if (!resolvedParameter.parameterInfo.reference) continue;
            int localIndex = resolvedParameter.local.getIndex();
            if (!refSlots.containsKey(localIndex)) refSlots.put(localIndex, new RefSlot(nextSlot++, resolvedParameter.local.getType()));
        }

        int arrayIndex = ASMUtils.getFreeVarIndex(transformedMethod);
        before.add(ASMUtils.intPush(resolvedParameters.length + refSlots.size()));
        before.add(new TypeInsnNode(Opcodes.ANEWARRAY, Types.internalName(Object.class)));
        before.add(new InsnNode(Opcodes.DUP));
        before.add(new VarInsnNode(Opcodes.ASTORE, arrayIndex));

        for (Map.Entry<Integer, RefSlot> entry : refSlots.entrySet()) {
            RefSlot refSlot = entry.getValue();
            before.add(new InsnNode(Opcodes.DUP));
            before.add(ASMUtils.intPush(refSlot.slot));
            before.add(new VarInsnNode(refSlot.type.getOpcode(Opcodes.ILOAD), entry.getKey()));
            AbstractInsnNode box = ASMUtils.getPrimitiveToObject(refSlot.type);
            if (box != null) before.add(box);
            before.add(new InsnNode(Opcodes.AASTORE));
        }

        for (ResolvedParameter resolvedParameter : resolvedParameters) {
            ParameterInfo parameterInfo = resolvedParameter.parameterInfo;
            before.add(new InsnNode(Opcodes.DUP));
            before.add(ASMUtils.intPush(parameterInfo.parameter.getAnnotationIndex()));
            if (parameterInfo.reference) {
                String refClass = parameterInfo.parameter.getType().getInternalName();
                before.add(new TypeInsnNode(Opcodes.NEW, refClass));
                before.add(new InsnNode(Opcodes.DUP));
                before.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                before.add(ASMUtils.intPush(refSlots.get(resolvedParameter.local.getIndex()).slot));
                before.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, refClass, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object[].class), Type.INT_TYPE), false));
            } else {
                before.add(new VarInsnNode(resolvedParameter.local.getType().getOpcode(Opcodes.ILOAD), resolvedParameter.local.getIndex()));
                AbstractInsnNode box = ASMUtils.getPrimitiveToObject(resolvedParameter.local.getType());
                if (box != null) before.add(box);
            }
            before.add(new InsnNode(Opcodes.AASTORE));
        }

        for (ResolvedParameter resolvedParameter : resolvedParameters) {
            ParameterInfo parameterInfo = resolvedParameter.parameterInfo;
            if (parameterInfo.reference || !parameterInfo.annotation.modifiable()) continue;
            after.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
            after.add(ASMUtils.intPush(parameterInfo.parameter.getAnnotationIndex()));
            after.add(new InsnNode(Opcodes.AALOAD));
            after.add(ASMUtils.getCast(parameterInfo.parameter.getType()));
            after.add(new VarInsnNode(ASMUtils.getStoreOpcode(parameterInfo.parameter.getType()), resolvedParameter.local.getIndex()));
        }
        for (Map.Entry<Integer, RefSlot> entry : refSlots.entrySet()) {
            RefSlot refSlot = entry.getValue();
            after.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
            after.add(ASMUtils.intPush(refSlot.slot));
            after.add(new InsnNode(Opcodes.AALOAD));
            after.add(ASMUtils.getCast(refSlot.type));
            after.add(new VarInsnNode(ASMUtils.getStoreOpcode(refSlot.type), entry.getKey()));
        }
    }


    private static class ParameterInfo {
        private final CoprocessorUtils.AnnotatedParameter parameter;
        private final CLocalVariable annotation;
        private final Type targetType;
        private final boolean reference;

        private ParameterInfo(final CoprocessorUtils.AnnotatedParameter parameter, final CLocalVariable annotation, final Type targetType, final boolean reference) {
            this.parameter = parameter;
            this.annotation = annotation;
            this.targetType = targetType;
            this.reference = reference;
        }
    }

    private static class ResolvedParameter {
        private final ParameterInfo parameterInfo;
        private final LocalVariableLookup.Local local;

        private ResolvedParameter(final ParameterInfo parameterInfo, final LocalVariableLookup.Local local) {
            this.parameterInfo = parameterInfo;
            this.local = local;
        }
    }

    private static class RefSlot {
        private final int slot;
        private final Type type;

        private RefSlot(final int slot, final Type type) {
            this.slot = slot;
            this.type = type;
        }
    }

}
