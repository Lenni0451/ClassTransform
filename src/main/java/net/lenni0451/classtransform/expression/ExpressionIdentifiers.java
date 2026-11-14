package net.lenni0451.classtransform.expression;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The identifiers available to expressions.
 */
@ParametersAreNonnullByDefault
public class ExpressionIdentifiers {

    private final Map<String, List<MemberDefinition>> members = new HashMap<>();
    private final Map<String, List<TypeDefinition>> types = new HashMap<>();
    private final ClassNode targetClass;
    private final MethodNode targetMethod;

    public ExpressionIdentifiers(final ClassNode targetClass, final MethodNode targetMethod) {
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
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

    public boolean memberExists(final String id) {
        return this.members.containsKey(id);
    }

    public boolean typeExists(final String id) {
        return this.types.containsKey(id);
    }

    public boolean matchesMember(final String id, final ExpressionFlow.FlowValue node) {
        List<MemberDefinition> definitions = this.members.get(id);
        if (definitions == null) throw new IllegalArgumentException("Use of undeclared identifier '" + id + "'");
        for (MemberDefinition definition : definitions) {
            if (definition.matches(node)) return true;
        }
        return false;
    }

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


    public interface MemberDefinition {
        boolean matches(ExpressionFlow.FlowValue node);
    }

    public interface TypeDefinition {
        boolean matches(Type type);
    }


    private static class MethodDefinition implements MemberDefinition {
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
            return matchesWildcard(this.owner, method.owner) && matchesWildcard(this.name, method.name) && matchesWildcard(this.desc, method.desc);
        }
    }

    private static class FieldDefinition implements MemberDefinition {
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
            return matchesWildcard(this.owner, field.owner) && matchesWildcard(this.name, field.name) && matchesWildcard(this.desc, field.desc);
        }
    }

    private static class LocalDefinition implements MemberDefinition {
        private final Type type;
        private final List<String> names;
        private final int ordinal;
        private final int index;
        private final boolean argsOnly;
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
            this.targetMethod = targetMethod;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node) {
            if (!(node.getInsn() instanceof VarInsnNode)) return false;
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
