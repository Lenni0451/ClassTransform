package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.Types;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.attributes.SharedVariableAttribute;
import net.lenni0451.classtransform.utils.local.LocalRefUtils;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The coprocessor for {@link CShared} parameters.<br>
 * Shared values are stored in a local variable of the target method and are therefore isolated per invocation.
 * <p>
 * The values are tracked in a per-method {@link SharedVariableAttribute}: the first use initializes a local holding
 * the default value of the annotated type (or a new reference instance), later uses must agree on the type and
 * reference-ness. Non-reference values are written back after every call, while reference parameters share the same
 * boxed instance.
 */
public class CSharedCoprocessor implements IAnnotationCoprocessor {

    private CoprocessorUtils.AnnotatedParameter[] parameters;
    private ParameterInfo[] parameterInfos;

    @Override
    public MethodNode preprocess(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, ClassNode transformer, MethodNode transformerMethod) {
        this.parameters = CoprocessorUtils.getAnnotatedParameters(transformerMethod, CShared.class);
        if (this.parameters == null) return transformerMethod; //No annotated parameters found
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (int i = 0; i < this.parameters.length; i++) {
            CoprocessorUtils.AnnotatedParameter parameter = this.parameters[i];
            if (parameter == null) continue;
            CShared annotation = AnnotationParser.parse(CShared.class, transformerManager, AnnotationUtils.listToMap(parameter.getAnnotation().values));
            boolean reference = LocalRefUtils.isRef(parameter.getType());
            Type targetType = parameter.getType();
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

        SharedVariableAttribute attribute = this.getAttribute(transformedMethod);
        ParsedSharedVariable[] parsedSharedVariables = this.initializeSharedVariables(transformerManager, transformer, transformerMethod, attribute, transformedMethod);
        int targetArrayIndex = ASMUtils.getFreeVarIndex(transformedMethod);
        InsnList before = new InsnList();
        InsnList after = new InsnList();

        { //Instructions inserted before the method call
            before.add(ASMUtils.intPush(parsedSharedVariables.length)); //Push the array size
            before.add(new TypeInsnNode(Opcodes.ANEWARRAY, Types.internalName(Object.class))); //Create the array
            before.add(new InsnNode(Opcodes.DUP)); //Duplicate the array
            before.add(new VarInsnNode(Opcodes.ASTORE, targetArrayIndex)); //Store the array in a local variable
            for (int i = 0; i < parsedSharedVariables.length; i++) {
                ParsedSharedVariable parsedSharedVariable = parsedSharedVariables[i];
                before.add(new InsnNode(Opcodes.DUP)); //Duplicate the array
                before.add(ASMUtils.intPush(i)); //Push the array index
                if (parsedSharedVariable.parameterInfo.reference) {
                    before.add(new VarInsnNode(Opcodes.ALOAD, parsedSharedVariable.sharedVariable.getVariableIndex())); //Load the shared reference
                } else {
                    before.add(new VarInsnNode(parsedSharedVariable.parameterInfo.parameter.getType().getOpcode(Opcodes.ILOAD), parsedSharedVariable.sharedVariable.getVariableIndex())); //Load the variable
                    AbstractInsnNode cast = ASMUtils.getPrimitiveToObject(parsedSharedVariable.parameterInfo.parameter.getType());
                    if (cast != null) before.add(cast); //Convert primitive to object (if needed)
                }
                before.add(new InsnNode(Opcodes.AASTORE)); //Store the variable in the array
            }
        }
        { //Instructions inserted after the method call
            for (int i = 0; i < parsedSharedVariables.length; i++) {
                ParsedSharedVariable parsedSharedVariable = parsedSharedVariables[i];
                if (parsedSharedVariable.parameterInfo.reference) continue; //Reference changes are visible immediately
                after.add(new VarInsnNode(Opcodes.ALOAD, targetArrayIndex)); //Load the array
                after.add(ASMUtils.intPush(i)); //Push the array index
                after.add(new InsnNode(Opcodes.AALOAD)); //Load the variable from the array
                after.add(ASMUtils.getCast(parsedSharedVariable.parameterInfo.parameter.getType()));
                after.add(new VarInsnNode(ASMUtils.getStoreOpcode(parsedSharedVariable.parameterInfo.parameter.getType()), parsedSharedVariable.sharedVariable.getVariableIndex())); //Store the variable back
            }
        }
        for (MethodInsnNode transformerCall : transformerMethodCalls) {
            transformerCall.desc = transformerMethod.desc; //Set the correct method descriptor (with the array parameter)
            transformedMethod.instructions.insertBefore(transformerCall, ASMUtils.cloneInsnList(before));
            transformedMethod.instructions.insert(transformerCall, ASMUtils.cloneInsnList(after));
        }
    }

    private SharedVariableAttribute getAttribute(final MethodNode methodNode) {
        if (methodNode.attrs == null) methodNode.attrs = new ArrayList<>();
        SharedVariableAttribute attribute = null;
        for (Attribute attr : methodNode.attrs) {
            if (attr instanceof SharedVariableAttribute) {
                //Found the already parsed attribute
                attribute = (SharedVariableAttribute) attr;
                break;
            } else if (attr.type.equals(SharedVariableAttribute.NAME)) {
                //Found the attribute but it is not parsed yet
                SharedVariableAttribute newAttr;
                try {
                    newAttr = new SharedVariableAttribute(attr);
                } catch (Throwable t) {
                    //If it can not be parsed, create a new one
                    newAttr = new SharedVariableAttribute();
                }
                methodNode.attrs.remove(attr);
                methodNode.attrs.add(newAttr);
                attribute = newAttr;
                break;
            }
        }
        if (attribute == null) {
            //No attribute found, create a new one
            attribute = new SharedVariableAttribute();
            methodNode.attrs.add(attribute);
        }
        return attribute;
    }

    private ParsedSharedVariable[] initializeSharedVariables(final TransformerManager transformerManager, final ClassNode transformer, final MethodNode transformerMethod, final SharedVariableAttribute attribute, final MethodNode transformedMethod) {
        List<ParsedSharedVariable> parsedSharedVariables = new ArrayList<>();
        for (ParameterInfo parameterInfo : this.parameterInfos) {
            CShared annotation = parameterInfo.annotation;
            SharedVariableAttribute.SharedVariable sharedVariable = attribute.getVariable(transformer.name, annotation.value(), annotation.namespace(), annotation.global());
            if (sharedVariable == null) {
                //The shared variable with this name is not yet initialized
                sharedVariable = attribute.addVariable(transformer.name, annotation.value(), annotation.namespace(), ASMUtils.getFreeVarIndex(transformedMethod), parameterInfo.targetType, annotation.global(), parameterInfo.reference);
                InsnList initialization = this.getDefaultInstructions(transformer, transformerMethod, parameterInfo, sharedVariable.getVariableIndex());
                sharedVariable.addInitialization(initialization.toArray());
                transformedMethod.instructions.insert(initialization);
            } else {
                //The shared variable with this name is already initialized
                if (sharedVariable.isReference() != parameterInfo.reference) {
                    throw new TransformerException(transformerMethod, transformer, "uses shared variable '" + annotation.value() + "' both as a value and as a reference");
                }
                if (!sharedVariable.getType().equals(parameterInfo.targetType)) {
                    throw new TransformerException(transformerMethod, transformer, "uses shared variable '" + annotation.value() + "' with the wrong type (" + parameterInfo.targetType + " != " + sharedVariable.getType() + ")");
                }
            }
            parsedSharedVariables.add(new ParsedSharedVariable(parameterInfo, sharedVariable));
        }
        return parsedSharedVariables.toArray(new ParsedSharedVariable[0]);
    }

    private InsnList getDefaultInstructions(final ClassNode transformer, final MethodNode transformerMethod, final ParameterInfo parameterInfo, final int index) {
        InsnList insns = new InsnList();
        if (parameterInfo.reference) {
            String referenceClass = parameterInfo.parameter.getType().getInternalName();
            insns.add(new TypeInsnNode(Opcodes.NEW, referenceClass));
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, referenceClass, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false));
        } else {
            switch (parameterInfo.targetType.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    insns.add(new InsnNode(Opcodes.ICONST_0));
                    break;
                case Type.LONG:
                    insns.add(new InsnNode(Opcodes.LCONST_0));
                    break;
                case Type.FLOAT:
                    insns.add(new InsnNode(Opcodes.FCONST_0));
                    break;
                case Type.DOUBLE:
                    insns.add(new InsnNode(Opcodes.DCONST_0));
                    break;
                case Type.ARRAY:
                case Type.OBJECT:
                    insns.add(new InsnNode(Opcodes.ACONST_NULL));
                    break;
                default:
                    throw new TransformerException(transformerMethod, transformer, "uses an unknown type: " + parameterInfo.targetType);
            }
        }
        insns.add(new VarInsnNode(parameterInfo.reference ? Opcodes.ASTORE : ASMUtils.getStoreOpcode(parameterInfo.targetType), index));
        return insns;
    }


    private static class ParameterInfo {
        private final CoprocessorUtils.AnnotatedParameter parameter;
        private final CShared annotation;
        private final Type targetType;
        private final boolean reference;

        private ParameterInfo(final CoprocessorUtils.AnnotatedParameter parameter, final CShared annotation, final Type targetType, final boolean reference) {
            this.parameter = parameter;
            this.annotation = annotation;
            this.targetType = targetType;
            this.reference = reference;
        }
    }

    private static class ParsedSharedVariable {
        private final ParameterInfo parameterInfo;
        private final SharedVariableAttribute.SharedVariable sharedVariable;

        private ParsedSharedVariable(final ParameterInfo parameterInfo, final SharedVariableAttribute.SharedVariable sharedVariable) {
            this.parameterInfo = parameterInfo;
            this.sharedVariable = sharedVariable;
        }
    }

}
