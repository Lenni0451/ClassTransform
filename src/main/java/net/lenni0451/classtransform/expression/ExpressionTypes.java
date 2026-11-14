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
 */
@ParametersAreNonnullByDefault
public class ExpressionTypes {

    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final Type NULL_TYPE = Type.getObjectType("null");
    public static final Type INT_LIKE_TYPE = Type.getObjectType("int-like");

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

    public static Type getNaryType(final AbstractInsnNode insn) {
        if (insn.getOpcode() == Opcodes.MULTIANEWARRAY) return Type.getType(((MultiANewArrayInsnNode) insn).desc);
        if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) return Type.getReturnType(((InvokeDynamicInsnNode) insn).desc);
        return Type.getReturnType(((MethodInsnNode) insn).desc);
    }

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

    public static Type getInnerType(final Type arrayType) {
        if (arrayType.equals(NULL_TYPE)) return NULL_TYPE;
        if (arrayType.getSort() != Type.ARRAY) return OBJECT_TYPE;
        return Type.getType(arrayType.getDescriptor().substring(1));
    }

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

    public static boolean isPrimitiveLike(@Nullable final Type type) {
        return type != null && (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE || isIntLike(type));
    }

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
