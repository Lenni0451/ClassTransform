package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * The expression AST used by the expression engine.
 */
@ParametersAreNonnullByDefault
public abstract class ExpressionAst implements Expression {

    protected final ExpressionSource src;

    protected ExpressionAst(final ExpressionSource src) {
        this.src = src;
    }

    public ExpressionSource getSource() {
        return this.src;
    }

    public abstract boolean matches(ExpressionFlow.FlowValue node, ExpressionContext ctx);

    public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
        if (node.isComplex()) return;
        ctx.capture(node, this, node.getType(), null, new Type[0], null, null, false);
    }

    protected boolean inputsMatch(final ExpressionFlow.FlowValue node, final ExpressionContext ctx, final Expression... values) {
        return this.inputsMatch(node, ctx, false, values);
    }

    protected boolean inputsMatch(final ExpressionFlow.FlowValue node, final ExpressionContext ctx, final boolean allowIncomplete, final Expression... values) {
        if (node.isComplex()) return false;
        int required = node.inputCount();
        if (!(allowIncomplete && values.length < required) && values.length != required) return false;
        for (int i = 0; i < values.length; i++) {
            if (!values[i].matches(node.getInput(i), ctx)) return false;
        }
        return true;
    }

    protected static boolean matchesNull(final ExpressionFlow.FlowValue node) {
        return !node.isComplex() && node.getInsn().getOpcode() == Opcodes.ACONST_NULL;
    }

    @Nullable
    protected static Object getConstant(final AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ICONST_M1: return -1;
            case Opcodes.ICONST_0: return 0;
            case Opcodes.ICONST_1: return 1;
            case Opcodes.ICONST_2: return 2;
            case Opcodes.ICONST_3: return 3;
            case Opcodes.ICONST_4: return 4;
            case Opcodes.ICONST_5: return 5;
            case Opcodes.LCONST_0: return 0L;
            case Opcodes.LCONST_1: return 1L;
            case Opcodes.FCONST_0: return 0F;
            case Opcodes.FCONST_1: return 1F;
            case Opcodes.FCONST_2: return 2F;
            case Opcodes.DCONST_0: return 0D;
            case Opcodes.DCONST_1: return 1D;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                return ((IntInsnNode) insn).operand;
            case Opcodes.LDC: return ((LdcInsnNode) insn).cst;
            default: return null;
        }
    }


    public static class CapturingExpression extends ExpressionAst {
        private final Expression inner;

        public CapturingExpression(final ExpressionSource src, final Expression inner) {
            super(src);
            this.inner = inner;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (!this.inner.matches(node, ctx)) return false;
            this.inner.capture(node, ctx);
            return true;
        }
    }

    public static class WildcardExpression extends ExpressionAst {
        public WildcardExpression(final ExpressionSource src) {
            super(src);
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            return !node.isComplex();
        }
    }

    public static class ThisExpression extends ExpressionAst {
        public ThisExpression(final ExpressionSource src) {
            super(src);
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof VarInsnNode)) return false;
            VarInsnNode var = (VarInsnNode) node.getInsn();
            return var.getOpcode() == Opcodes.ALOAD && var.var == 0;
        }
    }

    public static class IntLiteralExpression extends ExpressionAst {
        private final long value;

        public IntLiteralExpression(final ExpressionSource src, final long value) {
            super(src);
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            Object constant = getConstant(node.getInsn());
            return constant instanceof Number && ((Number) constant).longValue() == this.value;
        }

        @Override
        public boolean matchesZero() {
            return this.value == 0;
        }
    }

    public static class DecimalLiteralExpression extends ExpressionAst {
        private final double value;

        public DecimalLiteralExpression(final ExpressionSource src, final double value) {
            super(src);
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            Object constant = getConstant(node.getInsn());
            return constant instanceof Number && ((Number) constant).doubleValue() == this.value;
        }
    }

    public static class StringLiteralExpression extends ExpressionAst {
        private final String value;

        public StringLiteralExpression(final ExpressionSource src, final String value) {
            super(src);
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof LdcInsnNode)) return false;
            return this.value.equals(((LdcInsnNode) node.getInsn()).cst);
        }
    }

    public static class BooleanLiteralExpression extends ExpressionAst {
        private final boolean value;

        public BooleanLiteralExpression(final ExpressionSource src, final boolean value) {
            super(src);
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            Object constant = getConstant(node.getInsn());
            return constant instanceof Number && (((Number) constant).intValue() != 0) == this.value;
        }
    }

    public static class NullLiteralExpression extends ExpressionAst {
        public NullLiteralExpression(final ExpressionSource src) {
            super(src);
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            return matchesNull(node);
        }

        @Override
        public boolean matchesNull() {
            return true;
        }
    }

    public static class ClassConstantExpression extends ExpressionAst {
        private final TypeIdentifier type;

        public ClassConstantExpression(final ExpressionSource src, final TypeIdentifier type) {
            super(src);
            this.type = type;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof LdcInsnNode)) return false;
            Object constant = ((LdcInsnNode) node.getInsn()).cst;
            return constant instanceof Type && this.type.matches(ctx.getIdentifiers(), (Type) constant);
        }
    }

    public static class IdentifierExpression extends ExpressionAst {
        private final String identifier;

        public IdentifierExpression(final ExpressionSource src, final String identifier) {
            super(src);
            this.identifier = identifier;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            return ctx.getIdentifiers().matchesMember(this.identifier, node);
        }

        public String getIdentifier() {
            return this.identifier;
        }
    }

    public static class MemberAccessExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;

        public MemberAccessExpression(final ExpressionSource src, final Expression receiver, final MemberName member) {
            super(src);
            this.receiver = receiver;
            this.member = member;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof FieldInsnNode)) return false;
            FieldInsnNode field = (FieldInsnNode) node.getInsn();
            if (field.getOpcode() != Opcodes.GETFIELD && field.getOpcode() != Opcodes.GETSTATIC) return false;
            if (!this.member.matches(ctx, node)) return false;
            if (field.getOpcode() == Opcodes.GETFIELD) {
                return node.inputCount() == 1 && this.receiver.matches(node.getInput(0), ctx);
            }
            return node.inputCount() == 0;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            FieldInsnNode field = (FieldInsnNode) node.getInsn();
            ctx.capture(node, this, Type.getType(field.desc), field.getOpcode() == Opcodes.GETFIELD ? Type.getObjectType(field.owner) : null, new Type[0], null, null, false);
        }

        public Expression getReceiver() {
            return this.receiver;
        }

        public MemberName getMember() {
            return this.member;
        }
    }

    public static class MethodCallExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;
        private final List<Expression> arguments;

        public MethodCallExpression(final ExpressionSource src, final Expression receiver, final MemberName member, final List<Expression> arguments) {
            super(src);
            this.receiver = receiver;
            this.member = member;
            this.arguments = arguments;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof MethodInsnNode)) return false;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            if (method.name.equals("<init>")) return false;
            if (!this.member.matches(ctx, node)) return false;
            Type[] arguments = Type.getArgumentTypes(method.desc);
            boolean isStatic = method.getOpcode() == Opcodes.INVOKESTATIC;
            int offset = isStatic ? 0 : 1;
            if (node.inputCount() != offset + arguments.length) return false;
            if (!isStatic && !this.receiver.matches(node.getInput(0), ctx)) return false;
            for (int i = 0; i < this.arguments.size(); i++) {
                if (!this.arguments.get(i).matches(node.getInput(offset + i), ctx)) return false;
            }
            return true;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            boolean isStatic = method.getOpcode() == Opcodes.INVOKESTATIC;
            ctx.capture(node, this, Type.getReturnType(method.desc), isStatic ? null : Type.getObjectType(method.owner), Type.getArgumentTypes(method.desc), null, null, false);
        }
    }

    public static class StaticMethodCallExpression extends ExpressionAst {
        private final MemberName member;
        private final List<Expression> arguments;

        public StaticMethodCallExpression(final ExpressionSource src, final MemberName member, final List<Expression> arguments) {
            super(src);
            this.member = member;
            this.arguments = arguments;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof MethodInsnNode)) return false;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            if (method.getOpcode() != Opcodes.INVOKESTATIC) return false;
            if (!this.member.matches(ctx, node)) return false;
            if (node.inputCount() != this.arguments.size()) return false;
            for (int i = 0; i < this.arguments.size(); i++) {
                if (!this.arguments.get(i).matches(node.getInput(i), ctx)) return false;
            }
            return true;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            ctx.capture(node, this, Type.getReturnType(method.desc), null, Type.getArgumentTypes(method.desc), null, null, false);
        }
    }

    public static class SuperCallExpression extends ExpressionAst {
        private final MemberName member;
        private final List<Expression> arguments;

        public SuperCallExpression(final ExpressionSource src, final MemberName member, final List<Expression> arguments) {
            super(src);
            this.member = member;
            this.arguments = arguments;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof MethodInsnNode)) return false;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            if (method.getOpcode() != Opcodes.INVOKESPECIAL || method.name.equals("<init>")) return false;
            if (!this.member.matches(ctx, node)) return false;
            int offset = 1;
            if (node.inputCount() != offset + this.arguments.size()) return false;
            for (int i = 0; i < this.arguments.size(); i++) {
                if (!this.arguments.get(i).matches(node.getInput(offset + i), ctx)) return false;
            }
            return true;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            ctx.capture(node, this, Type.getReturnType(method.desc), Type.getObjectType(method.owner), Type.getArgumentTypes(method.desc), null, null, false);
        }
    }

    public static class InstantiationExpression extends ExpressionAst {
        private final TypeIdentifier type;
        private final List<Expression> arguments;

        public InstantiationExpression(final ExpressionSource src, final TypeIdentifier type, final List<Expression> arguments) {
            super(src);
            this.type = type;
            this.arguments = arguments;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof MethodInsnNode)) return false;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            if (!method.name.equals("<init>") || node.inputCount() < 1) return false;
            ExpressionFlow.FlowValue newValue = node.getInput(0);
            if (newValue.isComplex() || newValue.getInsn().getOpcode() != Opcodes.NEW) return false;
            if (!this.type.matches(ctx.getIdentifiers(), Type.getObjectType(((TypeInsnNode) newValue.getInsn()).desc))) return false;
            if (node.inputCount() - 1 != this.arguments.size()) return false;
            for (int i = 0; i < this.arguments.size(); i++) {
                if (!this.arguments.get(i).matches(node.getInput(i + 1), ctx)) return false;
            }
            return true;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            MethodInsnNode method = (MethodInsnNode) node.getInsn();
            ctx.capture(node, this, Type.getObjectType(method.owner), null, Type.getArgumentTypes(method.desc), null, null, false);
        }
    }

    public static class NewArrayExpression extends ExpressionAst {
        private final TypeIdentifier innerType;
        private final List<Expression> dimensions;
        private final int blankDimensions;

        public NewArrayExpression(final ExpressionSource src, final TypeIdentifier innerType, final List<Expression> dimensions, final int blankDimensions) {
            super(src);
            this.innerType = innerType;
            this.dimensions = dimensions;
            this.blankDimensions = blankDimensions;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            int opcode = node.getInsn().getOpcode();
            Type elementType;
            int newBlankDimensions;
            if (opcode == Opcodes.NEWARRAY) {
                if (!(node.getInsn() instanceof IntInsnNode)) return false;
                elementType = getPrimitiveType(((IntInsnNode) node.getInsn()).operand);
                newBlankDimensions = 0;
            } else if (opcode == Opcodes.ANEWARRAY) {
                Type type = Type.getObjectType(((TypeInsnNode) node.getInsn()).desc);
                elementType = type.getSort() == Type.ARRAY ? type.getElementType() : type;
                newBlankDimensions = type.getSort() == Type.ARRAY ? type.getDimensions() : 0;
            } else if (opcode == Opcodes.MULTIANEWARRAY) {
                MultiANewArrayInsnNode multi = (MultiANewArrayInsnNode) node.getInsn();
                Type type = Type.getType(multi.desc);
                elementType = type.getElementType();
                newBlankDimensions = type.getDimensions() - multi.dims;
            } else {
                return false;
            }
            if (!this.innerType.matches(ctx.getIdentifiers(), elementType)) return false;
            if (node.inputCount() != this.dimensions.size()) return false;
            if (newBlankDimensions + node.inputCount() != this.blankDimensions + this.dimensions.size()) return false;
            for (int i = 0; i < this.dimensions.size(); i++) {
                if (!this.dimensions.get(i).matches(node.getInput(i), ctx)) return false;
            }
            return true;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            Type type;
            switch (node.getInsn().getOpcode()) {
                case Opcodes.NEWARRAY:
                    type = Type.getType("[" + getPrimitiveDescriptor(((IntInsnNode) node.getInsn()).operand));
                    break;
                case Opcodes.ANEWARRAY:
                    type = Type.getType("[" + Type.getObjectType(((TypeInsnNode) node.getInsn()).desc).getDescriptor());
                    break;
                default:
                    type = Type.getType(((MultiANewArrayInsnNode) node.getInsn()).desc);
            }
            ctx.capture(node, this, type, null, new Type[]{Type.INT_TYPE}, null, null, false);
        }

        private static Type getPrimitiveType(final int operand) {
            switch (operand) {
                case Opcodes.T_BOOLEAN: return Type.BOOLEAN_TYPE;
                case Opcodes.T_CHAR: return Type.CHAR_TYPE;
                case Opcodes.T_BYTE: return Type.BYTE_TYPE;
                case Opcodes.T_SHORT: return Type.SHORT_TYPE;
                case Opcodes.T_INT: return Type.INT_TYPE;
                case Opcodes.T_FLOAT: return Type.FLOAT_TYPE;
                case Opcodes.T_DOUBLE: return Type.DOUBLE_TYPE;
                case Opcodes.T_LONG: return Type.LONG_TYPE;
                default: throw new IllegalArgumentException("Unknown array type " + operand);
            }
        }

        private static String getPrimitiveDescriptor(final int operand) {
            switch (operand) {
                case Opcodes.T_BOOLEAN: return "Z";
                case Opcodes.T_CHAR: return "C";
                case Opcodes.T_BYTE: return "B";
                case Opcodes.T_SHORT: return "S";
                case Opcodes.T_INT: return "I";
                case Opcodes.T_FLOAT: return "F";
                case Opcodes.T_DOUBLE: return "D";
                case Opcodes.T_LONG: return "J";
                default: throw new IllegalArgumentException("Unknown array type " + operand);
            }
        }
    }

    public static class ArrayAccessExpression extends ExpressionAst {
        private final Expression array;
        private final Expression index;

        public ArrayAccessExpression(final ExpressionSource src, final Expression array, final Expression index) {
            super(src);
            this.array = array;
            this.index = index;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            int opcode = node.getInsn().getOpcode();
            if (!(opcode >= Opcodes.IALOAD && opcode <= Opcodes.SALOAD)) return false;
            return node.inputCount() == 2 && this.array.matches(node.getInput(0), ctx) && this.index.matches(node.getInput(1), ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            Type arrayType = node.getInput(0).getType();
            ctx.capture(node, this, ExpressionTypes.getInnerType(arrayType), null, new Type[]{arrayType, Type.INT_TYPE}, null, null, false);
        }

        public Expression getArray() {
            return this.array;
        }

        public Expression getIndex() {
            return this.index;
        }
    }

    public static class ArrayStoreExpression extends ExpressionAst {
        private final Expression array;
        private final Expression index;
        private final Expression value;

        public ArrayStoreExpression(final ExpressionSource src, final Expression array, final Expression index, final Expression value) {
            super(src);
            this.array = array;
            this.index = index;
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            int opcode = node.getInsn().getOpcode();
            if (!(opcode >= Opcodes.IASTORE && opcode <= Opcodes.SASTORE)) return false;
            return node.inputCount() == 3
                    && this.array.matches(node.getInput(0), ctx)
                    && this.index.matches(node.getInput(1), ctx)
                    && this.value.matches(node.getInput(2), ctx);
        }
    }

    public static class BinaryExpression extends ExpressionAst {
        private final Expression left;
        private final Operator operator;
        private final Expression right;

        public BinaryExpression(final ExpressionSource src, final Expression left, final Operator operator, final Expression right) {
            super(src);
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || node.inputCount() != 2) return false;
            if (!this.operator.matches(node.getInsn().getOpcode())) return false;
            return this.left.matches(node.getInput(0), ctx) && this.right.matches(node.getInput(1), ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            ctx.capture(node, this, node.getType(), null, new Type[]{node.getInput(0).getType(), node.getInput(1).getType()}, null, null, false);
        }


        public enum Operator {
            PLUS(Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD),
            MINUS(Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB),
            MULT(Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL),
            DIV(Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV),
            MOD(Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM),
            SHL(Opcodes.ISHL, Opcodes.LSHL),
            SHR(Opcodes.ISHR, Opcodes.LSHR),
            USHR(Opcodes.IUSHR, Opcodes.LUSHR),
            BITWISE_AND(Opcodes.IAND, Opcodes.LAND),
            BITWISE_XOR(Opcodes.IXOR, Opcodes.LXOR),
            BITWISE_OR(Opcodes.IOR, Opcodes.LOR);

            private final int[] opcodes;

            Operator(final int... opcodes) {
                this.opcodes = opcodes;
            }

            public boolean matches(final int opcode) {
                for (int candidate : this.opcodes) if (candidate == opcode) return true;
                return false;
            }
        }
    }

    public static class UnaryExpression extends ExpressionAst {
        private final Operator operator;
        private final Expression expression;

        public UnaryExpression(final ExpressionSource src, final Operator operator, final Expression expression) {
            super(src);
            this.operator = operator;
            this.expression = expression;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            int opcode = node.getInsn().getOpcode();
            if (this.operator == Operator.MINUS) {
                if (opcode != Opcodes.INEG && opcode != Opcodes.LNEG && opcode != Opcodes.FNEG && opcode != Opcodes.DNEG) return false;
                return node.inputCount() == 1 && this.expression.matches(node.getInput(0), ctx);
            } else {
                if (node.inputCount() != 2 || (opcode != Opcodes.IXOR && opcode != Opcodes.LXOR)) return false;
                if (node.getInput(1).isComplex()) return false;
                Object constant = getConstant(node.getInput(1).getInsn());
                if (!(constant instanceof Number) || ((Number) constant).longValue() != -1L) return false;
                return this.expression.matches(node.getInput(0), ctx);
            }
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            ctx.capture(node, this, node.getType(), null, new Type[]{node.getInput(0).getType()}, null, null, false);
        }

        public enum Operator {
            MINUS, BITWISE_NOT
        }
    }

    public static class ComparisonExpression extends ExpressionAst {
        private final Expression left;
        private final Operator operator;
        private final Expression right;

        public ComparisonExpression(final ExpressionSource src, final Expression left, final Operator operator, final Expression right) {
            super(src);
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof JumpInsnNode)) return false;
            JumpInsnNode jump = (JumpInsnNode) node.getInsn();
            ComparisonMatch match = this.findMatch(node, jump);
            if (match == null) return false;
            if (match.implicit) {
                ExpressionFlow.FlowValue operand = node.getInput(0);
                if (this.left.matches(operand, ctx)) {
                    return match.implicitNull ? this.right.matchesNull() : this.right.matchesZero();
                }
                if (this.right.matches(operand, ctx)) {
                    return match.implicitNull ? this.left.matchesNull() : this.left.matchesZero();
                }
                return false;
            }
            return this.left.matches(match.left, ctx) && this.right.matches(match.right, ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            JumpInsnNode jump = (JumpInsnNode) node.getInsn();
            ComparisonMatch match = this.findMatch(node, jump);
            if (match == null) return;
            Type[] argumentTypes;
            if (match.implicit) {
                argumentTypes = new Type[]{node.getInput(0).getType()};
            } else if (match.comparison != null) {
                argumentTypes = new Type[]{node.getInput(0).getInput(0).getType(), node.getInput(0).getInput(1).getType()};
            } else {
                argumentTypes = new Type[]{node.getInput(0).getType(), node.getInput(1).getType()};
            }
            ctx.capture(node, this, Type.BOOLEAN_TYPE, null, argumentTypes, jump, match.comparison, match.inverted);
        }

        private ComparisonMatch findMatch(final ExpressionFlow.FlowValue node, final JumpInsnNode jump) {
            int opcode = jump.getOpcode();
            boolean implicit = opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL;
            boolean implicitNull = opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL;
            ExpressionFlow.FlowValue first;
            ExpressionFlow.FlowValue second;
            AbstractInsnNode comparison = null;
            if (implicit) {
                first = node.getInput(0);
                second = null;
                if (!implicitNull && first.getType().getSort() != Type.INT) return null;
            } else {
                if (node.inputCount() == 1) {
                    first = node.getInput(0);
                    if (first.getInsn() == null || first.getInsn().getOpcode() < Opcodes.LCMP || first.getInsn().getOpcode() > Opcodes.DCMPG) return null;
                    comparison = first.getInsn();
                    if (first.inputCount() != 2) return null;
                    second = first.getInput(1);
                    first = first.getInput(0);
                } else if (node.inputCount() == 2) {
                    first = node.getInput(0);
                    second = node.getInput(1);
                } else {
                    return null;
                }
            }
            boolean direct = this.operator.isDirect(opcode, implicitNull, comparison != null);
            boolean inverted = this.operator.isInverted(opcode, implicitNull, comparison != null);
            if (!direct && !inverted) return null;
            Type type = implicit ? first.getType() : (comparison != null ? first.getType() : (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE ? Type.getType(Object.class) : first.getType()));
            return new ComparisonMatch(first, second, type, implicit, implicitNull, comparison, inverted);
        }


        public enum Operator {
            EQ, NE, LT, LE, GT, GE;

            public boolean isDirect(final int opcode, final boolean implicitNull, final boolean complex) {
                if (implicitNull) return this == EQ && opcode == Opcodes.IFNULL || this == NE && opcode == Opcodes.IFNONNULL;
                if (complex) {
                    switch (this) {
                        case EQ: return opcode == Opcodes.IFEQ;
                        case NE: return opcode == Opcodes.IFNE;
                        case LT: return opcode == Opcodes.IFLT;
                        case LE: return opcode == Opcodes.IFLE;
                        case GT: return opcode == Opcodes.IFGT;
                        case GE: return opcode == Opcodes.IFGE;
                    }
                }
                if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE) {
                    switch (this) {
                        case EQ: return opcode == Opcodes.IFEQ;
                        case NE: return opcode == Opcodes.IFNE;
                        case LT: return opcode == Opcodes.IFLT;
                        case LE: return opcode == Opcodes.IFLE;
                        case GT: return opcode == Opcodes.IFGT;
                        case GE: return opcode == Opcodes.IFGE;
                    }
                }
                switch (this) {
                    case EQ: return opcode == Opcodes.IF_ICMPEQ || opcode == Opcodes.IF_ACMPEQ;
                    case NE: return opcode == Opcodes.IF_ICMPNE || opcode == Opcodes.IF_ACMPNE;
                    case LT: return opcode == Opcodes.IF_ICMPLT;
                    case LE: return opcode == Opcodes.IF_ICMPLE;
                    case GT: return opcode == Opcodes.IF_ICMPGT;
                    case GE: return opcode == Opcodes.IF_ICMPGE;
                    default: return false;
                }
            }

            public boolean isInverted(final int opcode, final boolean implicitNull, final boolean complex) {
                if (implicitNull) return false;
                if (complex) {
                    switch (this) {
                        case EQ: return opcode == Opcodes.IFNE;
                        case NE: return opcode == Opcodes.IFEQ;
                        case LT: return opcode == Opcodes.IFGE;
                        case LE: return opcode == Opcodes.IFGT;
                        case GT: return opcode == Opcodes.IFLE;
                        case GE: return opcode == Opcodes.IFLT;
                    }
                }
                if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE) {
                    switch (this) {
                        case EQ: return opcode == Opcodes.IFNE;
                        case NE: return opcode == Opcodes.IFEQ;
                        case LT: return opcode == Opcodes.IFGE;
                        case LE: return opcode == Opcodes.IFGT;
                        case GT: return opcode == Opcodes.IFLE;
                        case GE: return opcode == Opcodes.IFLT;
                    }
                }
                switch (this) {
                    case EQ: return opcode == Opcodes.IF_ICMPNE || opcode == Opcodes.IF_ACMPNE;
                    case NE: return opcode == Opcodes.IF_ICMPEQ || opcode == Opcodes.IF_ACMPEQ;
                    case LT: return opcode == Opcodes.IF_ICMPGE;
                    case LE: return opcode == Opcodes.IF_ICMPGT;
                    case GT: return opcode == Opcodes.IF_ICMPLE;
                    case GE: return opcode == Opcodes.IF_ICMPLT;
                    default: return false;
                }
            }
        }

        private static class ComparisonMatch {
            private final ExpressionFlow.FlowValue left;
            private final ExpressionFlow.FlowValue right;
            private final Type type;
            private final boolean implicit;
            private final boolean implicitNull;
            private final AbstractInsnNode comparison;
            private final boolean inverted;

            private ComparisonMatch(final ExpressionFlow.FlowValue left, final ExpressionFlow.FlowValue right, final Type type, final boolean implicit, final boolean implicitNull, final AbstractInsnNode comparison, final boolean inverted) {
                this.left = left;
                this.right = right;
                this.type = type;
                this.implicit = implicit;
                this.implicitNull = implicitNull;
                this.comparison = comparison;
                this.inverted = inverted;
            }
        }
    }

    public static class CastExpression extends ExpressionAst {
        private final TypeIdentifier type;
        private final Expression expression;

        public CastExpression(final ExpressionSource src, final TypeIdentifier type, final Expression expression) {
            super(src);
            this.type = type;
            this.expression = expression;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            Type castType = ExpressionTypes.getCastType(node.getInsn());
            if (castType == null || !this.type.matches(ctx.getIdentifiers(), castType)) return false;
            return node.inputCount() == 1 && this.expression.matches(node.getInput(0), ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            ctx.capture(node, this, ExpressionTypes.getCastType(node.getInsn()), null, new Type[]{node.getInput(0).getType()}, null, null, false);
        }
    }

    public static class InstanceofExpression extends ExpressionAst {
        private final Expression expression;
        private final TypeIdentifier type;

        public InstanceofExpression(final ExpressionSource src, final Expression expression, final TypeIdentifier type) {
            super(src);
            this.expression = expression;
            this.type = type;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || node.getInsn().getOpcode() != Opcodes.INSTANCEOF) return false;
            if (!this.type.matches(ctx.getIdentifiers(), Type.getObjectType(((TypeInsnNode) node.getInsn()).desc))) return false;
            return node.inputCount() == 1 && this.expression.matches(node.getInput(0), ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            ctx.capture(node, this, Type.BOOLEAN_TYPE, null, new Type[]{Type.getType(Object.class)}, null, null, false);
        }
    }

    public static class MemberAssignmentExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;
        private final Expression value;

        public MemberAssignmentExpression(final ExpressionSource src, final Expression receiver, final MemberName member, final Expression value) {
            super(src);
            this.receiver = receiver;
            this.member = member;
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof FieldInsnNode)) return false;
            FieldInsnNode field = (FieldInsnNode) node.getInsn();
            if (field.getOpcode() != Opcodes.PUTFIELD && field.getOpcode() != Opcodes.PUTSTATIC) return false;
            if (!this.member.matches(ctx, node)) return false;
            if (field.getOpcode() == Opcodes.PUTFIELD) {
                return node.inputCount() == 2 && this.receiver.matches(node.getInput(0), ctx) && this.value.matches(node.getInput(1), ctx);
            }
            return node.inputCount() == 1 && this.value.matches(node.getInput(0), ctx);
        }
    }

    public static class IdentifierAssignmentExpression extends ExpressionAst {
        private final String identifier;
        private final Expression value;

        public IdentifierAssignmentExpression(final ExpressionSource src, final String identifier, final Expression value) {
            super(src);
            this.identifier = identifier;
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || !(node.getInsn() instanceof VarInsnNode)) return false;
            int opcode = node.getInsn().getOpcode();
            if (opcode < Opcodes.ISTORE || opcode > Opcodes.ASTORE) return false;
            return node.inputCount() == 1 && this.value.matches(node.getInput(0), ctx) && ctx.getIdentifiers().matchesMember(this.identifier, node);
        }
    }

    public static class ReturnExpression extends ExpressionAst {
        private final Expression value;

        public ReturnExpression(final ExpressionSource src, final Expression value) {
            super(src);
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            int opcode = node.getInsn().getOpcode();
            if (!(opcode >= Opcodes.IRETURN && opcode <= Opcodes.ARETURN)) return false;
            return node.inputCount() == 1 && this.value.matches(node.getInput(0), ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            ctx.capture(node, this, node.getInput(0).getType(), null, new Type[0], null, null, false);
        }
    }

    public static class ThrowExpression extends ExpressionAst {
        private final Expression value;

        public ThrowExpression(final ExpressionSource src, final Expression value) {
            super(src);
            this.value = value;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex() || node.getInsn().getOpcode() != Opcodes.ATHROW) return false;
            return node.inputCount() == 1 && this.value.matches(node.getInput(0), ctx);
        }
    }


    public interface MemberName {
        boolean matches(ExpressionContext ctx, ExpressionFlow.FlowValue node);
    }

    public interface TypeIdentifier {
        boolean matches(ExpressionIdentifiers identifiers, Type type);
    }

}
