package net.lenni0451.classtransform.transformer.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.returnType;
import static net.lenni0451.classtransform.utils.Types.type;

/**
 * The description of an operation and its current position in the wrapper chain.
 */
class OperationSite {

    private final AbstractInsnNode original;
    private final Type[] originalArgs;
    private final Type returnType;
    private final boolean newOperation;
    @Nullable
    private final String newOwner;
    private int bridgeCounter;

    private OperationSite(final AbstractInsnNode original, final Type[] originalArgs, final Type returnType, final boolean newOperation, @Nullable final String newOwner) {
        this.original = original;
        this.originalArgs = originalArgs;
        this.returnType = returnType;
        this.newOperation = newOperation;
        this.newOwner = newOwner;
    }

    AbstractInsnNode getOriginal() {
        return this.original;
    }

    Type[] getOriginalArgs() {
        return this.originalArgs;
    }

    Type getReturnType() {
        return this.returnType;
    }

    boolean isNewOperation() {
        return this.newOperation;
    }

    @Nullable
    String getNewOwner() {
        return this.newOwner;
    }

    int nextBridgeIndex() {
        return this.bridgeCounter++;
    }

    static OperationSite of(final ClassNode targetClass, final AbstractInsnNode original, final boolean newOperation) {
        OperationInfo info = describe(targetClass, original);
        if (!newOperation && info.newOperation) {
            throw new IllegalArgumentException("Constructor call operations must be targeted with NEW");
        }
        return new OperationSite(original, info.arguments, info.returnType, newOperation, info.newOperation ? info.newOwner : null);
    }

    static OperationInfo describe(final ClassNode targetClass, final AbstractInsnNode instruction) {
        if (instruction instanceof MethodInsnNode) {
            MethodInsnNode method = (MethodInsnNode) instruction;
            if (method.name.equals("<init>")) {
                return new OperationInfo(argumentTypes(method.desc), type(method.owner), true, method.owner);
            }
            Type[] arguments = argumentTypes(method.desc);
            if (method.getOpcode() != Opcodes.INVOKESTATIC) {
                Type receiver = method.getOpcode() == Opcodes.INVOKESPECIAL ? type(targetClass.name) : type(method.owner);
                Type[] withReceiver = new Type[arguments.length + 1];
                withReceiver[0] = receiver;
                System.arraycopy(arguments, 0, withReceiver, 1, arguments.length);
                arguments = withReceiver;
            }
            return new OperationInfo(arguments, returnType(method.desc), false, null);
        } else if (instruction instanceof FieldInsnNode) {
            FieldInsnNode field = (FieldInsnNode) instruction;
            Type owner = type(field.owner);
            Type fieldType = type(field.desc);
            switch (field.getOpcode()) {
                case Opcodes.GETFIELD:
                    return new OperationInfo(new Type[]{owner}, fieldType, false, null);
                case Opcodes.PUTFIELD:
                    return new OperationInfo(new Type[]{owner, fieldType}, Type.VOID_TYPE, false, null);
                case Opcodes.GETSTATIC:
                    return new OperationInfo(new Type[0], fieldType, false, null);
                case Opcodes.PUTSTATIC:
                    return new OperationInfo(new Type[]{fieldType}, Type.VOID_TYPE, false, null);
                default:
                    throw new IllegalArgumentException("Unsupported field operation " + field.getOpcode());
            }
        } else if (instruction.getOpcode() == Opcodes.INSTANCEOF) {
            return new OperationInfo(new Type[]{Type.getType(Object.class)}, Type.BOOLEAN_TYPE, false, null);
        } else if (instruction instanceof TypeInsnNode && instruction.getOpcode() == Opcodes.CHECKCAST) {
            return new OperationInfo(new Type[]{Type.getType(Object.class)}, Type.getObjectType(((TypeInsnNode) instruction).desc), false, null);
        }
        return simpleOperation(instruction.getOpcode());
    }

    private static OperationInfo simpleOperation(final int opcode) {
        switch (opcode) {
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
                return new OperationInfo(new Type[]{Type.INT_TYPE, Type.INT_TYPE}, Type.INT_TYPE, false, null);
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
                return new OperationInfo(new Type[]{Type.LONG_TYPE, Type.LONG_TYPE}, Type.LONG_TYPE, false, null);
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
                return new OperationInfo(new Type[]{Type.FLOAT_TYPE, Type.FLOAT_TYPE}, Type.FLOAT_TYPE, false, null);
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                return new OperationInfo(new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}, Type.DOUBLE_TYPE, false, null);
            case Opcodes.INEG:
                return new OperationInfo(new Type[]{Type.INT_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.LNEG:
                return new OperationInfo(new Type[]{Type.LONG_TYPE}, Type.LONG_TYPE, false, null);
            case Opcodes.FNEG:
                return new OperationInfo(new Type[]{Type.FLOAT_TYPE}, Type.FLOAT_TYPE, false, null);
            case Opcodes.DNEG:
                return new OperationInfo(new Type[]{Type.DOUBLE_TYPE}, Type.DOUBLE_TYPE, false, null);
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                return new OperationInfo(new Type[]{Type.INT_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.I2L:
                return new OperationInfo(new Type[]{Type.INT_TYPE}, Type.LONG_TYPE, false, null);
            case Opcodes.I2F:
                return new OperationInfo(new Type[]{Type.INT_TYPE}, Type.FLOAT_TYPE, false, null);
            case Opcodes.I2D:
                return new OperationInfo(new Type[]{Type.INT_TYPE}, Type.DOUBLE_TYPE, false, null);
            case Opcodes.L2I:
                return new OperationInfo(new Type[]{Type.LONG_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.L2F:
                return new OperationInfo(new Type[]{Type.LONG_TYPE}, Type.FLOAT_TYPE, false, null);
            case Opcodes.L2D:
                return new OperationInfo(new Type[]{Type.LONG_TYPE}, Type.DOUBLE_TYPE, false, null);
            case Opcodes.F2I:
                return new OperationInfo(new Type[]{Type.FLOAT_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.F2L:
                return new OperationInfo(new Type[]{Type.FLOAT_TYPE}, Type.LONG_TYPE, false, null);
            case Opcodes.F2D:
                return new OperationInfo(new Type[]{Type.FLOAT_TYPE}, Type.DOUBLE_TYPE, false, null);
            case Opcodes.D2I:
                return new OperationInfo(new Type[]{Type.DOUBLE_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.D2L:
                return new OperationInfo(new Type[]{Type.DOUBLE_TYPE}, Type.LONG_TYPE, false, null);
            case Opcodes.D2F:
                return new OperationInfo(new Type[]{Type.DOUBLE_TYPE}, Type.FLOAT_TYPE, false, null);
            case Opcodes.LCMP:
                return new OperationInfo(new Type[]{Type.LONG_TYPE, Type.LONG_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                return new OperationInfo(new Type[]{Type.FLOAT_TYPE, Type.FLOAT_TYPE}, Type.INT_TYPE, false, null);
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return new OperationInfo(new Type[]{Type.DOUBLE_TYPE, Type.DOUBLE_TYPE}, Type.INT_TYPE, false, null);
            default:
                throw new IllegalArgumentException("Unsupported operation with opcode " + opcode);
        }
    }


    static class OperationInfo {
        final Type[] arguments;
        final Type returnType;
        final boolean newOperation;
        @Nullable
        final String newOwner;

        private OperationInfo(final Type[] arguments, final Type returnType, final boolean newOperation, @Nullable final String newOwner) {
            this.arguments = arguments;
            this.returnType = returnType;
            this.newOperation = newOperation;
            this.newOwner = newOwner;
        }
    }

}
