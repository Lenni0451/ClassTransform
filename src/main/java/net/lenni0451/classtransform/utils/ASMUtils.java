package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.lenni0451.classtransform.utils.tree.TreeClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * General utils for using ASM.
 */
@ParametersAreNonnullByDefault
public class ASMUtils {

    public static final String METHOD_DECLARATION_PATTERN = "^(?>L([^;]+);|([^.]+)\\.)([^(]+)(\\([^)]*\\).+)$";
    public static final String FIELD_DECLARATION_PATTERN = "^(?>L([^;]+);|([^.]+)\\.)([^(]+):(.+)$";

    /**
     * Get a class node from the raw bytecode of a class.
     *
     * @param bytecode The bytecode of the class
     * @return The parsed class node
     */
    public static ClassNode fromBytes(final byte[] bytecode) {
        return fromBytes(bytecode, ClassReader.EXPAND_FRAMES);
    }

    /**
     * Get a class node from the raw bytecode of a class.
     *
     * @param bytecode The bytecode of the class
     * @param flags    The flags to use for the class reader
     * @return The parsed class node
     */
    public static ClassNode fromBytes(final byte[] bytecode, final int flags) {
        ClassNode node = new ClassNode();
        new ClassReader(bytecode).accept(node, flags);
        return node;
    }

    /**
     * Get the bytecode from a class node.
     *
     * @param node          The class node
     * @param classTree     The class tree used to get the super classes
     * @param classProvider The class provider used for stack frame calculation
     * @return The bytecode of the class
     */
    public static byte[] toBytes(final ClassNode node, final ClassTree classTree, final IClassProvider classProvider) {
        TreeClassWriter writer = new TreeClassWriter(classTree, classProvider);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Get the bytecode from a class node.
     *
     * @param node          The class node
     * @param classTree     The class tree used to get the super classes
     * @param classProvider The class provider used for stack frame calculation
     * @param flags         The flags to use for the class writer
     * @return The bytecode of the class
     */
    public static byte[] toBytes(final ClassNode node, final ClassTree classTree, final IClassProvider classProvider, final int flags) {
        TreeClassWriter writer = new TreeClassWriter(flags, classTree, classProvider);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Get the bytecode from a class node without calculating stack map frames.
     *
     * @param node The class node
     * @return The bytecode of the class
     */
    public static byte[] toStacklessBytes(final ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Get a method node from a class node using the name and descriptor.
     *
     * @param classNode The class node to search in
     * @param name      The name of the method
     * @param desc      The descriptor of the method
     * @return The method node or null if no method was found
     */
    @Nullable
    public static MethodNode getMethod(final ClassNode classNode, final String name, final String desc) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    /**
     * Get a field node from a class node using the name.<br>
     * The descriptor is passed as a parameter but is not used for the search.
     *
     * @param classNode The class node to search in
     * @param name      The name of the field
     * @param desc      The descriptor of the field
     * @return The field node or null if no field was found
     */
    @Nullable
    public static FieldNode getField(final ClassNode classNode, final String name, final String desc) {
        for (FieldNode field : classNode.fields) {
            if (field.name.equals(name)) return field;
        }
        return null;
    }

    /**
     * Get a list of method nodes from a class node using the combined name and descriptor.<br>
     * Use <b>*</b> for a wildcard search.<br>
     * e.g. <b>print(Ljava/lang/String;)V</b>
     *
     * @param classNode The class node to search in
     * @param combi     The combined name and descriptor
     * @return The list of method nodes
     * @throws IllegalArgumentException If the combined name and descriptor is empty
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
     * Get a list of method nodes from a class node using the combined name and descriptor.<br>
     * Use <b>*</b> for a wildcard search.<br>
     * e.g. <b>print(Ljava/lang/String;)V</b>
     *
     * @param classNode The class node to search in
     * @param combi     The combined name and descriptor
     * @return The list of method nodes
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
     * Convert a field or method search pattern to a regex pattern.<br>
     * Use <b>*</b> for a wildcard search.<br>
     * e.g. <b>get*</b> -&gt; <b>^\Qget\E.*$</b>
     *
     * @param combi The search pattern
     * @return The converted regex pattern
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
     * Check if the given access is lower than another.<br>
     * private {@literal <} package private {@literal <} protected {@literal <} public
     *
     * @param toCheck      The access to check
     * @param checkAgainst The access to check against
     * @return If the access is lower than the other
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
     * Set the wanted access to a given access mask.<br>
     * Other access flags will be removed.
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
     * Get the needed return opcode for the given type.
     *
     * @param returnType The return type of a method
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
     * Get the needed load opcode for the given type.
     *
     * @param type The type to get the load opcode for
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
     * Get the needed store opcode for the given type.
     *
     * @param type The type to get the store opcode for
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
     * Get the last empty local variable index.<br>
     * You can simply count the index up from there.
     *
     * @param methodNode The method to get the last empty local variable index for
     * @return The last empty local variable index
     */
    public static int getFreeVarIndex(final MethodNode methodNode) {
        int currentIndex = 0;
        if (!Modifier.isStatic(methodNode.access)) currentIndex = 1;
        for (Type arg : Type.getArgumentTypes(methodNode.desc)) currentIndex += arg.getSize();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if ((instruction.getOpcode() >= Opcodes.ISTORE && instruction.getOpcode() <= Opcodes.ASTORE) || (instruction.getOpcode() >= Opcodes.ILOAD && instruction.getOpcode() <= Opcodes.ALOAD)) {
                VarInsnNode varInsnNode = (VarInsnNode) instruction;
                if (varInsnNode.var > currentIndex) currentIndex = varInsnNode.var;
            } else if (instruction.getOpcode() == Opcodes.IINC) {
                IincInsnNode iincInsnNode = (IincInsnNode) instruction;
                if (iincInsnNode.var > currentIndex) currentIndex = iincInsnNode.var;
            }
        }
        return currentIndex + 2; //Add 2 just to be sure not to overwrite anything
    }

    /**
     * Get the byte code to cast an object to a given type.<br>
     * Converts primitive wrapper to their primitive types (e.g. Integer to int).
     *
     * @param wantedType The wanted type
     * @return The byte code for the cast
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
     * Get the byte code to wrap a primitive to its wrapper type (e.g. int to Integer).
     *
     * @param primitive The primitive type to wrap
     * @return The byte code for the wrapper or null if the given type is not a primitive
     */
    @Nullable
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
     * Split a member declaration into owner, name and descriptor.<br>
     * All parts are required.<br>
     * Examples:<br>
     * - {@code java/lang/String.length()I}<br>
     * - {@code java/lang/System.out:Ljava/io/PrintStream;}<br>
     * - {@code Ljava/lang/String;length()I}<br>
     * - {@code Ljava/lang/System;out:Ljava/io/PrintStream;}
     *
     * @param memberDeclaration The member declaration
     * @return The split member declaration
     */
    @Nullable
    public static MemberDeclaration splitMemberDeclaration(final String memberDeclaration) {
        if (memberDeclaration.matches(METHOD_DECLARATION_PATTERN)) {
            Matcher matcher = Pattern.compile(METHOD_DECLARATION_PATTERN).matcher(memberDeclaration);
            if (matcher.find()) return new MemberDeclaration(matcher.group(1) == null ? matcher.group(2) : matcher.group(1), matcher.group(3), matcher.group(4));
        } else if (memberDeclaration.matches(FIELD_DECLARATION_PATTERN)) {
            Matcher matcher = Pattern.compile(FIELD_DECLARATION_PATTERN).matcher(memberDeclaration);
            if (matcher.find()) return new MemberDeclaration(matcher.group(1) == null ? matcher.group(2) : matcher.group(1), matcher.group(3), matcher.group(4));
        }
        return null;
    }

    /**
     * Get the first instruction of a constructor skipping the super init call.
     *
     * @param superClass The name of the super class
     * @param methodNode The constructor method node
     * @return The first actual instruction of the constructor
     */
    @Nullable
    public static AbstractInsnNode getFirstConstructorInstruction(final String superClass, final MethodNode methodNode) {
        AbstractInsnNode first = methodNode.instructions.getFirst();
        while (first != null) {
            if (first.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) first).owner.equals(superClass)) return first.getNext();
            first = first.getNext();
        }
        return null;
    }

    /**
     * Get a number from an insn node if it represents one.<br>
     * This also works for constants (e.g. {@link Opcodes#ICONST_0})
     *
     * @param instruction The insn node
     * @return The number or null if it's not a number
     */
    @Nullable
    public static Number getNumber(@Nullable final AbstractInsnNode instruction) {
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
     * Compare an array of types with a target array of types.<br>
     * This supports the usage of object instead of the actual type but does not check for other inheritance.<br>
     * Object can not be used as a replacement for primitives.
     *
     * @param types       The source array of types
     * @param targetTypes The target array of types
     * @return If the arrays match
     */
    public static boolean compareTypes(Type[] types, final Type[] targetTypes) {
        return compareTypes(types, targetTypes, false);
    }

    /**
     * Compare an array of types with a target array of types.<br>
     * This supports the usage of object instead of the actual type but does not check for other inheritance.<br>
     * Object can not be used as a replacement for primitives.
     *
     * @param types                 The source array of types
     * @param targetTypes           The target array of types
     * @param prepend               If the additional types array should be prepended or appended
     * @param additionalNeededTypes The additional types to append/prepend to the source
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
     * Compare a single type with a target type<br>
     * This supports the usage of object instead of the actual type but does not check for other inheritance.<br>
     * Object can not be used as a replacement for primitives.
     *
     * @param type       The source type
     * @param targetType The target type
     * @return If the types match
     */
    public static boolean compareType(final Type type, final Type targetType) {
        if (type.equals(targetType)) return true;
        return type.getSort() == Type.OBJECT && targetType.equals(Type.getType(Object.class));
    }

    /**
     * Clone a class node with all its methods and fields.
     *
     * @param classNode The class node to clone
     * @return The cloned class node
     */
    public static ClassNode cloneClass(final ClassNode classNode) {
        ClassNode clonedClass = new ClassNode();
        classNode.accept(clonedClass);
        return clonedClass;
    }

    /**
     * Clone a method node with all its instructions.
     *
     * @param methodNode The method node to clone
     * @return The cloned method node
     */
    public static MethodNode cloneMethod(final MethodNode methodNode) {
        MethodNode clonedMethod = new MethodNode(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions == null ? null : methodNode.exceptions.toArray(new String[0]));
        methodNode.accept(clonedMethod);
        return clonedMethod;
    }

    /**
     * Clone an insn list with all its instructions.
     *
     * @param insnList The insn list to clone
     * @return The cloned insn list
     */
    public static InsnList cloneInsnList(final InsnList insnList) {
        InsnList clonedInsnList = new InsnList();
        Map<LabelNode, LabelNode> clonedLabels = new HashMap<>();
        for (AbstractInsnNode insn : insnList) {
            if (insn instanceof LabelNode) clonedLabels.put((LabelNode) insn, new LabelNode());
        }
        for (AbstractInsnNode instruction : insnList.toArray()) clonedInsnList.add(instruction.clone(clonedLabels));
        return clonedInsnList;
    }

    /**
     * Create an empty class with a default constructor.
     *
     * @param name The name of the class
     * @return The generated class node
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
     * Create a new insn node representing the given int.<br>
     * This uses the most efficient opcode for the given int.
     *
     * @param i The int
     * @return The insn node
     */
    public static AbstractInsnNode intPush(final int i) {
        if (i >= -1 && i <= 5) return new InsnNode(Opcodes.ICONST_0 + i);
        if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) return new IntInsnNode(Opcodes.BIPUSH, i);
        if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) return new IntInsnNode(Opcodes.SIPUSH, i);
        return new LdcInsnNode(i);
    }

    /**
     * Get the variable indices for the parameters of a method.
     *
     * @param methodNode The method node
     * @return The variable indices
     */
    public static int[] getParameterIndices(final MethodNode methodNode) {
        return getParameterIndices(argumentTypes(methodNode), Modifier.isStatic(methodNode.access));
    }

    /**
     * Get the variable indices for an array of types.
     *
     * @param types    The types
     * @param isStatic If the first index should be 0 or 1
     * @return The variable indices
     */
    public static int[] getParameterIndices(final Type[] types, final boolean isStatic) {
        int[] indices = new int[types.length];
        int current = isStatic ? 0 : 1;
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            indices[i] = current;
            current += type.getSize();
        }
        return indices;
    }

    /**
     * Replace all slashes with dots in the given class/package name.
     *
     * @param s The class/package name
     * @return The class/package name with dots
     */
    public static String dot(final String s) {
        return s.replace('/', '.');
    }

    /**
     * Replace all dots with slashes in the given class/package name.
     *
     * @param s The class/package name
     * @return The class/package name with slashes
     */
    public static String slash(final String s) {
        return s.replace('.', '/');
    }

}
