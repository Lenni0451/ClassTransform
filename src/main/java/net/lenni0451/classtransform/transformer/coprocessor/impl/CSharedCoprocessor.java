package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.Types;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.attributes.SharedVariableAttribute;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class CSharedCoprocessor implements IAnnotationCoprocessor {

    private CoprocessorUtils.AnnotatedParameter[] parameters;

    @Override
    public MethodNode preprocess(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, ClassNode transformer, MethodNode transformerMethod) {
        this.parameters = CoprocessorUtils.getAnnotatedParameters(transformerMethod, CShared.class);
        if (this.parameters == null) return transformerMethod; //No annotated parameters found
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
        ParsedSharedVariable[] parsedSharedVariables = this.initializeSharedVariables(transformerManager, transformer, attribute, transformedMethod);
        int targetArrayIndex = ASMUtils.getFreeVarIndex(transformedMethod);
        InsnList before = new InsnList();
        InsnList after = new InsnList();

        { //Instructions inserted before the method call
            before.add(ASMUtils.intPush(parsedSharedVariables.length)); //Push the array size
            before.add(new TypeInsnNode(Opcodes.ANEWARRAY, Types.internalName(Object.class))); //Create the array
            before.add(new InsnNode(Opcodes.DUP)); //Duplicate the array
            before.add(new VarInsnNode(Opcodes.ASTORE, targetArrayIndex)); //Store the array in a local variable
            for (int i = 0; i < parsedSharedVariables.length; i++) {
                //Go through all shared variables and add them to the array
                ParsedSharedVariable parsedSharedVariable = parsedSharedVariables[i];
                before.add(new InsnNode(Opcodes.DUP)); //Duplicate the array
                before.add(ASMUtils.intPush(i)); //Push the array index
                before.add(new VarInsnNode(parsedSharedVariable.parameter.getType().getOpcode(Opcodes.ILOAD), parsedSharedVariable.sharedVariable.getVariableIndex())); //Load the variable
                AbstractInsnNode cast = ASMUtils.getPrimitiveToObject(parsedSharedVariable.parameter.getType());
                if (cast != null) before.add(cast); //Convert primitive to object (if needed)
                before.add(new InsnNode(Opcodes.AASTORE)); //Store the variable in the array
            }
        }
        { //Instructions inserted after the method call
            for (int i = 0; i < parsedSharedVariables.length; i++) {
                ParsedSharedVariable parsedSharedVariable = parsedSharedVariables[i];
                after.add(new VarInsnNode(Opcodes.ALOAD, targetArrayIndex)); //Load the array
                after.add(ASMUtils.intPush(i)); //Push the array index
                after.add(new InsnNode(Opcodes.AALOAD)); //Load the variable from the array
                InsnList cast = ASMUtils.getCast(parsedSharedVariable.sharedVariable.getType());
                after.add(cast);
                after.add(new VarInsnNode(parsedSharedVariable.parameter.getType().getOpcode(Opcodes.ISTORE), parsedSharedVariable.sharedVariable.getVariableIndex())); //Store the variable back
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

    private ParsedSharedVariable[] initializeSharedVariables(final TransformerManager transformerManager, final ClassNode transformer, final SharedVariableAttribute attribute, final MethodNode transformedMethod) {
        List<ParsedSharedVariable> parsedSharedVariables = new ArrayList<>();
        for (CoprocessorUtils.AnnotatedParameter parameter : this.parameters) {
            if (parameter == null) continue;
            CShared annotation = AnnotationParser.parse(CShared.class, transformerManager, AnnotationUtils.listToMap(parameter.getAnnotation().values));
            SharedVariableAttribute.SharedVariable sharedVariable = attribute.getVariableIndex(transformer.name, annotation.value(), annotation.global());
            if (sharedVariable == null) {
                //The shared variable with this name is not yet initialized
                sharedVariable = attribute.addVariable(transformer.name, annotation.value(), ASMUtils.getFreeVarIndex(transformedMethod), parameter.getType(), annotation.global());
                transformedMethod.instructions.insert(this.getDefaultInstructions(parameter.getType(), sharedVariable.getVariableIndex()));
            } else {
                //The shared variable with this name is already initialized
                //Compare the types to ensure that the shared variable is not used with the wrong type
                if (!ASMUtils.compareType(sharedVariable.getType(), parameter.getType())) {
                    throw new IllegalArgumentException("Shared variable '" + annotation.value() + "' has the wrong type: " + parameter.getType() + " != " + sharedVariable.getType());
                }
            }
            parsedSharedVariables.add(new ParsedSharedVariable(parameter, sharedVariable));
        }
        return parsedSharedVariables.toArray(new ParsedSharedVariable[0]);
    }

    private InsnList getDefaultInstructions(final Type type, final int index) {
        InsnList insns = new InsnList();
        switch (type.getSort()) {
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
                throw new IllegalArgumentException("Unknown type: " + type);
        }
        insns.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
        return insns;
    }


    private static class ParsedSharedVariable {
        private final CoprocessorUtils.AnnotatedParameter parameter;
        private final SharedVariableAttribute.SharedVariable sharedVariable;

        private ParsedSharedVariable(final CoprocessorUtils.AnnotatedParameter parameter, final SharedVariableAttribute.SharedVariable sharedVariable) {
            this.parameter = parameter;
            this.sharedVariable = sharedVariable;
        }
    }

}
