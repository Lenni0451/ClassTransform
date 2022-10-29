package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.lenni0451.classtransform.utils.tree.TreeClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lenni0451.classtransform.utils.Types.*;

public class ASMUtils {

    /**
     * Get a {@link ClassNode} from the raw bytecode of a class
     *
     * @param bytecode The bytecode of the class
     * @return The loaded {@link ClassNode}
     */
    public static ClassNode fromBytes(final byte[] bytecode) {
        ClassNode node = new ClassNode();
        new ClassReader(bytecode).accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    /**
     * Get the bytecode from a {@link ClassNode}
     *
     * @param node          The {@link ClassNode}
     * @param classProvider The {@link IClassProvider}
     * @return The bytecode of the class
     */
    public static byte[] toBytes(final ClassNode node, final IClassProvider classProvider) {
        TreeClassWriter writer = new TreeClassWriter(classProvider);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Get a {@link MethodNode} from a {@link ClassNode} using the name and descriptor
     *
     * @param classNode The owner {@link ClassNode}
     * @param name      The name of the method
     * @param desc      The descriptor of the method
     * @return The {@link MethodNode}
     */
    public static MethodNode getMethod(final ClassNode classNode, final String name, final String desc) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    /**
     * Get a {@link FieldNode} from a {@link ClassNode} using the name
     *
     * @param classNode The owner {@link ClassNode}
     * @param name      The name of the field
     * @param desc      The descriptor of the field
     * @return The {@link FieldNode}
     */
    public static FieldNode getField(final ClassNode classNode, final String name, final String desc) {
        for (FieldNode field : classNode.fields) {
            if (field.name.equals(name)) return field;
        }
        return null;
    }

    /**
     * Get a {@link List} of {@link MethodNode}s from a {@link ClassNode} using the combined name and descriptor<br>
     * Use <b>*</b> for wildcard<br>
     * e.g. <b>print(Ljava/lang/String;)V</b>
     *
     * @param classNode The owner {@link ClassNode}
     * @param combi     The combined name and descriptor
     * @return The {@link List} of {@link MethodNode}s
     */
    public static List<MethodNode> getMethodsFromCombi(final ClassNode classNode, final String combi) {
        if (combi.isEmpty()) throw new IllegalArgumentException("Combi cannot be empty");
        List<MethodNode> methods = new ArrayList<>();
        if (combi.contains("(")) {
            String name = combi.substring(0, combi.indexOf("("));
            String desc = combi.substring(combi.indexOf("("));
            MethodNode method = getMethod(classNode, name, desc);
            if (method != null) methods.add(method);
        } else {
            String regex = combiToRegex(combi);
            for (MethodNode method : classNode.methods) {
                if (method.name.matches(regex)) methods.add(method);
            }
            if (methods.size() > 1 && methods.stream().anyMatch(method -> (method.access & Opcodes.ACC_SYNTHETIC) == 0)) {
                methods.removeIf(method -> (method.access & Opcodes.ACC_SYNTHETIC) != 0);
            }
        }
        return methods;
    }

    /**
     * Get a {@link List} of {@link FieldNode}s from a {@link ClassNode} using the combined name and descriptor<br>
     * Use <b>*</b> for wildcard<br>
     * e.g. <b>out:Ljava/io/PrintStream;</b>
     *
     * @param classNode The owner {@link ClassNode}
     * @param combi     The combined name and descriptor
     * @return The {@link List} of {@link FieldNode}s
     */
    public static List<FieldNode> getFieldsFromCombi(final ClassNode classNode, final String combi) {
        if (combi.isEmpty()) throw new IllegalArgumentException("Combi cannot be empty");
        List<FieldNode> fields = new ArrayList<>();
        if (combi.contains(":")) {
            String name = combi.substring(0, combi.indexOf(":"));
            String desc = combi.substring(combi.indexOf(":") + 1);
            FieldNode field = getField(classNode, name, desc);
            if (field != null) fields.add(field);
        } else {
            String regex = combiToRegex(combi);
            for (FieldNode field : classNode.fields) {
                if (field.name.matches(regex)) fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Convert a field or method name matching name and descriptor to a regex pattern<br>
     * Use <b>*</b> for wildcard<br>
     * e.g. <b>get*</b> -&gt; <b>^\Qget\E.*$</b>
     *
     * @param combi The name of a field or method
     * @return The regex pattern
     */
    public static String combiToRegex(String combi) {
        if (combi.replace("*", "").isEmpty()) {
            return ".*";
        } else if (combi.contains("*")) {
            boolean startsWith = combi.startsWith("*");
            boolean endsWith = combi.endsWith("*");
            while (combi.startsWith("*")) combi = combi.substring(1);
            while (combi.endsWith("*")) combi = combi.substring(0, combi.length() - 1);
            while (combi.contains("**")) combi = combi.replace("**", "*");
            String[] parts = combi.split("\\*");
            combi = "^";
            if (startsWith) combi += ".*";
            for (int i = 0; i < parts.length; i++) combi += Pattern.quote(parts[i]) + (i == parts.length - 1 ? "" : ".*");
            if (endsWith) combi += ".*";
            combi += "$";
            return combi;
        }
        return Pattern.quote(combi);
    }

    /**
     * Check if the access is lower than another
     *
     * @param toCheck      The access to check
     * @param checkAgainst The access to check against
     * @return True if the access is lower than the other
     */
    public static boolean isAccessLower(final int toCheck, final int checkAgainst) {
        int rank1;
        int rank2;
        if ((toCheck & Opcodes.ACC_PUBLIC) != 0) rank1 = 4;
        else if ((toCheck & Opcodes.ACC_PROTECTED) != 0) rank1 = 3;
        else if ((toCheck & Opcodes.ACC_PRIVATE) == 0) rank1 = 2;
        else rank1 = 1;
        if ((checkAgainst & Opcodes.ACC_PUBLIC) != 0) rank2 = 4;
        else if ((checkAgainst & Opcodes.ACC_PROTECTED) != 0) rank2 = 3;
        else if ((checkAgainst & Opcodes.ACC_PRIVATE) == 0) rank2 = 2;
        else rank2 = 1;
        return rank1 < rank2;
    }

    /**
     * Set the wanted access to a given access mask
     *
     * @param currentAccess The current access mask
     * @param newAccess     The wanted access
     * @return The new access mask
     */
    public static int setAccess(final int currentAccess, final int newAccess) {
        int access = currentAccess;
        access = access & ~Opcodes.ACC_PRIVATE;
        access = access & ~Opcodes.ACC_PROTECTED;
        access = access & ~Opcodes.ACC_PUBLIC;
        access = access | newAccess;
        return access;
    }

    /**
     * Get the needed return opcode for a given {@link Type}
     *
     * @param returnType The return {@link Type} of a method
     * @return The needed return opcode
     */
    public static int getReturnOpcode(final Type returnType) {
        if (returnType.equals(Type.VOID_TYPE)) return Opcodes.RETURN;
        else if (returnType.equals(Type.BOOLEAN_TYPE)) return Opcodes.IRETURN;
        else if (returnType.equals(Type.BYTE_TYPE)) return Opcodes.IRETURN;
        else if (returnType.equals(Type.CHAR_TYPE)) return Opcodes.IRETURN;
        else if (returnType.equals(Type.SHORT_TYPE)) return Opcodes.IRETURN;
        else if (returnType.equals(Type.INT_TYPE)) return Opcodes.IRETURN;
        else if (returnType.equals(Type.FLOAT_TYPE)) return Opcodes.FRETURN;
        else if (returnType.equals(Type.LONG_TYPE)) return Opcodes.LRETURN;
        else if (returnType.equals(Type.DOUBLE_TYPE)) return Opcodes.DRETURN;
        else return Opcodes.ARETURN;
    }

    /**
     * Get the needed load opcode for a given {@link Type}
     *
     * @param type The {@link Type} to get the load opcode for
     * @return The needed load opcode
     */
    public static int getLoadOpcode(final Type type) {
        if (type.equals(Type.BOOLEAN_TYPE)) return Opcodes.ILOAD;
        else if (type.equals(Type.BYTE_TYPE)) return Opcodes.ILOAD;
        else if (type.equals(Type.CHAR_TYPE)) return Opcodes.ILOAD;
        else if (type.equals(Type.SHORT_TYPE)) return Opcodes.ILOAD;
        else if (type.equals(Type.INT_TYPE)) return Opcodes.ILOAD;
        else if (type.equals(Type.FLOAT_TYPE)) return Opcodes.FLOAD;
        else if (type.equals(Type.LONG_TYPE)) return Opcodes.LLOAD;
        else if (type.equals(Type.DOUBLE_TYPE)) return Opcodes.DLOAD;
        else return Opcodes.ALOAD;
    }

    /**
     * Get the needed store opcode for a given {@link Type}
     *
     * @param type The {@link Type} to get the store opcode for
     * @return The needed store opcode
     */
    public static int getStoreOpcode(final Type type) {
        if (type.equals(Type.BOOLEAN_TYPE)) return Opcodes.ISTORE;
        else if (type.equals(Type.BYTE_TYPE)) return Opcodes.ISTORE;
        else if (type.equals(Type.CHAR_TYPE)) return Opcodes.ISTORE;
        else if (type.equals(Type.SHORT_TYPE)) return Opcodes.ISTORE;
        else if (type.equals(Type.INT_TYPE)) return Opcodes.ISTORE;
        else if (type.equals(Type.FLOAT_TYPE)) return Opcodes.FSTORE;
        else if (type.equals(Type.LONG_TYPE)) return Opcodes.LSTORE;
        else if (type.equals(Type.DOUBLE_TYPE)) return Opcodes.DSTORE;
        else return Opcodes.ASTORE;
    }

    /**
     * Get the last empty local variable index
     *
     * @param methodNode The method to get the last empty local variable index for
     * @return The last empty local variable index
     */
    public static int getFreeVarIndex(final MethodNode methodNode) {
        int currentIndex = 0;
        if (!Modifier.isStatic(methodNode.access)) currentIndex = 1;
        for (Type arg : Type.getArgumentTypes(methodNode.desc)) currentIndex += arg.getSize();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction.getOpcode() >= Opcodes.ISTORE && instruction.getOpcode() <= Opcodes.ASTORE || instruction.getOpcode() >= Opcodes.ILOAD && instruction.getOpcode() <= Opcodes.ALOAD) {
                VarInsnNode varInsnNode = (VarInsnNode) instruction;
                if (varInsnNode.var > currentIndex) currentIndex = varInsnNode.var;
            }
        }
        return currentIndex + 2;
    }

    /**
     * Get the code to cast an {@link Object} to any {@link Type}<br>
     * Converts primitive wrapper to their primitive types (e.g. Integer to int)
     *
     * @param wantedType The wanted {@link Type}
     * @return The code to cast an {@link Object} to any {@link Type}
     */
    public static InsnList getCast(final Type wantedType) {
        InsnList list = new InsnList();
        if (wantedType.equals(Type.BOOLEAN_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Boolean));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Boolean, "booleanValue", methodDescriptor(boolean.class), false));
        } else if (wantedType.equals(Type.BYTE_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Byte));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Byte, "byteValue", methodDescriptor(byte.class), false));
        } else if (wantedType.equals(Type.CHAR_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Character));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Character, "charValue", methodDescriptor(char.class), false));
        } else if (wantedType.equals(Type.SHORT_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Short));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Short, "shortValue", methodDescriptor(short.class), false));
        } else if (wantedType.equals(Type.INT_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Integer));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Integer, "intValue", methodDescriptor(int.class), false));
        } else if (wantedType.equals(Type.FLOAT_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Float));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Float, "floatValue", methodDescriptor(float.class), false));
        } else if (wantedType.equals(Type.LONG_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Long));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Long, "longValue", methodDescriptor(long.class), false));
        } else if (wantedType.equals(Type.DOUBLE_TYPE)) {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, IN_Double));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, IN_Double, "doubleValue", methodDescriptor(double.class), false));
        } else {
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, wantedType.getInternalName()));
        }
        return list;
    }

    /**
     * Get the code to wrap a primitive to their wrapper type (e.g. int to Integer)
     *
     * @param primitive The primitive type to wrap
     * @return The code to wrap a primitive to their wrapper type
     */
    public static AbstractInsnNode getPrimitiveToObject(final Type primitive) {
        if (primitive.equals(Type.BOOLEAN_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Boolean, "valueOf", methodDescriptor(Boolean.class, boolean.class), false);
        } else if (primitive.equals(Type.BYTE_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Byte, "valueOf", methodDescriptor(Byte.class, byte.class), false);
        } else if (primitive.equals(Type.SHORT_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Short, "valueOf", methodDescriptor(Short.class, short.class), false);
        } else if (primitive.equals(Type.CHAR_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Character, "valueOf", methodDescriptor(Character.class, char.class), false);
        } else if (primitive.equals(Type.INT_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Integer, "valueOf", methodDescriptor(Integer.class, int.class), false);
        } else if (primitive.equals(Type.LONG_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Long, "valueOf", methodDescriptor(Long.class, long.class), false);
        } else if (primitive.equals(Type.FLOAT_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Float, "valueOf", methodDescriptor(Float.class, float.class), false);
        } else if (primitive.equals(Type.DOUBLE_TYPE)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, IN_Double, "valueOf", methodDescriptor(Double.class, double.class), false);
        } else {
            return null;
        }
    }

    /**
     * Split an injection declaration into owner, name and desc
     *
     * @param injectDeclaration The injection declaration
     * @return The owner, name and desc
     */
    public static MemberDeclaration splitMemberDeclaration(final String injectDeclaration) {
        Matcher matcher = Pattern.compile("^L([^;]+);([^(:]+):?(\\(?[^\\n]+)$").matcher(injectDeclaration);
        if (matcher.find()) return new MemberDeclaration(matcher.group(1), matcher.group(2), matcher.group(3));
        return null;
    }

    /**
     * Get the first instruction of a constructor
     *
     * @param superClass The name of the super class
     * @param methodNode The {@link MethodNode} of the constructor
     * @return The first instruction of the constructor
     */
    public static AbstractInsnNode getFirstConstructorInstruction(final String superClass, final MethodNode methodNode) {
        AbstractInsnNode first = methodNode.instructions.getFirst();
        while (first != null) {
            if (first.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) first).owner.equals(superClass)) return first.getNext();
            first = first.getNext();
        }
        return null;
    }

    /**
     * Get a {@link Number} from an {@link AbstractInsnNode} if it represents a {@link Number}
     *
     * @param instruction The {@link AbstractInsnNode}
     * @return The {@link Number} or null if it is not a {@link Number}
     */
    public static Number getNumber(final AbstractInsnNode instruction) {
        if (instruction == null) return null;
        if (instruction.getOpcode() >= Opcodes.ICONST_M1 && instruction.getOpcode() <= Opcodes.ICONST_5) return instruction.getOpcode() - Opcodes.ICONST_0;
        else if (instruction.getOpcode() >= Opcodes.LCONST_0 && instruction.getOpcode() <= Opcodes.LCONST_1) return (long) (instruction.getOpcode() - Opcodes.LCONST_0);
        else if (instruction.getOpcode() >= Opcodes.FCONST_0 && instruction.getOpcode() <= Opcodes.FCONST_2) return (float) (instruction.getOpcode() - Opcodes.FCONST_0);
        else if (instruction.getOpcode() >= Opcodes.DCONST_0 && instruction.getOpcode() <= Opcodes.DCONST_1) return (double) (instruction.getOpcode() - Opcodes.DCONST_0);
        else if (instruction.getOpcode() == Opcodes.BIPUSH) return (byte) ((IntInsnNode) instruction).operand;
        else if (instruction.getOpcode() == Opcodes.SIPUSH) return (short) ((IntInsnNode) instruction).operand;
        else if (instruction.getOpcode() == Opcodes.LDC && ((LdcInsnNode) instruction).cst instanceof Number) return (Number) ((LdcInsnNode) instruction).cst;
        return null;
    }

    /**
     * Compare an array of {@link Type} with a target array of {@link Type}<br>
     * This supports {@link Object} instead of direct types
     *
     * @param types       The source array of {@link Type}
     * @param targetTypes The target array of {@link Type}
     * @return If the arrays match
     */
    public static boolean compareTypes(Type[] types, final Type[] targetTypes) {
        return compareTypes(types, targetTypes, false);
    }

    /**
     * Compare an array of {@link Type} with a target array of {@link Type}<br>
     * This supports {@link Object} instead of direct types
     *
     * @param types                 The source array of {@link Type}
     * @param targetTypes           The target array of {@link Type}
     * @param prepend               If the additional {@link Type} array should be prepended or appended
     * @param additionalNeededTypes The additional {@link Type} to append to the source
     * @return If the arrays match
     */
    public static boolean compareTypes(Type[] types, final Type[] targetTypes, final boolean prepend, final Type... additionalNeededTypes) {
        if (additionalNeededTypes.length > 0) {
            Type[] mergedTypes = new Type[types.length + additionalNeededTypes.length];
            if (prepend) {
                System.arraycopy(additionalNeededTypes, 0, mergedTypes, 0, additionalNeededTypes.length);
                System.arraycopy(types, 0, mergedTypes, additionalNeededTypes.length, types.length);
            } else {
                System.arraycopy(types, 0, mergedTypes, 0, types.length);
                System.arraycopy(additionalNeededTypes, 0, mergedTypes, types.length, additionalNeededTypes.length);
            }
            types = mergedTypes;
        }
        if (types.length != targetTypes.length) return false;
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            Type targetType = targetTypes[i];
            boolean areEqual = type.equals(targetType);
            if (type.getSort() != Type.OBJECT && !areEqual) return false;
            if (!areEqual && !targetType.equals(Type.getType(Object.class))) return false;
        }
        return true;
    }

    /**
     * Compare a single {@link Type} with a target {@link Type}<br>
     * This supports {@link Object} instead of direct types
     *
     * @param type       The source {@link Type}
     * @param targetType The target {@link Type}
     * @return If the types match
     */
    public static boolean compareType(final Type type, final Type targetType) {
        if (type.equals(targetType)) return true;
        return type.getSort() == Type.OBJECT && targetType.equals(Type.getType(Object.class));
    }

    /**
     * Clone a {@link ClassNode}
     *
     * @param classNode The {@link ClassNode} to clone
     * @return The cloned {@link ClassNode}
     */
    public static ClassNode cloneClass(final ClassNode classNode) {
        ClassNode clonedClass = new ClassNode();
        classNode.accept(clonedClass);
        return clonedClass;
    }

    /**
     * Clone a {@link MethodNode}
     *
     * @param methodNode The {@link MethodNode} to clone
     * @return The cloned {@link MethodNode}
     */
    public static MethodNode cloneMethod(final MethodNode methodNode) {
        MethodNode clonedMethod = new MethodNode(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(new String[0]));
        methodNode.accept(clonedMethod);
        return clonedMethod;
    }

    /**
     * Create an empty class with a default constructor
     *
     * @param name The name of the class
     * @return The {@link ClassNode}
     */
    public static ClassNode createEmptyClass(final String name) {
        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, slash(name), null, IN_Object, null);

        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, MN_Init, MD_Void, null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, IN_Object, MN_Init, MD_Void));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(constructor);

        return node;
    }

    /**
     * Replace all slashes with dots in the given class/package name
     *
     * @param s The class/package name
     * @return The class/package name with dots
     */
    public static String dot(final String s) {
        return s.replace('/', '.');
    }

    /**
     * Replace all dots with slashes in the given class/package name
     *
     * @param s The class/package name
     * @return The class/package name with slashes
     */
    public static String slash(final String s) {
        return s.replace('.', '/');
    }

}
