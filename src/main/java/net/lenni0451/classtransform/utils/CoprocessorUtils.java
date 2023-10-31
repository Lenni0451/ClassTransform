package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Utils for annotation coprocessors.
 */
@ParametersAreNonnullByDefault
public class CoprocessorUtils {

    /**
     * Get all annotated parameters of a method.
     *
     * @param methodNode      The method to get the parameters from
     * @param annotationClass The annotation class to search for
     * @return An array of annotated parameters or null if none were found
     */
    @Nullable
    public static AnnotatedParameter[] getAnnotatedParameters(final MethodNode methodNode, final Class<?> annotationClass) {
        Optional<AnnotationNode[]> optionalAnnotations = AnnotationUtils.findParameterAnnotations(methodNode, annotationClass);
        if (!optionalAnnotations.isPresent()) return null; //No annotated parameters found
        AnnotationNode[] annotations = optionalAnnotations.get();
        Type[] types = Types.argumentTypes(methodNode.desc); //The current method argument types
        int[] indices = ASMUtils.getParameterIndices(methodNode); //The argument variable indices
        if (types.length != annotations.length) {
            //Ensure that the parameter count matches the annotation count
            throw new RuntimeException("Parameter count does not match annotation count");
        }

        AnnotatedParameter[] annotatedParameters = new AnnotatedParameter[annotations.length];
        int x = 0; //The index of the annotated parameter within all annotated parameters
        for (int i = 0; i < annotations.length; i++) annotatedParameters[i] = new AnnotatedParameter(x++, indices[i], types[i], annotations[i]);
        return annotatedParameters;
    }

    /**
     * Merge all annotated parameters into an array.<br>
     * The array will be the last parameter of the method.
     *
     * @param methodNode          The method to merge the parameters from
     * @param annotatedParameters The annotated parameters to merge
     * @return The variable index of the array
     */
    public static int mergeParametersToArray(final MethodNode methodNode, final AnnotatedParameter[] annotatedParameters) {
        Type[] types = Types.argumentTypes(methodNode.desc); //The current method argument types
        int[] typeIndices = ASMUtils.getParameterIndices(methodNode); //The argument variable indices
        Map<Integer, Integer> indexMappings = new HashMap<>(); //Mappings from old variable index to new variable index. Used for non annotated parameters.
        Map<Integer, AnnotatedParameter> arrayMappings = new HashMap<>(); //Mappings from old variable index to annotated parameter

        //Calculate mappings
        List<Type> newTypes = new ArrayList<>();
        int currentIndex = Modifier.isStatic(methodNode.access) ? 0 : 1; //The current variable index. Will be the new array index after the following loop.
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            int index = typeIndices[i];
            AnnotatedParameter parameter = annotatedParameters[i];
            if (parameter == null) {
                //Not annotated
                newTypes.add(type);
                indexMappings.put(index, currentIndex);
                currentIndex += type.getSize();
            } else {
                //Annotated
                arrayMappings.put(index, parameter);
            }
        }

        //New method descriptor
        newTypes.add(Types.type(Object[].class));
        methodNode.desc = Types.methodDescriptor(Types.returnType(methodNode), newTypes.toArray()); //Set new method descriptor

        //Map variables
        //Because at least one parameter is annotated we don't need to move the other local variables because we can't collide with them.
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) insn;
                if (indexMappings.containsKey(varInsnNode.var)) {
                    varInsnNode.var = indexMappings.get(varInsnNode.var);
                } else if (arrayMappings.containsKey(varInsnNode.var)) {
                    AnnotatedParameter parameter = arrayMappings.get(varInsnNode.var);
                    InsnList insns = new InsnList();
                    if (varInsnNode.getOpcode() >= Opcodes.ISTORE && varInsnNode.getOpcode() <= Opcodes.ASTORE) {
                        AbstractInsnNode objectCast = ASMUtils.getPrimitiveToObject(parameter.type);
                        if (objectCast != null) insns.add(objectCast); //Convert primitive to object
                        insns.add(new VarInsnNode(Opcodes.ALOAD, currentIndex)); //Load array
                        insns.add(new InsnNode(Opcodes.SWAP)); //Swap array and value
                        insns.add(ASMUtils.intPush(parameter.annotationIndex)); //int -> Array index
                        insns.add(new InsnNode(Opcodes.SWAP)); //Swap array index and value
                        insns.add(new InsnNode(Opcodes.AASTORE)); //Store value in array
                    } else if (varInsnNode.getOpcode() >= Opcodes.ILOAD && varInsnNode.getOpcode() <= Opcodes.ALOAD) {
                        insns.add(new VarInsnNode(Opcodes.ALOAD, currentIndex)); //Load array
                        insns.add(ASMUtils.intPush(parameter.annotationIndex)); //int -> Array index
                        insns.add(new InsnNode(Opcodes.AALOAD)); //Load element from array
                        insns.add(ASMUtils.getCast(parameter.type)); //Cast to correct type (also converts object to primitive)
                    } else {
                        throw new IllegalStateException("Unknown var insn opcode: " + varInsnNode.getOpcode());
                    }
                    methodNode.instructions.insert(varInsnNode, insns);
                    methodNode.instructions.remove(varInsnNode);
                }
            } else if (insn instanceof IincInsnNode) {
                IincInsnNode iincInsnNode = (IincInsnNode) insn;
                if (indexMappings.containsKey(iincInsnNode.var)) {
                    iincInsnNode.var = indexMappings.get(iincInsnNode.var);
                } else if (arrayMappings.containsKey(iincInsnNode.var)) {
                    AnnotatedParameter parameter = arrayMappings.get(iincInsnNode.var);
                    InsnList insns = new InsnList();
                    insns.add(new VarInsnNode(Opcodes.ALOAD, currentIndex)); //Load array
                    insns.add(ASMUtils.intPush(parameter.annotationIndex)); //int -> Array index
                    insns.add(new InsnNode(Opcodes.DUP2)); //Duplicate array and index
                    insns.add(new InsnNode(Opcodes.AALOAD)); //Load element from array
                    insns.add(ASMUtils.getCast(Type.INT_TYPE)); //Cast to primitive int
                    insns.add(ASMUtils.intPush(iincInsnNode.incr)); //int -> Increment
                    insns.add(new InsnNode(Opcodes.IADD)); //Add increment to value
                    insns.add(ASMUtils.getPrimitiveToObject(Type.INT_TYPE)); //Convert primitive to object
                    insns.add(new InsnNode(Opcodes.AASTORE)); //Store value in array
                    methodNode.instructions.insert(iincInsnNode, insns);
                    methodNode.instructions.remove(iincInsnNode);
                }
            }
        }
        return currentIndex;
    }


    /**
     * A parameter annotated with an annotation and its variable index and type.
     */
    public static class AnnotatedParameter {
        private final int annotationIndex;
        private final int index;
        private final Type type;
        private final AnnotationNode annotation;

        private AnnotatedParameter(final int annotationIndex, final int index, final Type type, final AnnotationNode annotation) {
            this.annotationIndex = annotationIndex;
            this.index = index;
            this.type = type;
            this.annotation = annotation;
        }

        public int getAnnotationIndex() {
            return this.annotationIndex;
        }

        public int getIndex() {
            return this.index;
        }

        public Type getType() {
            return this.type;
        }

        public AnnotationNode getAnnotation() {
            return this.annotation;
        }
    }

}
