package net.lenni0451.classtransform.expression;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import net.lenni0451.classtransform.utils.log.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The identifiers available to expressions.
 * Maps the names used in expressions and defined by {@code @CDefinition} to member and type matchers. The table
 * always contains the primitive type names and the special {@code length} member for array length loads.
 */
@ParametersAreNonnullByDefault
public class ExpressionIdentifiers {

    private final Map<String, List<MemberDefinition>> members = new HashMap<>();
    private final Map<String, List<TypeDefinition>> types = new HashMap<>();
    private final ClassNode targetClass;
    private final MethodNode targetMethod;
    @Nullable
    private final TransformerManager transformerManager;

    /**
     * Creates an identifier table without class hierarchy lookups.
     *
     * @param targetClass  the transformed class
     * @param targetMethod the target method
     */
    public ExpressionIdentifiers(final ClassNode targetClass, final MethodNode targetMethod) {
        this(targetClass, targetMethod, null);
    }

    /**
     * Creates an identifier table.
     *
     * @param targetClass        the transformed class
     * @param targetMethod       the target method
     * @param transformerManager the manager used to resolve supertypes, or null to match owners literally
     */
    public ExpressionIdentifiers(final ClassNode targetClass, final MethodNode targetMethod, @Nullable final TransformerManager transformerManager) {
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
        this.transformerManager = transformerManager;
        this.addType("byte", type -> type.equals(Type.BYTE_TYPE));
        this.addType("char", type -> type.equals(Type.CHAR_TYPE));
        this.addType("double", type -> type.equals(Type.DOUBLE_TYPE));
        this.addType("float", type -> type.equals(Type.FLOAT_TYPE));
        this.addType("int", type -> type.equals(Type.INT_TYPE));
        this.addType("long", type -> type.equals(Type.LONG_TYPE));
        this.addType("short", type -> type.equals(Type.SHORT_TYPE));
        this.addType("boolean", type -> type.equals(Type.BOOLEAN_TYPE));
        this.addType("void", type -> type.equals(Type.VOID_TYPE));
        this.addMember("length", node -> node.getInsn() != null && node.getInsn().getOpcode() == Opcodes.ARRAYLENGTH);
    }

    /**
     * Registers the members, types and locals of a {@code @CDefinition}.
     * <p>
     * Multiple declarations may share an identifier; a value matches if any of them matches.
     *
     * @param id      the identifier to define
     * @param methods the method declarations to match
     * @param fields  the field declarations to match
     * @param types   the types to match
     * @param locals  the local variable definitions to match
     */
    public void addDefinition(final String id, final List<String> methods, final List<String> fields, final List<Type> types, final List<AnnotationNode> locals) {
        for (String method : methods) {
            this.addMember(id, new MethodDefinition(method));
        }
        for (String field : fields) {
            this.addMember(id, new FieldDefinition(field));
        }
        for (Type type : types) {
            this.addType(id, candidate -> candidate.equals(type));
        }
        for (AnnotationNode local : locals) {
            this.addMember(id, new LocalDefinition(local, this.targetMethod));
        }
    }

    private void printLocals(final int position, final boolean argsOnly) {
        Logger.info("Local variables of {}.{}{} at instruction {}:", this.targetClass.name, this.targetMethod.name, this.targetMethod.desc, position);
        if (this.targetMethod.localVariables == null || this.targetMethod.localVariables.isEmpty()) {
            Logger.info("  no local variable table found");
            return;
        }
        for (LocalVariableNode local : this.targetMethod.localVariables) {
            int start = this.targetMethod.instructions.indexOf(local.start);
            int end = this.targetMethod.instructions.indexOf(local.end);
            if (position < start || position >= end) continue;
            if (argsOnly && !this.isArgument(local.index)) continue;
            Logger.info("  index={} name={} type={} scope=[{}, {}] parameter={}", local.index, local.name, Type.getType(local.desc).getClassName(), start, end, this.isArgument(local.index));
        }
    }

    private boolean isArgument(final int index) {
        int first = (this.targetMethod.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        int end = first;
        for (Type argument : Type.getArgumentTypes(this.targetMethod.desc)) end += argument.getSize();
        return index >= first && index < end;
    }

    /**
     * Tests if an identifier has member definitions.
     *
     * @param id the identifier
     * @return whether the identifier is declared as a member
     */
    public boolean memberExists(final String id) {
        return this.members.containsKey(id);
    }

    /**
     * Tests if an identifier has type definitions.
     *
     * @param id the identifier
     * @return whether the identifier is declared as a type
     */
    public boolean typeExists(final String id) {
        return this.types.containsKey(id);
    }

    /**
     * Tests if a value matches a declared member.
     * <p>
     * Owner patterns are matched as wildcards and, if a transformer manager is available, also against the
     * supertypes of the value's owner.
     *
     * @param id   the identifier of the member
     * @param node the value to match
     * @return whether any definition of the identifier matches the value
     * @throws IllegalArgumentException if the identifier is not declared
     */
    public boolean matchesMember(final String id, final ExpressionFlow.FlowValue node) {
        List<MemberDefinition> definitions = this.members.get(id);
        if (definitions == null) throw new IllegalArgumentException("Use of undeclared identifier '" + id + "'");
        for (MemberDefinition definition : definitions) {
            if (definition.matches(node)) return true;
        }
        return false;
    }

    /**
     * Tests if a method handle matches a declared member.
     *
     * @param id     The identifier of the member
     * @param handle The method handle to match
     * @return If the handle matches the member
     * @throws IllegalArgumentException if the identifier is not declared
     */
    public boolean matchesMember(final String id, final Handle handle) {
        List<MemberDefinition> definitions = this.members.get(id);
        if (definitions == null) throw new IllegalArgumentException("Use of undeclared identifier '" + id + "'");
        for (MemberDefinition definition : definitions) {
            if (definition.matches(handle)) return true;
        }
        return false;
    }

    /**
     * Returns the lambda metafactory information of a flow value.
     *
     * @param node The flow value
     * @return The information or null if the value is not a lambda
     */
    @Nullable
    public LMFInfo getLMFInfo(final ExpressionFlow.FlowValue node) {
        return LMFInfo.get(node, this.targetClass.name);
    }

    /**
     * Tests if a type matches a declared type identifier.
     *
     * @param id   the identifier of the type
     * @param type the type to match
     * @return whether any definition of the identifier matches the type
     * @throws IllegalArgumentException if the identifier is not declared
     */
    public boolean matchesType(final String id, final Type type) {
        List<TypeDefinition> definitions = this.types.get(id);
        if (definitions == null) throw new IllegalArgumentException("Use of undeclared identifier '" + id + "'");
        for (TypeDefinition definition : definitions) {
            if (definition.matches(type)) return true;
        }
        return false;
    }

    private void addMember(final String id, final MemberDefinition definition) {
        this.members.computeIfAbsent(id, k -> new ArrayList<>()).add(definition);
    }

    private void addType(final String id, final TypeDefinition definition) {
        this.types.computeIfAbsent(id, k -> new ArrayList<>()).add(definition);
    }

    private boolean matchesOwner(@Nullable final String pattern, final String owner) {
        if (matchesWildcard(pattern, owner)) return true;
        if (pattern == null || owner == null || this.transformerManager == null) return false;
        return this.isSuperType(owner, ASMUtils.dot(pattern), new HashSet<>());
    }

    private boolean isSuperType(final String owner, final String expected, final Set<String> visited) {
        String name = ASMUtils.dot(owner);
        if (name.equals(expected)) return true;
        if (!visited.add(name)) return false;
        try {
            ClassNode node = ASMUtils.fromBytes(this.transformerManager.getClassProvider().getClass(name));
            if (node.superName != null && this.isSuperType(node.superName, expected, visited)) return true;
            if (node.interfaces != null) {
                for (String anInterface : node.interfaces) {
                    if (this.isSuperType(anInterface, expected, visited)) return true;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return false;
    }

    private static boolean matchesWildcard(@Nullable final String pattern, final String value) {
        if (pattern == null || pattern.isEmpty() || pattern.equals("*")) return true;
        StringBuilder regex = new StringBuilder("(?s)^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') regex.append(".*");
            else if (c == '?') regex.append('.');
            else regex.append(Pattern.quote(String.valueOf(c)));
        }
        regex.append('$');
        return value.matches(regex.toString());
    }


    /**
     * A matcher for one declaration of a member identifier.
     */
    public interface MemberDefinition {
        /**
         * Tests if a value matches this declaration.
         *
         * @param node the value to match
         * @return whether the value matches
         */
        boolean matches(ExpressionFlow.FlowValue node);

        /**
         * Tests if a method handle matches this declaration.
         * <p>
         * Field and local declarations cannot match handles, so the default returns {@code false}.
         *
         * @param handle the method handle to match
         * @return whether the handle matches
         */
        default boolean matches(final Handle handle) {
            return false;
        }
    }

    /**
     * A matcher for a type identifier.
     */
    public interface TypeDefinition {
        /**
         * Tests if a type matches this declaration.
         *
         * @param type the type to match
         * @return whether the type matches
         */
        boolean matches(Type type);
    }


    private class MethodDefinition implements MemberDefinition {
        private final String owner;
        private final String name;
        private final String desc;

        private MethodDefinition(final String declaration) {
            MemberDeclaration member = ASMUtils.splitMemberDeclaration(declaration);
            if (member != null) {
                this.owner = member.getOwner();
                this.name = member.getName();
                this.desc = member.getDesc();
            } else {
                int index = declaration.indexOf('(');
                if (index < 0) {
                    this.owner = null;
                    this.name = declaration;
                    this.desc = "*";
                } else {
                    this.owner = null;
                    this.name = declaration.substring(0, index);
                    this.desc = declaration.substring(index);
                }
            }
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node) {
            if (!(node.getInsn() instanceof MethodInsnNode)) return false;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            return this.matches(method.owner, method.name, method.desc);
        }

        @Override
        public boolean matches(final Handle handle) {
            return this.matches(handle.getOwner(), handle.getName(), handle.getDesc());
        }

        private boolean matches(final String owner, final String name, final String desc) {
            return ExpressionIdentifiers.this.matchesOwner(this.owner, owner) && matchesWildcard(this.name, name) && matchesWildcard(this.desc, desc);
        }
    }

    private class FieldDefinition implements MemberDefinition {
        private final String owner;
        private final String name;
        private final String desc;

        private FieldDefinition(final String declaration) {
            MemberDeclaration member = ASMUtils.splitMemberDeclaration(declaration);
            if (member != null) {
                this.owner = member.getOwner();
                this.name = member.getName();
                this.desc = member.getDesc();
            } else {
                int index = declaration.indexOf(':');
                if (index < 0) {
                    this.owner = null;
                    this.name = declaration;
                    this.desc = "*";
                } else {
                    this.owner = null;
                    this.name = declaration.substring(0, index);
                    this.desc = declaration.substring(index + 1);
                }
            }
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node) {
            if (!(node.getInsn() instanceof FieldInsnNode)) return false;
            FieldInsnNode field = (FieldInsnNode) node.getInsn();
            return ExpressionIdentifiers.this.matchesOwner(this.owner, field.owner) && matchesWildcard(this.name, field.name) && matchesWildcard(this.desc, field.desc);
        }
    }

    private class LocalDefinition implements MemberDefinition {
        private final Type type;
        private final List<String> names;
        private final int ordinal;
        private final int index;
        private final boolean argsOnly;
        private final boolean print;
        private final MethodNode targetMethod;

        private LocalDefinition(final AnnotationNode annotation, final MethodNode targetMethod) {
            Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
            Object typeValue = values.get("type");
            this.type = typeValue instanceof Type && !((Type) typeValue).equals(Type.VOID_TYPE) ? (Type) typeValue : null;
            this.names = new ArrayList<>();
            Object nameValue = values.get("name");
            if (nameValue instanceof String && !((String) nameValue).isEmpty()) this.names.add((String) nameValue);
            Object namesValue = values.get("names");
            if (namesValue instanceof List) {
                for (Object name : (List<?>) namesValue) if (name instanceof String) this.names.add((String) name);
            }
            this.ordinal = values.get("ordinal") instanceof Integer ? (Integer) values.get("ordinal") : -1;
            this.index = values.get("index") instanceof Integer ? (Integer) values.get("index") : -1;
            this.argsOnly = Boolean.TRUE.equals(values.get("argsOnly"));
            this.print = Boolean.TRUE.equals(values.get("print"));
            this.targetMethod = targetMethod;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node) {
            if (!(node.getInsn() instanceof VarInsnNode)) return false;
            if (this.print) {
                int position = ExpressionIdentifiers.this.targetMethod.instructions.indexOf(node.getInsn());
                if (position >= 0 && node.getInsn().getOpcode() >= Opcodes.ISTORE && node.getInsn().getOpcode() <= Opcodes.ASTORE) position++;
                ExpressionIdentifiers.this.printLocals(position, this.argsOnly);
                throw new IllegalArgumentException("Injection aborted because local variables are being printed");
            }
            VarInsnNode var = (VarInsnNode) node.getInsn();
            if (this.index >= 0 && var.var != this.index) return false;
            if (this.type != null && !this.type.equals(node.getType()) && !this.type.equals(this.localType(var.var))) return false;
            if (this.index >= 0) return this.argsOnly ? this.isArgument(var.var) : true;
            List<LocalVariableNode> candidates = this.getCandidates(var.var);
            if (candidates.isEmpty()) return this.type == null || this.type.equals(node.getType());
            if (!this.names.isEmpty()) {
                for (LocalVariableNode candidate : candidates) {
                    if (this.names.contains(candidate.name)) return true;
                }
                return false;
            }
            if (this.ordinal >= 0) {
                List<LocalVariableNode> typed = new ArrayList<>();
                for (LocalVariableNode candidate : candidates) {
                    if (this.type == null || Type.getType(candidate.desc).equals(this.type)) typed.add(candidate);
                }
                typed.sort(Comparator.comparingInt(candidate -> candidate.index));
                return this.ordinal < typed.size() && typed.get(this.ordinal).index == var.var;
            }
            return true;
        }

        private List<LocalVariableNode> getCandidates(final int var) {
            List<LocalVariableNode> result = new ArrayList<>();
            if (this.targetMethod.localVariables == null) return result;
            for (LocalVariableNode local : this.targetMethod.localVariables) {
                if (local.index != var) continue;
                if (this.argsOnly && !this.isArgument(var)) continue;
                result.add(local);
            }
            return result;
        }

        private boolean isArgument(final int index) {
            int first = (this.targetMethod.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
            int end = first;
            for (Type argument : Type.getArgumentTypes(this.targetMethod.desc)) end += argument.getSize();
            return index >= first && index < end;
        }

        @Nullable
        private Type localType(final int var) {
            if (this.targetMethod.localVariables == null) return null;
            for (LocalVariableNode local : this.targetMethod.localVariables) {
                if (local.index == var) return Type.getType(local.desc);
            }
            return null;
        }
    }

}
