package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.Types;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CLocalVariableCoprocessor implements IAnnotationCoprocessor {

    private CoprocessorUtils.AnnotatedParameter[] parameters;
    private boolean isAnyModifiable;

    @Override
    public MethodNode preprocess(TransformerManager transformerManager, ClassNode transformedClass, MethodNode transformedMethod, ClassNode transformer, MethodNode transformerMethod) {
        this.parameters = CoprocessorUtils.getAnnotatedParameters(transformerMethod, CLocalVariable.class);
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

        LocalVariable[] localVariables = this.getLocalVariables(transformerManager, transformedMethod);
        int targetArrayIndex = ASMUtils.getFreeVarIndex(transformedMethod);
        InsnList before = new InsnList();
        InsnList after = new InsnList();

        { //Instructions inserted before the method call
            before.add(ASMUtils.intPush(localVariables.length)); //Push the array size
            before.add(new TypeInsnNode(Opcodes.ANEWARRAY, Types.internalName(Object.class))); //Create the array
            if (this.isAnyModifiable) {
                //If any parameter is modifiable, we need to store the array in a local variable for later use
                before.add(new InsnNode(Opcodes.DUP)); //Duplicate the array
                before.add(new VarInsnNode(Opcodes.ASTORE, targetArrayIndex)); //Store the array in a local variable
            }
            for (int i = 0; i < localVariables.length; i++) {
                //Go through all parameters and add them to the array
                LocalVariable localVariable = localVariables[i];
                before.add(new InsnNode(Opcodes.DUP)); //Duplicate the array
                before.add(ASMUtils.intPush(i)); //Push the array index
                before.add(new VarInsnNode(localVariable.type.getOpcode(Opcodes.ILOAD), localVariable.variableIndex)); //Load the variable
                AbstractInsnNode cast = ASMUtils.getPrimitiveToObject(localVariable.parameter.getType());
                if (cast != null) before.add(cast); //Convert primitive to object (if needed)
                before.add(new InsnNode(Opcodes.AASTORE)); //Store the variable in the array
            }
        }
        if (this.isAnyModifiable) { //Instructions inserted after the method call
            for (int i = 0; i < localVariables.length; i++) {
                LocalVariable localVariable = localVariables[i];
                if (localVariable.annotation.modifiable()) {
                    after.add(new VarInsnNode(Opcodes.ALOAD, targetArrayIndex)); //Load the array
                    after.add(ASMUtils.intPush(i)); //Push the array index
                    after.add(new InsnNode(Opcodes.AALOAD)); //Load the variable from the array
                    InsnList cast = ASMUtils.getCast(localVariable.type);
                    after.add(cast);
                    after.add(new VarInsnNode(localVariable.type.getOpcode(Opcodes.ISTORE), localVariable.variableIndex)); //Store the variable back
                }
            }
        }
        for (MethodInsnNode transformerCall : transformerMethodCalls) {
            transformerCall.desc = transformerMethod.desc; //Set the correct method descriptor (with the array parameter)
            transformedMethod.instructions.insertBefore(transformerCall, ASMUtils.cloneInsnList(before));
            transformedMethod.instructions.insert(transformerCall, ASMUtils.cloneInsnList(after));
        }
    }

    private LocalVariable[] getLocalVariables(final TransformerManager transformerManager, final MethodNode methodNode) {
        List<LocalVariable> localVariables = new ArrayList<>();
        for (CoprocessorUtils.AnnotatedParameter parameter : this.parameters) {
            if (parameter == null) continue;
            CLocalVariable annotation = AnnotationParser.parse(CLocalVariable.class, transformerManager, AnnotationUtils.listToMap(parameter.getAnnotation().values));
            IParsedAnnotation parsedAnnotation = (IParsedAnnotation) annotation;
            this.isAnyModifiable |= annotation.modifiable();

            boolean nameSet = parsedAnnotation.wasSet("name");
            boolean indexSet = parsedAnnotation.wasSet("index");
            Integer variableIndex = null;
            if (nameSet || (!indexSet && parameter.getName() != null)) {
                String name = nameSet ? annotation.name() : parameter.getName(); //Use the given name or the original parameter name
                if (methodNode.localVariables == null) {
                    //If no local variable table is present, we can't get the index by name
                    //Only throw an exception if the index was not set manually
                    if (!indexSet) throw new IllegalStateException("Local variables are not available");
                } else {
                    //Try to get the index by name
                    for (LocalVariableNode localVariable : methodNode.localVariables) {
                        if (localVariable.name.equals(name)) {
                            //Found the local variable
                            variableIndex = localVariable.index;
                            break;
                        }
                    }
                }
            }
            if (indexSet && variableIndex == null) variableIndex = annotation.index(); //The index was set manually (and the name was not found/set)
            if (variableIndex == null) throw new IllegalArgumentException("No index or name was set for annotated parameter " + parameter.getAnnotationIndex());

            Type variableType = null;
            if (parsedAnnotation.wasSet("loadOpcode")) {
                //A load opcode was set, so we can get the type from that
                variableType = this.getType(annotation.loadOpcode());
            } else {
                //Go through all instructions and try to find the variable type
                //If a type is found multiple times and is not the same, we can not be sure which one is the correct one
                for (AbstractInsnNode instruction : methodNode.instructions) {
                    if (instruction instanceof VarInsnNode && ((VarInsnNode) instruction).var == variableIndex) {
                        //Found a variable instruction
                        Type opcodeType = this.getType(instruction.getOpcode());
                        if (variableType == null) {
                            variableType = opcodeType;
                        } else if (!opcodeType.equals(variableType)) {
                            throw new IllegalStateException("Local variable " + variableIndex + " has multiple types. Please specify the correct opcode.");
                        }
                    } else if (instruction instanceof IincInsnNode && ((IincInsnNode) instruction).var == variableIndex) {
                        //IInc instructions are always int
                        if (variableType == null) {
                            variableType = Type.INT_TYPE;
                        } else if (!Type.INT_TYPE.equals(variableType)) {
                            throw new IllegalStateException("Local variable " + variableIndex + " has multiple types. Please specify the correct opcode.");
                        }
                    }
                }
                if (variableType == null) throw new IllegalStateException("Local variable " + variableIndex + " could not be resolved");
            }

            //Index and type have been resolved
            localVariables.add(new LocalVariable(variableIndex, variableType, parameter, annotation));
        }
        localVariables.sort(Comparator.comparingInt(o -> o.parameter.getAnnotationIndex()));
        return localVariables.toArray(new LocalVariable[0]);
    }

    private Type getType(final int opcode) {
        switch (opcode) {
            case Opcodes.ILOAD:
            case Opcodes.ISTORE:
                return Type.INT_TYPE;
            case Opcodes.LLOAD:
            case Opcodes.LSTORE:
                return Type.LONG_TYPE;
            case Opcodes.FLOAD:
            case Opcodes.FSTORE:
                return Type.FLOAT_TYPE;
            case Opcodes.DLOAD:
            case Opcodes.DSTORE:
                return Type.DOUBLE_TYPE;
            case Opcodes.ALOAD:
            case Opcodes.ASTORE:
                return Types.type(Object.class);
        }
        throw new IllegalStateException("Unknown opcode " + opcode);
    }


    private static class LocalVariable {
        private final int variableIndex;
        private final Type type;
        private final CoprocessorUtils.AnnotatedParameter parameter;
        private final CLocalVariable annotation;

        private LocalVariable(final int variableIndex, final Type type, final CoprocessorUtils.AnnotatedParameter parameter, final CLocalVariable annotation) {
            this.variableIndex = variableIndex;
            this.type = type;
            this.parameter = parameter;
            this.annotation = annotation;
        }
    }

}
