package net.lenni0451.classtransform.utils;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A wrapper for the {@link Type} class.<br>
 * This contains some constants and convenience methods.
 */
@ParametersAreNonnullByDefault
public class Types {

    //types
    /**
     * The {@link Object} type.
     */
    public static final Type T_Object = Type.getType(Object.class);
    //internal names
    /**
     * The internal name of {@link Object}.
     */
    public static final String IN_Object = internalName(Object.class);
    /**
     * The internal name of {@link String}.
     */
    public static final String IN_String = internalName(String.class);
    /**
     * The internal name of {@link Void}.
     */
    public static final String IN_Void = internalName(Void.class);
    /**
     * The internal name of {@link Boolean}.
     */
    public static final String IN_Boolean = internalName(Boolean.class);
    /**
     * The internal name of {@link Byte}.
     */
    public static final String IN_Byte = internalName(Byte.class);
    /**
     * The internal name of {@link Short}.
     */
    public static final String IN_Short = internalName(Short.class);
    /**
     * The internal name of {@link Character}.
     */
    public static final String IN_Character = internalName(Character.class);
    /**
     * The internal name of {@link Integer}.
     */
    public static final String IN_Integer = internalName(Integer.class);
    /**
     * The internal name of {@link Long}.
     */
    public static final String IN_Long = internalName(Long.class);
    /**
     * The internal name of {@link Float}.
     */
    public static final String IN_Float = internalName(Float.class);
    /**
     * The internal name of {@link Double}.
     */
    public static final String IN_Double = internalName(Double.class);
    //method descriptor
    /**
     * The method descriptor of a method returning {@code void} and taking no arguments.
     */
    public static final String MD_Void = methodDescriptor(void.class);
    //method name
    /**
     * The name of an instance initializer (constructor).
     */
    public static final String MN_Init = "<init>";
    /**
     * The name of the static initializer.
     */
    public static final String MN_Clinit = "<clinit>";

    /**
     * Check if a type is a primitive type.<br>
     * Primitive Types:<br>
     * - void ({@link Type#VOID_TYPE})<br>
     * - boolean ({@link Type#BOOLEAN_TYPE})<br>
     * - byte ({@link Type#BYTE_TYPE})<br>
     * - short ({@link Type#SHORT_TYPE})<br>
     * - char ({@link Type#CHAR_TYPE})<br>
     * - int ({@link Type#INT_TYPE})<br>
     * - long ({@link Type#LONG_TYPE})<br>
     * - float ({@link Type#FLOAT_TYPE})<br>
     * - double ({@link Type#DOUBLE_TYPE})
     *
     * @param type The type to check
     * @return True if the type is a primitive type
     */
    public static boolean isPrimitive(final Type type) {
        if (type.equals(Type.VOID_TYPE)) return true;
        else if (type.equals(Type.BOOLEAN_TYPE)) return true;
        else if (type.equals(Type.BYTE_TYPE)) return true;
        else if (type.equals(Type.SHORT_TYPE)) return true;
        else if (type.equals(Type.CHAR_TYPE)) return true;
        else if (type.equals(Type.INT_TYPE)) return true;
        else if (type.equals(Type.LONG_TYPE)) return true;
        else if (type.equals(Type.FLOAT_TYPE)) return true;
        else if (type.equals(Type.DOUBLE_TYPE)) return true;
        return false;
    }

    /**
     * Get a type from an object.<br>
     * Supported types:
     * <ul>
     * <li>{@link String} (parsed as a descriptor first, then as an internal name)</li>
     * <li>{@link Class} ({@link Type#getType(Class)}) or {@link Field} (type of the field)</li>
     * <li>{@link Method} ({@link Type#getType(Method)})</li>
     * <li>{@link Constructor} ({@link Type#getType(Constructor)})</li>
     * <li>{@link Type} (the type itself)</li>
     * </ul>
     *
     * @param ob The object to get the type from
     * @return The type
     * @throws IllegalArgumentException If the object is not supported
     */
    public static Type type(final Object ob) {
        if (ob instanceof String) {
            String s = (String) ob;
            try {
                return Type.getType(s);
            } catch (Throwable t) {
                return Type.getObjectType(s);
            }
        } else if (ob instanceof Class) {
            return Type.getType((Class<?>) ob);
        } else if (ob instanceof Field) {
            return Type.getType(((Field) ob).getType());
        } else if (ob instanceof Method) {
            return Type.getType((Method) ob);
        } else if (ob instanceof Constructor) {
            return Type.getType((Constructor<?>) ob);
        } else if (ob instanceof Type) {
            return (Type) ob;
        }
        throw new IllegalArgumentException("Unable to convert " + asString(ob) + " into a type");
    }

    /**
     * Get a return type from an object.<br>
     * Supported types:<br>
     * - {@link String} ({@link Type#getReturnType(String)})<br>
     * - {@link Method} ({@link Type#getReturnType(Method)})<br>
     * - {@link MethodNode} ({@link Type#getReturnType(String)})<br>
     * - {@link MethodInsnNode} ({@link Type#getReturnType(String)})<br>
     * - {@link Type} ({@code return type})
     *
     * @param ob The object to get the return type from
     * @return The return type
     * @throws IllegalArgumentException If the object is not supported
     */
    public static Type returnType(final Object ob) {
        if (ob instanceof String) return Type.getReturnType((String) ob);
        else if (ob instanceof Method) return Type.getReturnType((Method) ob);
        else if (ob instanceof MethodNode) return Type.getReturnType(((MethodNode) ob).desc);
        else if (ob instanceof MethodInsnNode) return Type.getReturnType(((MethodInsnNode) ob).desc);
        else if (ob instanceof Type) return ((Type) ob).getReturnType();
        throw new IllegalArgumentException("Unable to get return type of " + asString(ob));
    }

    /**
     * Get argument types from an object.<br>
     * Supported types:<br>
     * - {@link String} ({@link Type#getArgumentTypes(String)})<br>
     * - {@link Method} ({@link Type#getArgumentTypes(Method)})
     * - {@link MethodNode} ({@link Type#getArgumentTypes(String)})<br>
     * - {@link MethodInsnNode} ({@link Type#getArgumentTypes(String)})<br>
     *
     * @param ob The object to get the argument types from
     * @return The argument types
     * @throws IllegalArgumentException If the object is not supported
     */
    public static Type[] argumentTypes(final Object ob) {
        if (ob instanceof String) return Type.getArgumentTypes((String) ob);
        else if (ob instanceof Method) return Type.getArgumentTypes((Method) ob);
        else if (ob instanceof MethodNode) return Type.getArgumentTypes(((MethodNode) ob).desc);
        else if (ob instanceof MethodInsnNode) return Type.getArgumentTypes(((MethodInsnNode) ob).desc);
        throw new IllegalArgumentException("Unable to get argument types of " + ob);
    }

    /**
     * Get the internal name of an object.<br>
     * See {@link #type(Object)} for supported types.
     *
     * @param ob The object to get the internal name from
     * @return The internal name
     * @throws IllegalArgumentException If the object is not supported
     */
    public static String internalName(final Object ob) {
        try {
            return type(ob).getInternalName();
        } catch (Throwable t) {
            throw new IllegalArgumentException("Unable to get internal name of " + asString(ob), t);
        }
    }

    /**
     * Get the descriptor of an object.<br>
     * See {@link #type(Object)} for supported types.
     *
     * @param ob The object to get the descriptor from
     * @return The descriptor
     * @throws IllegalArgumentException If the object is not supported
     */
    public static String typeDescriptor(final Object ob) {
        try {
            return type(ob).getDescriptor();
        } catch (Throwable t) {
            throw new IllegalArgumentException("Unable to convert " + asString(ob) + " into a descriptor", t);
        }
    }

    /**
     * Get the method descriptor of a return type and argument types.<br>
     * See {@link #type(Object)} for supported types.<br>
     * If the {@code returnType} is a {@link Method} or {@link Constructor} no {@code arguments} are required.
     *
     * @param returnType The return type
     * @param arguments  The argument types
     * @return The method descriptor
     * @throws IllegalArgumentException If an object is not supported or arguments a passed for a {@link Method} or {@link Constructor}
     */
    public static String methodDescriptor(final Object returnType, final Object... arguments) {
        if (returnType instanceof Method) {
            if (arguments.length != 0) throw new IllegalArgumentException("Expected arguments to be empty");
            return Type.getMethodDescriptor((Method) returnType);
        } else if (returnType instanceof Constructor) {
            if (arguments.length != 0) throw new IllegalArgumentException("Expected arguments to be empty");
            return Type.getConstructorDescriptor((Constructor<?>) returnType);
        }

        StringBuilder out = new StringBuilder("(");
        for (Object argument : arguments) out.append(typeDescriptor(argument));
        out.append(")").append(typeDescriptor(returnType));
        return out.toString();
    }

    private static String asString(@Nullable final Object ob) {
        if (ob == null) return "null";
        return ob.toString();
    }

}
