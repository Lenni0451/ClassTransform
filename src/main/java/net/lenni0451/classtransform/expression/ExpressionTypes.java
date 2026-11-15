package net.lenni0451.classtransform.expression;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * Type helpers for the expression engine.
 * Computes the type produced by an instruction and defines the pseudo types used by the value flow analysis.
 */
@ParametersAreNonnullByDefault
public class ExpressionTypes {

    /**
     * The type of {@code java.lang.Object}, used whenever no more precise type is known.
     */
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    /**
     * A pseudo type standing for the {@code null} constant and for values whose type could not be resolved.
     * It is deliberately distinct from {@link #OBJECT_TYPE}; merging with a concrete type usually yields the
     * concrete type.
     */
    public static final Type NULL_TYPE = Type.getObjectType("null");
    /**
     * A pseudo type shared by {@code boolean}, {@code byte}, {@code char}, {@code short} and {@code int}.
     * Constant instructions report this type because the JVM does not distinguish the int-like types on the
     * operand stack; merging two int-like types keeps it.
     */
    public static final Type INT_LIKE_TYPE = Type.getObjectType("int-like");

    /**
     * Computes the type produced by an instruction without operands.
     * <p>
     * Handles constant pushes, {@code LDC} constants, {@code GETSTATIC} and {@code NEW}. For {@code LDC} of a
     * class constant the result is {@code Class}, for a method type {@code MethodType} and for a method handle
     * {@code MethodHandle}.
     *
     * @param insn the instruction
     * @return the type produced by the instruction
     * @throws IllegalArgumentException if the opcode is unsupported or the {@code LDC} constant is unknown
     */
    public static Type getNewType(final AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ACONST_NULL:
                return NULL_TYPE;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                return INT_LIKE_TYPE;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                return Type.LONG_TYPE;
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                return Type.FLOAT_TYPE;
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                return Type.DOUBLE_TYPE;
            case Opcodes.LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) return INT_LIKE_TYPE;
                if (cst instanceof Float) return Type.FLOAT_TYPE;
                if (cst instanceof Long) return Type.LONG_TYPE;
                if (cst instanceof Double) return Type.DOUBLE_TYPE;
                if (cst instanceof String) return Type.getType(String.class);
                if (cst instanceof Type) {
                    int sort = ((Type) cst).getSort();
                    if (sort == Type.OBJECT || sort == Type.ARRAY) return Type.getType(Class.class);
                    if (sort == Type.METHOD) return Type.getType(MethodType.class);
                }
                if (cst instanceof Handle) return Type.getType(MethodHandle.class);
                if (cst instanceof ConstantDynamic) return OBJECT_TYPE;
                throw new IllegalArgumentException("Unsupported LDC constant " + cst);
            case Opcodes.GETSTATIC:
                return Type.getType(((FieldInsnNode) insn).desc);
            case Opcodes.NEW:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            default:
                throw new IllegalArgumentException("Could not compute type of opcode " + insn.getOpcode());
        }
    }

    /**
     * Computes the type produced by an instruction with one operand.
     * <p>
     * Covers negations, primitive conversions, {@code ARRAYLENGTH}, {@code IINC}, conditional jumps and
     * returns (which produce {@code void}), {@code GETFIELD}, array creation, {@code CHECKCAST} and
     * {@code INSTANCEOF}.
     *
     * @param insn the instruction
     * @return the type produced by the instruction
     * @throws IllegalArgumentException if the opcode is unsupported
     */
    public static Type getUnaryType(final AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.INEG:
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.IINC:
                return Type.INT_TYPE;
            case Opcodes.I2B:
                return Type.BYTE_TYPE;
            case Opcodes.I2C:
                return Type.CHAR_TYPE;
            case Opcodes.I2S:
                return Type.SHORT_TYPE;
            case Opcodes.FNEG:
            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
                return Type.FLOAT_TYPE;
            case Opcodes.LNEG:
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return Type.LONG_TYPE;
            case Opcodes.DNEG:
            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D:
                return Type.DOUBLE_TYPE;
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.PUTSTATIC:
            case Opcodes.ATHROW:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                return Type.VOID_TYPE;
            case Opcodes.GETFIELD:
                return Type.getType(((FieldInsnNode) insn).desc);
            case Opcodes.NEWARRAY:
                switch (((IntInsnNode) insn).operand) {
                    case Opcodes.T_BOOLEAN: return Type.getType("[Z");
                    case Opcodes.T_CHAR: return Type.getType("[C");
                    case Opcodes.T_BYTE: return Type.getType("[B");
                    case Opcodes.T_SHORT: return Type.getType("[S");
                    case Opcodes.T_INT: return Type.getType("[I");
                    case Opcodes.T_FLOAT: return Type.getType("[F");
                    case Opcodes.T_DOUBLE: return Type.getType("[D");
                    case Opcodes.T_LONG: return Type.getType("[J");
                    default: throw new IllegalArgumentException("Invalid array type");
                }
            case Opcodes.ANEWARRAY:
                return Type.getType("[" + Type.getObjectType(((TypeInsnNode) insn).desc));
            case Opcodes.CHECKCAST:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            case Opcodes.INSTANCEOF:
                return Type.BOOLEAN_TYPE;
            default:
                throw new IllegalArgumentException("Could not compute type of opcode " + insn.getOpcode());
        }
    }

    /**
     * Computes the type produced by an instruction with two operands.
     * <p>
     * Arithmetic instructions produce their natural result type and array loads the type of the loaded element;
     * for {@code AALOAD} (reference arrays) and {@code BALOAD} (boolean and byte arrays) that element type is
     * derived from the array type passed as {@code left}. The compare instructions {@code LCMP}, {@code FCMPL},
     * {@code FCMPG}, {@code DCMPL} and {@code DCMPG} produce an {@code int}, while the {@code IF_ICMP*} and
     * {@code IF_ACMP*} comparisons and {@code PUTFIELD} produce {@code void}.
     *
     * @param insn the instruction
     * @param left the type of the left operand, used to resolve the element type of {@code AALOAD} and
     *             {@code BALOAD}
     * @return the type produced by the instruction
     * @throws IllegalArgumentException if the opcode is unsupported
     */
    public static Type getBinaryType(final AbstractInsnNode insn, @Nullable final Type left) {
        switch (insn.getOpcode()) {
            case Opcodes.LALOAD:
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
                return Type.LONG_TYPE;
            case Opcodes.DALOAD:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                return Type.DOUBLE_TYPE;
            case Opcodes.IALOAD:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.LCMP:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return Type.INT_TYPE;
            case Opcodes.FALOAD:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
                return Type.FLOAT_TYPE;
            case Opcodes.CALOAD:
                return Type.CHAR_TYPE;
            case Opcodes.SALOAD:
                return Type.SHORT_TYPE;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.PUTFIELD:
                return Type.VOID_TYPE;
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
                return left == null ? OBJECT_TYPE : getInnerType(left);
            default:
                throw new IllegalArgumentException("Could not compute type of opcode " + insn.getOpcode());
        }
    }

    /**
     * Computes the type produced by an instruction with a variable number of operands.
     * <p>
     * For method and invokedynamic calls this is the return type; for {@code MULTIANEWARRAY} it is the created
     * array type.
     *
     * @param insn the instruction
     * @return the type produced by the instruction
     */
    public static Type getNaryType(final AbstractInsnNode insn) {
        if (insn.getOpcode() == Opcodes.MULTIANEWARRAY) return Type.getType(((MultiANewArrayInsnNode) insn).desc);
        if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) return Type.getReturnType(((InvokeDynamicInsnNode) insn).desc);
        return Type.getReturnType(((MethodInsnNode) insn).desc);
    }

    /**
     * Returns the type a cast instruction converts to.
     *
     * @param insn the instruction
     * @return the cast target type, or null if the instruction is not a cast
     */
    public static Type getCastType(final AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.CHECKCAST:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                return Type.INT_TYPE;
            case Opcodes.I2B:
                return Type.BYTE_TYPE;
            case Opcodes.I2C:
                return Type.CHAR_TYPE;
            case Opcodes.I2S:
                return Type.SHORT_TYPE;
            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
                return Type.FLOAT_TYPE;
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return Type.LONG_TYPE;
            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D:
                return Type.DOUBLE_TYPE;
            default:
                return null;
        }
    }

    /**
     * Returns the element type of an array type.
     * <p>
     * {@link #NULL_TYPE} is preserved and non-array types yield {@link #OBJECT_TYPE}.
     *
     * @param arrayType the array type
     * @return the element type
     */
    public static Type getInnerType(final Type arrayType) {
        if (arrayType.equals(NULL_TYPE)) return NULL_TYPE;
        if (arrayType.getSort() != Type.ARRAY) return OBJECT_TYPE;
        return Type.getType(arrayType.getDescriptor().substring(1));
    }

    /**
     * Tests if a type is one of the int-like types.
     * <p>
     * These are {@code boolean}, {@code byte}, {@code char}, {@code short}, {@code int} and the
     * {@link #INT_LIKE_TYPE} pseudo type.
     *
     * @param type the type to test, may be null
     * @return whether the type is int-like
     */
    public static boolean isIntLike(@Nullable final Type type) {
        if (type == null) return false;
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return true;
            case Type.OBJECT:
                return type.equals(INT_LIKE_TYPE);
            default:
                return false;
        }
    }

    /**
     * Tests if a type is a primitive or an int-like pseudo type.
     *
     * @param type the type to test, may be null
     * @return whether the type is primitive-like
     */
    public static boolean isPrimitiveLike(@Nullable final Type type) {
        return type != null && (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE || isIntLike(type));
    }

    /**
     * Merges two types into the type both values can be represented as.
     * <p>
     * Used for control flow merges in {@link ExpressionFlow}. {@link #NULL_TYPE} acts as the neutral element.
     * Two int-like types merge to {@link #INT_LIKE_TYPE}; two distinct reference types, and arrays with any
     * non-int-like type, merge to {@link #OBJECT_TYPE}; combining an int-like type with any other type yields
     * {@link #NULL_TYPE}, as does combining two primitive sorts unless both are int-like.
     *
     * @param first  the first type
     * @param second the second type
     * @return the merged type, never null
     */
    public static Type getCommonSupertype(final Type first, final Type second) {
        if (first.equals(second) || second.equals(NULL_TYPE)) return first;
        if (first.equals(NULL_TYPE)) return second;
        if (isIntLike(first) && isIntLike(second)) return INT_LIKE_TYPE;
        if (isIntLike(first) || isIntLike(second)) return NULL_TYPE;
        if (first.getSort() == Type.ARRAY && second.getSort() == Type.ARRAY) return OBJECT_TYPE;
        if (first.getSort() == Type.ARRAY || second.getSort() == Type.ARRAY) return OBJECT_TYPE;
        if (first.getSort() != second.getSort()) return NULL_TYPE;
        return OBJECT_TYPE;
    }

    /**
     * Returns the boxed wrapper type of a primitive type.
     * <p>
     * Non-primitive types are returned unchanged.
     *
     * @param type the type to box
     * @return the boxed type or the input type if it is not primitive
     */
    public static Type primitiveToBoxed(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN: return Type.getType(Boolean.class);
            case Type.BYTE: return Type.getType(Byte.class);
            case Type.CHAR: return Type.getType(Character.class);
            case Type.SHORT: return Type.getType(Short.class);
            case Type.INT: return Type.getType(Integer.class);
            case Type.FLOAT: return Type.getType(Float.class);
            case Type.LONG: return Type.getType(Long.class);
            case Type.DOUBLE: return Type.getType(Double.class);
            default: return type;
        }
    }

    /**
     * Returns the name of the method which unboxes a primitive type.
     * <p>
     * For example {@code int} yields {@code intValue}.
     *
     * @param type the primitive type
     * @return the unboxing method name, or null if the type is not a supported primitive
     */
    public static String getUnboxingMethod(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN: return "booleanValue";
            case Type.BYTE: return "byteValue";
            case Type.CHAR: return "charValue";
            case Type.SHORT: return "shortValue";
            case Type.INT: return "intValue";
            case Type.FLOAT: return "floatValue";
            case Type.LONG: return "longValue";
            case Type.DOUBLE: return "doubleValue";
            default: return null;
        }
    }

}
