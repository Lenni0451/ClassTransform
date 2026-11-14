package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoprocessorUtils;
import net.lenni0451.classtransform.utils.Types;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import net.lenni0451.classtransform.utils.log.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Resolve local variables for {@link CLocalVariable} parameters at a specific injection point.
 */
@ParametersAreNonnullByDefault
public class LocalVariableLookup {

    static Local find(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode targetMethod, final int position, final CLocalVariable annotation, final Type parameterType, final Type targetType, @Nullable final String parameterName) {
        IParsedAnnotation parsedAnnotation = (IParsedAnnotation) annotation;
        List<String> names = getNames(annotation, parsedAnnotation);
        boolean strict = annotation.strict() || !targetType.equals(parameterType);
        if (!names.isEmpty()) return findByName(transformer, transformerMethod, targetMethod, position, annotation, names, targetType, strict);
        if (parsedAnnotation.wasSet("ordinal")) return findOrdinal(transformer, transformerMethod, targetMethod, position, annotation, targetType);
        if (parsedAnnotation.wasSet("index")) return findIndex(transformer, transformerMethod, transformedClass, targetMethod, position, annotation, parameterType, targetType, strict);
        if (annotation.implicit()) return findImplicit(transformer, transformerMethod, transformedClass, targetMethod, position, annotation, targetType);
        if (parameterName != null && !strict) return findByName(transformer, transformerMethod, targetMethod, position, annotation, Collections.singletonList(parameterName), targetType, false);
        return findImplicit(transformer, transformerMethod, transformedClass, targetMethod, position, annotation, targetType);
    }

    /**
     * Find a local variable using the discriminator rules of Mixin's {@code @ModifyVariable}.<br>
     * The {@code this} reference is never considered a candidate.
     *
     * @param transformedClass  The transformed class, or null if unknown
     * @param transformer       The transformer class
     * @param transformerMethod The transformer method
     * @param targetMethod      The target method
     * @param position          The instruction index to search at
     * @param names             The candidate names, or null
     * @param ordinal           The ordinal of the variable type, or -1
     * @param index             The variable index, or -1
     * @param argsOnly          If only method arguments should be considered
     * @param targetType        The required variable type
     * @return The resolved local variable
     */
    public static Local findVariable(@Nullable final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode targetMethod, final int position, @Nullable final String[] names, final int ordinal, final int index, final boolean argsOnly, final Type targetType) {
        List<Candidate> candidates = candidates(transformedClass, targetMethod, position, argsOnly, true);
        if (ordinal >= 0) {
            List<Candidate> matching = new ArrayList<>();
            for (Candidate candidate : candidates) {
                if (candidate.type.equals(targetType)) matching.add(candidate);
            }
            if (ordinal >= matching.size()) {
                throw new TransformerException(transformerMethod, transformer, "could not find local variable ordinal " + ordinal + " with type " + targetType.getClassName() + " in scope")
                        .help("Set print = true to list all available local variables");
            }
            return new Local(matching.get(ordinal));
        }
        if (index >= 0) {
            Candidate match = null;
            for (Candidate candidate : candidates) {
                if (candidate.index != index) continue;
                if (match == null || candidate.start > match.start) match = candidate;
            }
            if (match == null) {
                throw new TransformerException(transformerMethod, transformer, "could not find local variable " + index + " in scope")
                        .help("Set print = true to list all available local variables");
            }
            if (!match.type.equals(targetType)) {
                throw new TransformerException(transformerMethod, transformer, "local variable " + index + " has type " + match.type.getClassName() + " but " + targetType.getClassName() + " is required");
            }
            return new Local(match);
        }
        if (names != null && names.length > 0) {
            if (targetMethod.localVariables == null || targetMethod.localVariables.isEmpty()) {
                throw new TransformerException(transformerMethod, transformer, "can not get the local variable index by name because no local variable table is present")
                        .help("Compile the target class with debug information or specify the index manually");
            }
            for (String name : names) {
                Candidate match = null;
                for (Candidate candidate : candidates) {
                    if (!name.equals(candidate.name) || !candidate.type.equals(targetType)) continue;
                    if (match == null || candidate.start > match.start) match = candidate;
                }
                if (match != null) return new Local(match);
            }
            throw new TransformerException(transformerMethod, transformer, "could not find a local variable with the name(s) " + Arrays.toString(names) + " and type " + targetType.getClassName() + " in scope")
                    .help("Set print = true to list all available local variables");
        }
        List<Candidate> matching = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.type.equals(targetType)) matching.add(candidate);
        }
        if (matching.size() == 1) return new Local(matching.get(0));
        if (matching.isEmpty()) {
            throw new TransformerException(transformerMethod, transformer, "could not find a local variable with type " + targetType.getClassName() + " in scope")
                    .help("Set print = true to list all available local variables");
        }
        throw new TransformerException(transformerMethod, transformer, "found multiple local variables with type " + targetType.getClassName() + " in scope: " + matching)
                .help("Set name, ordinal or index to select one of them");
    }

    /**
     * Get all local variables in scope at the given instruction.
     *
     * @param transformedClass The transformed class, or null if unknown
     * @param targetMethod     The target method
     * @param position         The instruction index
     * @return The local variables sorted by their variable index
     */
    public static List<Local> getLocalsInScope(@Nullable final ClassNode transformedClass, final MethodNode targetMethod, final int position) {
        List<Local> locals = new ArrayList<>();
        for (Candidate candidate : candidates(transformedClass, targetMethod, position, false, false)) {
            locals.add(new Local(candidate));
        }
        return locals;
    }

    public static void print(final ClassNode transformedClass, final MethodNode targetMethod, final int position, final Type targetType, final boolean argsOnly) {
        Logger.info("Local variables at {}.{}{} (instruction {}):", transformedClass.name, targetMethod.name, targetMethod.desc, position);
        List<Candidate> candidates = getInScope(targetMethod, position, argsOnly);
        if (candidates.isEmpty()) {
            Logger.info("  no local variables found in the local variable table");
            Map<Integer, Type> inferred = inferAvailable(transformedClass, targetMethod, position, argsOnly);
            for (Map.Entry<Integer, Type> entry : inferred.entrySet()) {
                Logger.info("  index={} type={} (inferred, no local variable table)", entry.getKey(), entry.getValue().getClassName());
            }
        } else {
            for (Candidate candidate : candidates) {
                Logger.info("  index={} name={} type={} scope=[{}, {}] parameter={}", candidate.index, candidate.name == null ? "<unknown>" : candidate.name, candidate.type.getClassName(), candidate.start, candidate.end, isArgumentIndex(targetMethod, candidate.index));
            }
        }
        if (targetType != null) Logger.info("  target type: {}", targetType.getClassName());
    }

    private static List<String> getNames(final CLocalVariable annotation, final IParsedAnnotation parsedAnnotation) {
        List<String> names = new ArrayList<>();
        if (parsedAnnotation.wasSet("name") && !annotation.name().isEmpty()) names.add(annotation.name());
        Collections.addAll(names, annotation.names());
        return names;
    }

    private static Local findByName(final ClassNode transformer, final MethodNode transformerMethod, final MethodNode targetMethod, final int position, final CLocalVariable annotation, final List<String> names, final Type targetType, final boolean strict) {
        if (targetMethod.localVariables == null || targetMethod.localVariables.isEmpty()) {
            throw new TransformerException(transformerMethod, transformer, "can not get the local variable index by name because no local variable table is present")
                    .help("Compile the target class with debug information or specify the index manually");
        }
        List<Candidate> candidates = getInScope(targetMethod, position, annotation.argsOnly());
        for (String name : names) {
            Candidate match = null;
            for (Candidate candidate : candidates) {
                if (!name.equals(candidate.name)) continue;
                if (strict && !candidate.type.equals(targetType)) continue;
                if (match == null || candidate.start > match.start) match = candidate;
            }
            if (match != null) return new Local(match);
        }
        throw new TransformerException(transformerMethod, transformer, "could not find a local variable with the name(s) " + names + " and type " + targetType.getClassName() + " in scope")
                .help("Set print = true on the parameter to list all available local variables");
    }

    private static Local findOrdinal(final ClassNode transformer, final MethodNode transformerMethod, final MethodNode targetMethod, final int position, final CLocalVariable annotation, final Type targetType) {
        List<Candidate> candidates = new ArrayList<>();
        for (Candidate candidate : getInScope(targetMethod, position, annotation.argsOnly())) {
            if (candidate.type.equals(targetType)) candidates.add(candidate);
        }
        if (candidates.isEmpty() && (targetMethod.localVariables == null || targetMethod.localVariables.isEmpty())) {
            Map<Integer, Type> inferred = inferAvailable(null, targetMethod, position, annotation.argsOnly());
            for (Map.Entry<Integer, Type> entry : inferred.entrySet()) {
                if (entry.getValue().equals(targetType)) candidates.add(new Candidate(entry.getKey(), entry.getValue(), null, 0, targetMethod.instructions.size()));
            }
        }
        candidates.sort(Comparator.comparingInt(candidate -> candidate.index));
        int ordinal = annotation.ordinal();
        if (ordinal < 0 || ordinal >= candidates.size()) {
            throw new TransformerException(transformerMethod, transformer, "could not find local variable ordinal " + ordinal + " with type " + targetType.getClassName() + " in scope")
                    .help("Set print = true on the parameter to list all available local variables");
        }
        return new Local(candidates.get(ordinal));
    }

    private static Local findIndex(final ClassNode transformer, final MethodNode transformerMethod, final ClassNode transformedClass, final MethodNode targetMethod, final int position, final CLocalVariable annotation, final Type parameterType, final Type targetType, final boolean strict) {
        int index = annotation.index();
        if (annotation.argsOnly() && !isArgumentIndex(targetMethod, index)) {
            throw new TransformerException(transformerMethod, transformer, "local variable " + index + " is not a method parameter but argsOnly is set");
        }
        Candidate candidate = null;
        for (Candidate inScope : getInScope(targetMethod, position, annotation.argsOnly())) {
            if (inScope.index != index) continue;
            if (candidate == null || inScope.start > candidate.start) candidate = inScope;
        }
        Type type = candidate == null ? null : candidate.type;
        if (annotation.loadOpcode() != -1) {
            type = getType(annotation.loadOpcode());
        } else if (type == null) {
            type = inferType(transformedClass, targetMethod, index);
        }
        if (type == null || (type.equals(Type.getType(Object.class)) && strict && targetType.getSort() >= Type.ARRAY)) {
            type = strict ? targetType : parameterType;
        }
        if (strict && !type.equals(targetType)) {
            throw new TransformerException(transformerMethod, transformer, "local variable " + index + " has type " + type.getClassName() + " but " + targetType.getClassName() + " is required");
        }
        return new Local(new Candidate(index, type, candidate == null ? null : candidate.name, candidate == null ? 0 : candidate.start, candidate == null ? targetMethod.instructions.size() : candidate.end));
    }

    private static Local findImplicit(final ClassNode transformer, final MethodNode transformerMethod, final ClassNode transformedClass, final MethodNode targetMethod, final int position, final CLocalVariable annotation, final Type targetType) {
        List<Candidate> candidates = new ArrayList<>();
        for (Candidate candidate : getInScope(targetMethod, position, annotation.argsOnly())) {
            if (candidate.type.equals(targetType)) candidates.add(candidate);
        }
        if (candidates.isEmpty() && (targetMethod.localVariables == null || targetMethod.localVariables.isEmpty())) {
            Map<Integer, Type> inferred = inferAvailable(transformedClass, targetMethod, position, annotation.argsOnly());
            for (Map.Entry<Integer, Type> entry : inferred.entrySet()) {
                if (entry.getValue().equals(targetType)) candidates.add(new Candidate(entry.getKey(), entry.getValue(), null, 0, targetMethod.instructions.size()));
            }
        }
        if (candidates.size() == 1) return new Local(candidates.get(0));
        if (candidates.isEmpty()) {
            throw new TransformerException(transformerMethod, transformer, "could not find a local variable with type " + targetType.getClassName() + " in scope")
                    .help("Set print = true on the parameter to list all available local variables");
        }
        candidates.sort(Comparator.comparingInt(candidate -> candidate.index));
        throw new TransformerException(transformerMethod, transformer, "found multiple local variables with type " + targetType.getClassName() + " in scope: " + candidates)
                .help("Set name, ordinal or index on the parameter to select one of them");
    }

    private static List<Candidate> candidates(@Nullable final ClassNode transformedClass, final MethodNode method, final int position, final boolean argsOnly, final boolean excludeThis) {
        List<Candidate> candidates = getInScope(method, position, argsOnly);
        if (candidates.isEmpty()) {
            Map<Integer, Type> inferred = inferAvailable(transformedClass, method, position, argsOnly);
            for (Map.Entry<Integer, Type> entry : inferred.entrySet()) {
                candidates.add(new Candidate(entry.getKey(), entry.getValue(), null, 0, method.instructions.size()));
            }
            candidates.sort(Comparator.comparingInt(candidate -> candidate.index));
        }
        if (excludeThis) {
            int baseArgIndex = Modifier.isStatic(method.access) ? 0 : 1;
            candidates.removeIf(candidate -> candidate.index < baseArgIndex);
        }
        return candidates;
    }

    private static List<Candidate> getInScope(final MethodNode method, final int position, final boolean argsOnly) {
        if (method.localVariables == null || method.localVariables.isEmpty()) return Collections.emptyList();
        Map<Integer, Candidate> candidates = new HashMap<>();
        for (LocalVariableNode localVariable : method.localVariables) {
            if (argsOnly && !isArgumentIndex(method, localVariable.index)) continue;
            int start = instructionIndex(method, localVariable.start);
            int end = instructionIndex(method, localVariable.end);
            if (start < 0) start = 0;
            if (end < 0) end = method.instructions.size();
            if (position < start || position >= end) continue;
            Candidate candidate = new Candidate(localVariable.index, Type.getType(localVariable.desc), localVariable.name, start, end);
            Candidate existing = candidates.get(candidate.index);
            if (existing == null || candidate.start >= existing.start) candidates.put(candidate.index, candidate);
        }
        List<Candidate> sorted = new ArrayList<>(candidates.values());
        sorted.sort(Comparator.comparingInt(candidate -> candidate.index));
        return sorted;
    }

    private static Map<Integer, Type> inferAvailable(@Nullable final ClassNode transformedClass, final MethodNode method, final int position, final boolean argsOnly) {
        Map<Integer, Type> available = new HashMap<>();
        if (!Modifier.isStatic(method.access)) {
            if (transformedClass != null) available.put(0, Type.getObjectType(transformedClass.name));
            else available.put(0, Type.getType(Object.class));
        }
        int[] parameterIndices = ASMUtils.getParameterIndices(method);
        Type[] parameterTypes = Types.argumentTypes(method);
        for (int i = 0; i < parameterIndices.length; i++) available.put(parameterIndices[i], parameterTypes[i]);
        for (int i = 0; i < position; i++) {
            AbstractInsnNode instruction = method.instructions.get(i);
            if (instruction instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) instruction;
                if (varInsnNode.getOpcode() >= Opcodes.ISTORE && varInsnNode.getOpcode() <= Opcodes.ASTORE) available.put(varInsnNode.var, getType(varInsnNode.getOpcode()));
            } else if (instruction instanceof IincInsnNode) {
                available.put(((IincInsnNode) instruction).var, Type.INT_TYPE);
            }
        }
        if (argsOnly) available.entrySet().removeIf(entry -> !isArgumentIndex(method, entry.getKey()));
        return available;
    }

    @Nullable
    private static Type inferType(@Nullable final ClassNode transformedClass, final MethodNode method, final int index) {
        if (!Modifier.isStatic(method.access) && index == 0 && transformedClass != null) return Type.getObjectType(transformedClass.name);
        int[] parameterIndices = ASMUtils.getParameterIndices(method);
        Type[] parameterTypes = Types.argumentTypes(method);
        for (int i = 0; i < parameterIndices.length; i++) {
            if (parameterIndices[i] == index) return parameterTypes[i];
        }
        Type type = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof VarInsnNode && ((VarInsnNode) instruction).var == index) {
                Type instructionType = getType(instruction.getOpcode());
                if (type == null) type = instructionType;
                else if (!instructionType.equals(type)) return null;
            } else if (instruction instanceof IincInsnNode && ((IincInsnNode) instruction).var == index) {
                if (type == null) type = Type.INT_TYPE;
                else if (!type.equals(Type.INT_TYPE)) return null;
            }
        }
        return type;
    }

    private static boolean isArgumentIndex(final MethodNode method, final int index) {
        int firstArgument = Modifier.isStatic(method.access) ? 0 : 1;
        return index >= firstArgument && index < CoprocessorUtils.getFirstNonArgIndex(method);
    }

    private static int instructionIndex(final MethodNode method, final LabelNode label) {
        return method.instructions.indexOf(label);
    }

    private static Type getType(final int opcode) {
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
                return Type.getType(Object.class);
            default:
                throw new IllegalArgumentException("Unknown opcode " + opcode);
        }
    }


    public static class Local {

        private final int index;
        private final Type type;

        private Local(final Candidate candidate) {
            this.index = candidate.index;
            this.type = candidate.type;
        }

        public int getIndex() {
            return this.index;
        }

        public Type getType() {
            return this.type;
        }

    }

    private static class Candidate {

        private final int index;
        private final Type type;
        @Nullable
        private final String name;
        private final int start;
        private final int end;

        private Candidate(final int index, final Type type, @Nullable final String name, final int start, final int end) {
            this.index = index;
            this.type = type;
            this.name = name;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "index=" + this.index + " name=" + this.name + " type=" + this.type.getClassName();
        }

    }

}
