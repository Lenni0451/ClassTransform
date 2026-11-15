package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * The expression AST used by the expression engine.
 * Every node is a small matcher for one shape of the value flow graph, from literals and wildcards over member
 * and array accesses to operators and assignments. Nodes are immutable and keep the {@link ExpressionSource}
 * range they were parsed from; matching is done by {@link #matches(ExpressionFlow.FlowValue, ExpressionContext)}
 * and capturing by {@link #capture(ExpressionFlow.FlowValue, ExpressionContext)}.
 */
@ParametersAreNonnullByDefault
public abstract class ExpressionAst implements Expression {

    /**
     * The range of the expression string this node was parsed from.
     */
    protected final ExpressionSource src;

    /**
     * Creates a node for the given source range.
     *
     * @param src the range in the expression string
     */
    protected ExpressionAst(final ExpressionSource src) {
        this.src = src;
    }

    /**
     * Returns the range of the expression string this node was parsed from.
     *
     * @return the source range of this node
     */
    public ExpressionSource getSource() {
        return this.src;
    }

    /**
     * Tests if the given value satisfies this node.
     * <p>
     * Implementations only inspect the value graph and normally record no captures;
     * {@link #capture(ExpressionFlow.FlowValue, ExpressionContext)} must be called after a successful match.
     * The exception is {@link CapturingExpression}, which captures its inner expression while matching. Complex
     * values are rejected unless the node is a wildcard.
     *
     * @param node the value to match
     * @param ctx  the context of the current match
     * @return whether the value satisfies this node
     */
    public abstract boolean matches(ExpressionFlow.FlowValue node, ExpressionContext ctx);

    /**
     * Captures the instruction which produced the given value.
     * <p>
     * Records the value type and, for operations with a receiver or arguments, their types. Complex values are
     * ignored.
     *
     * @param node the matched value
     * @param ctx  the context to add the capture to
     */
    public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
        if (node.isComplex()) return;
        ctx.capture(node, this, node.getType(), null, new Type[0], null, null, false);
    }

    /**
     * Matches all inputs of a value against the given expressions.
     * <p>
     * The number of expressions has to equal the number of inputs. Complex values never match.
     *
     * @param node   the value whose inputs are matched
     * @param ctx    the context of the current match
     * @param values the expressions for the inputs, in input order
     * @return whether every input matches its expression
     */
    protected boolean inputsMatch(final ExpressionFlow.FlowValue node, final ExpressionContext ctx, final Expression... values) {
        return this.inputsMatch(node, ctx, false, values);
    }

    /**
     * Matches the inputs of a value against the given expressions.
     * <p>
     * By default the number of expressions has to equal the number of inputs; if {@code allowIncomplete} is
     * set, a shorter expression list matches the first inputs and the remaining ones are ignored. Complex values
     * never match.
     *
     * @param node            the value whose inputs are matched
     * @param ctx             the context of the current match
     * @param allowIncomplete whether more inputs than expressions are allowed
     * @param values          the expressions for the inputs, in input order
     * @return whether every matched input satisfies its expression
     */
    protected boolean inputsMatch(final ExpressionFlow.FlowValue node, final ExpressionContext ctx, final boolean allowIncomplete, final Expression... values) {
        if (node.isComplex()) return false;
        int required = node.inputCount();
        if (!(allowIncomplete && values.length < required) && values.length != required) return false;
        for (int i = 0; i < values.length; i++) {
            if (!values[i].matches(node.getInput(i), ctx)) return false;
        }
        return true;
    }

    /**
     * Tests if a value is the {@code null} constant.
     *
     * @param node the value to test
     * @return whether the value was produced by {@code ACONST_NULL}
     */
    protected static boolean matchesNull(final ExpressionFlow.FlowValue node) {
        return !node.isComplex() && node.getInsn().getOpcode() == Opcodes.ACONST_NULL;
    }

    /**
     * Returns the compile-time constant pushed by an instruction.
     * <p>
     * Supports the primitive constant instructions, {@code BIPUSH}, {@code SIPUSH} and {@code LDC} (including
     * non-numeric constants such as strings and types).
     *
     * @param insn the instruction
     * @return the pushed constant, or null if the instruction does not push a constant
     */
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


    /**
     * A capturing expression such as {@code @(?)}.
     * Matches its inner expression and records the captures of the inner expression instead of itself, allowing
     * a sub-operation of a larger expression to be selected as injection target.
     */
    public static class CapturingExpression extends ExpressionAst {
        private final Expression inner;

        /**
         * Creates a capturing expression.
         *
         * @param src   the range of the {@code @(...)} expression
         * @param inner the expression whose captures should be recorded
         */
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

        /**
         * Returns the wrapped expression.
         *
         * @return the expression whose captures are recorded
         */
        public Expression getInner() {
            return this.inner;
        }
    }

    /**
     * The wildcard expression {@code ?}.
     * Matches every value, including values merged from multiple branches.
     */
    public static class WildcardExpression extends ExpressionAst {
        /**
         * Creates a wildcard expression.
         *
         * @param src the range of the {@code ?} token
         */
        public WildcardExpression(final ExpressionSource src) {
            super(src);
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            return true;
        }
    }

    /**
     * The {@code this} expression.
     * Matches a load of local variable 0, i.e. the receiver of an instance method.
     */
    public static class ThisExpression extends ExpressionAst {
        /**
         * Creates a {@code this} expression.
         *
         * @param src the range of the {@code this} token
         */
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

    /**
     * An integer literal such as {@code 42} or {@code 0x2A}.
     * Matches numeric constants with an equal value.
     */
    public static class IntLiteralExpression extends ExpressionAst {
        private final long value;

        /**
         * Creates an integer literal.
         *
         * @param src   the range of the literal
         * @param value the literal value
         */
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

    /**
     * A floating-point literal such as {@code 1.5}.
     * Matches numeric constants with an equal floating-point value.
     */
    public static class DecimalLiteralExpression extends ExpressionAst {
        private final double value;

        /**
         * Creates a decimal literal.
         *
         * @param src   the range of the literal
         * @param value the literal value
         */
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

    /**
     * A string literal such as {@code 'text'}.
     * Matches an {@code LDC} instruction pushing an equal string.
     */
    public static class StringLiteralExpression extends ExpressionAst {
        private final String value;

        /**
         * Creates a string literal.
         *
         * @param src   the range of the literal
         * @param value the unescaped literal value without quotes
         */
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

    /**
     * The {@code true} or {@code false} literal.
     * Matches numeric constants which are non-zero for {@code true} and zero for {@code false}.
     */
    public static class BooleanLiteralExpression extends ExpressionAst {
        private final boolean value;

        /**
         * Creates a boolean literal.
         *
         * @param src   the range of the literal
         * @param value the literal value
         */
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

    /**
     * The {@code null} literal.
     * Matches an {@code ACONST_NULL} instruction.
     */
    public static class NullLiteralExpression extends ExpressionAst {
        /**
         * Creates a {@code null} literal.
         *
         * @param src the range of the {@code null} token
         */
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

    /**
     * A class constant such as {@code String.class}.
     * Matches an {@code LDC} of a type whose class matches the type identifier.
     */
    public static class ClassConstantExpression extends ExpressionAst {
        private final TypeIdentifier type;

        /**
         * Creates a class constant expression.
         *
         * @param src  the range of the {@code .class} expression
         * @param type the expected type
         */
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

    /**
     * A reference to a declared member by its identifier.
     * The identifier is resolved through the {@link ExpressionIdentifiers} of the context, so it can match
     * methods, fields and local variables depending on the {@code @CDefinition}.
     */
    public static class IdentifierExpression extends ExpressionAst {
        private final String identifier;

        /**
         * Creates an identifier reference.
         *
         * @param src        the range of the identifier token
         * @param identifier the declared identifier name
         */
        public IdentifierExpression(final ExpressionSource src, final String identifier) {
            super(src);
            this.identifier = identifier;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            return ctx.getIdentifiers().matchesMember(this.identifier, node);
        }

        /**
         * Returns the referenced identifier.
         *
         * @return the identifier name
         */
        public String getIdentifier() {
            return this.identifier;
        }
    }

    /**
     * A field read such as {@code receiver.member}.
     * Matches {@code GETFIELD} with a matching receiver and {@code GETSTATIC} without inputs.
     */
    public static class MemberAccessExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;

        /**
         * Creates a field read.
         *
         * @param src      the range of the member access
         * @param receiver the expression matching the receiver instance
         * @param member   the field name matcher
         */
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

        /**
         * Returns the expression matching the receiver instance.
         *
         * @return the receiver expression
         */
        public Expression getReceiver() {
            return this.receiver;
        }

        /**
         * Returns the field name matcher.
         *
         * @return the member matcher
         */
        public MemberName getMember() {
            return this.member;
        }
    }

    /**
     * An instance method call such as {@code receiver.member(args)}.
     * Matches a non-constructor method call with a matching receiver, member name and arguments; static calls
     * are handled by {@link StaticMethodCallExpression}.
     */
    public static class MethodCallExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;
        private final List<Expression> arguments;

        /**
         * Creates an instance method call.
         *
         * @param src       the range of the call
         * @param receiver  the expression matching the receiver instance
         * @param member    the method name matcher
         * @param arguments the expressions matching the arguments, in order
         */
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

    /**
     * A static method call such as {@code member(args)}.
     * Matches an {@code INVOKESTATIC} instruction with a matching member name and arguments.
     */
    public static class StaticMethodCallExpression extends ExpressionAst {
        private final MemberName member;
        private final List<Expression> arguments;

        /**
         * Creates a static method call.
         *
         * @param src       the range of the call
         * @param member    the method name matcher
         * @param arguments the expressions matching the arguments, in order
         */
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

    /**
     * A super method call such as {@code super.member(args)}.
     * Matches an {@code INVOKESPECIAL} non-constructor call; the receiver is the first input and is not matched
     * against an expression.
     */
    public static class SuperCallExpression extends ExpressionAst {
        private final MemberName member;
        private final List<Expression> arguments;

        /**
         * Creates a super method call.
         *
         * @param src       the range of the call
         * @param member    the method name matcher
         * @param arguments the expressions matching the arguments, in order
         */
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

    /**
     * A method reference with an explicit receiver such as {@code receiver::member}.
     * For bound references the receiver is matched against the first lambda input with synthetic null checks
     * removed. For free references such as {@code Type::method} the receiver has to be an identifier naming a
     * type which matches the implementation owner.
     */
    public static class BoundMethodReferenceExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;

        /**
         * Creates a method reference with receiver.
         *
         * @param src      the range of the method reference
         * @param receiver the expression matching the bound receiver or naming the declaring type
         * @param member   the method name matcher
         */
        public BoundMethodReferenceExpression(final ExpressionSource src, final Expression receiver, final MemberName member) {
            super(src);
            this.receiver = receiver;
            this.member = member;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            LMFInfo info = ctx.getIdentifiers().getLMFInfo(node);
            if (info == null) return false;
            if (!this.member.matches(ctx, info.impl)) return false;
            if (info.type == LMFInfo.Type.BOUND_METHOD) {
                if (node.inputCount() < 1) return false;
                return this.receiver.matches(LMFInfo.unwrapReceiver(node.getInput(0)), ctx);
            }
            if (info.type != LMFInfo.Type.FREE_METHOD || !(this.receiver instanceof IdentifierExpression)) return false;
            String identifier = ((IdentifierExpression) this.receiver).getIdentifier();
            return ctx.getIdentifiers().typeExists(identifier)
                    && ctx.getIdentifiers().matchesType(identifier, Type.getObjectType(info.impl.getOwner()));
        }
    }

    /**
     * A free method reference such as {@code ::member}.
     * Matches a lambda whose implementation is an unbound instance or static method.
     */
    public static class FreeMethodReferenceExpression extends ExpressionAst {
        private final MemberName member;

        /**
         * Creates a free method reference.
         *
         * @param src    the range of the method reference
         * @param member the method name matcher
         */
        public FreeMethodReferenceExpression(final ExpressionSource src, final MemberName member) {
            super(src);
            this.member = member;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            LMFInfo info = ctx.getIdentifiers().getLMFInfo(node);
            if (info == null || info.type != LMFInfo.Type.FREE_METHOD) return false;
            return this.member.matches(ctx, info.impl);
        }
    }

    /**
     * A constructor reference such as {@code Type::new}.
     * Matches a lambda which instantiates the given type.
     */
    public static class ConstructorReferenceExpression extends ExpressionAst {
        private final TypeIdentifier type;

        /**
         * Creates a constructor reference.
         *
         * @param src  the range of the constructor reference
         * @param type the instantiated type
         */
        public ConstructorReferenceExpression(final ExpressionSource src, final TypeIdentifier type) {
            super(src);
            this.type = type;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            LMFInfo info = ctx.getIdentifiers().getLMFInfo(node);
            if (info == null || info.type != LMFInfo.Type.INSTANTIATION) return false;
            return this.type.matches(ctx.getIdentifiers(), Type.getObjectType(info.impl.getOwner()));
        }
    }

    /**
     * An instantiation such as {@code new Type(args)}.
     * Matches a constructor call whose first input is an uninitialized {@code NEW} value of the expected type;
     * the remaining inputs are matched against the constructor arguments.
     */
    public static class InstantiationExpression extends ExpressionAst {
        private final TypeIdentifier type;
        private final List<Expression> arguments;

        /**
         * Creates an instantiation expression.
         *
         * @param src       the range of the {@code new} expression
         * @param type      the instantiated type
         * @param arguments the expressions matching the constructor arguments, in order
         */
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

    /**
     * An array creation with explicit dimensions such as {@code new Type[dim][]}.
     * Matches {@code NEWARRAY}, {@code ANEWARRAY} and {@code MULTIANEWARRAY}; the specified dimensions are
     * matched against the inputs of the creation instruction.
     */
    public static class NewArrayExpression extends ExpressionAst {
        private final TypeIdentifier innerType;
        private final List<Expression> dimensions;
        private final int blankDimensions;

        /**
         * Creates an array creation expression.
         *
         * @param src            the range of the {@code new} expression
         * @param innerType      the type of the innermost element
         * @param dimensions     the expressions matching the dimensions with a size, in order
         * @param blankDimensions the number of trailing dimensions without a size
         */
        public NewArrayExpression(final ExpressionSource src, final TypeIdentifier innerType, final List<Expression> dimensions, final int blankDimensions) {
            super(src);
            this.innerType = innerType;
            this.dimensions = dimensions;
            this.blankDimensions = blankDimensions;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            if (NewArrayPostProcessor.getInfo(node) != null) return false;
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

    /**
     * An array literal such as {@code new Type[]{value, ...}}.
     * Requires the array to be created with a constant size and initialized by constant-index stores, as
     * detected by {@link NewArrayPostProcessor}; the values are matched against the stored values in index order.
     */
    public static class ArrayLiteralExpression extends ExpressionAst {
        private final TypeIdentifier elementType;
        private final List<Expression> values;

        /**
         * Creates an array literal.
         *
         * @param src         the range of the {@code new} expression
         * @param elementType the expected element type
         * @param values      the expressions matching the elements, in index order
         */
        public ArrayLiteralExpression(final ExpressionSource src, final TypeIdentifier elementType, final List<Expression> values) {
            super(src);
            this.elementType = elementType;
            this.values = values;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            ArrayCreationInfo creation = NewArrayPostProcessor.getInfo(node);
            if (creation == null) return false;
            Type newElementType;
            switch (node.getInsn().getOpcode()) {
                case Opcodes.NEWARRAY:
                    newElementType = NewArrayExpression.getPrimitiveType(((IntInsnNode) node.getInsn()).operand);
                    break;
                case Opcodes.ANEWARRAY:
                    newElementType = Type.getObjectType(((TypeInsnNode) node.getInsn()).desc);
                    break;
                default:
                    return false;
            }
            if (!this.elementType.matches(ctx.getIdentifiers(), newElementType)) return false;
            List<ExpressionFlow.FlowValue> stores = creation.getStores();
            if (stores.size() != this.values.size()) return false;
            for (int i = 0; i < this.values.size(); i++) {
                if (!this.values.get(i).matches(stores.get(i).getInput(2), ctx)) return false;
            }
            return true;
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            ArrayCreationInfo creation = NewArrayPostProcessor.getInfo(node);
            if (creation == null) return;
            ctx.capture(creation.getInitialized(), this, node.getType(), null, new Type[0], null, null, false);
        }
    }

    /**
     * An array element read such as {@code array[index]}.
     * Matches any of the typed array load instructions.
     */
    public static class ArrayAccessExpression extends ExpressionAst {
        private final Expression array;
        private final Expression index;

        /**
         * Creates an array access.
         *
         * @param src   the range of the access
         * @param array the expression matching the array
         * @param index the expression matching the index
         */
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

        /**
         * Returns the expression matching the array.
         *
         * @return the array expression
         */
        public Expression getArray() {
            return this.array;
        }

        /**
         * Returns the expression matching the index.
         *
         * @return the index expression
         */
        public Expression getIndex() {
            return this.index;
        }
    }

    /**
     * An array element write such as {@code array[index] = value}.
     * Matches any of the typed array store instructions with array, index and value as inputs.
     */
    public static class ArrayStoreExpression extends ExpressionAst {
        private final Expression array;
        private final Expression index;
        private final Expression value;

        /**
         * Creates an array store.
         *
         * @param src   the range of the assignment
         * @param array the expression matching the array
         * @param index the expression matching the index
         * @param value the expression matching the stored value
         */
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

    /**
     * A binary arithmetic or bitwise operation such as {@code left + right}.
     * Matches instructions with two inputs; a plus expression additionally matches string concatenation chains
     * and increments ({@code IINC}).
     */
    public static class BinaryExpression extends ExpressionAst {
        private final Expression left;
        private final Operator operator;
        private final Expression right;

        /**
         * Creates a binary expression.
         *
         * @param src      the range of the operator
         * @param left     the expression matching the left operand
         * @param operator the operator
         * @param right    the expression matching the right operand
         */
        public BinaryExpression(final ExpressionSource src, final Expression left, final Operator operator, final Expression right) {
            super(src);
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        public boolean matches(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return false;
            if (node.getInsn().getOpcode() == Opcodes.IINC) {
                if (this.operator != Operator.PLUS) return false;
                IincInsnNode iinc = (IincInsnNode) node.getInsn();
                ExpressionFlow.FlowValue load = new ExpressionFlow.FlowValue(Type.INT_TYPE, new VarInsnNode(Opcodes.ILOAD, iinc.var));
                ExpressionFlow.FlowValue constant = new ExpressionFlow.FlowValue(Type.INT_TYPE, new LdcInsnNode(iinc.incr));
                return this.left.matches(load, ctx) && this.right.matches(constant, ctx);
            }
            if (node.inputCount() == 2 && this.operator.matches(node.getInsn().getOpcode())) {
                return this.left.matches(node.getInput(0), ctx) && this.right.matches(node.getInput(1), ctx);
            }
            if (this.operator != Operator.PLUS) return false;
            StringConcatInfo concat = StringConcatInfo.get(node);
            if (concat == null) return false;
            List<ExpressionFlow.FlowValue> parts = concat.getParts();
            return this.matchesConcat(parts, parts.size(), ctx);
        }

        private boolean matchesConcat(final List<ExpressionFlow.FlowValue> parts, final int end, final ExpressionContext ctx) {
            if (end < 2) return false;
            if (!this.right.matches(parts.get(end - 1), ctx)) return false;
            if (end == 2) return this.left.matches(parts.get(0), ctx);
            if (this.left instanceof WildcardExpression) return true;
            if (this.left instanceof CapturingExpression) {
                Expression inner = ((CapturingExpression) this.left).getInner();
                if (inner instanceof WildcardExpression) return true;
                return inner instanceof BinaryExpression
                        && ((BinaryExpression) inner).operator == Operator.PLUS
                        && ((BinaryExpression) inner).matchesConcat(parts, end - 1, ctx);
            }
            return this.left instanceof BinaryExpression
                    && ((BinaryExpression) this.left).operator == Operator.PLUS
                    && ((BinaryExpression) this.left).matchesConcat(parts, end - 1, ctx);
        }

        @Override
        public void capture(final ExpressionFlow.FlowValue node, final ExpressionContext ctx) {
            if (node.isComplex()) return;
            if (node.getInsn().getOpcode() == Opcodes.IINC) {
                ctx.capture(node, this, Type.INT_TYPE, null, new Type[0], null, null, false);
                return;
            }
            Type[] argumentTypes = node.inputCount() == 2
                    ? new Type[]{node.getInput(0).getType(), node.getInput(1).getType()}
                    : new Type[0];
            ctx.capture(node, this, node.getType(), null, argumentTypes, null, null, false);
        }


        /**
         * The binary operators supported by the expression language.
         */
        public enum Operator {
            /**
             * Addition; also used for string concatenation and increment matches.
             */
            PLUS(Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD),
            /**
             * Subtraction.
             */
            MINUS(Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB),
            /**
             * Multiplication.
             */
            MULT(Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL),
            /**
             * Division.
             */
            DIV(Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV),
            /**
             * Remainder.
             */
            MOD(Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM),
            /**
             * Left shift for int and long values.
             */
            SHL(Opcodes.ISHL, Opcodes.LSHL),
            /**
             * Arithmetic right shift for int and long values.
             */
            SHR(Opcodes.ISHR, Opcodes.LSHR),
            /**
             * Unsigned right shift for int and long values.
             */
            USHR(Opcodes.IUSHR, Opcodes.LUSHR),
            /**
             * Bitwise and.
             */
            BITWISE_AND(Opcodes.IAND, Opcodes.LAND),
            /**
             * Bitwise xor.
             */
            BITWISE_XOR(Opcodes.IXOR, Opcodes.LXOR),
            /**
             * Bitwise or.
             */
            BITWISE_OR(Opcodes.IOR, Opcodes.LOR);

            private final int[] opcodes;

            Operator(final int... opcodes) {
                this.opcodes = opcodes;
            }

            /**
             * Tests if the given opcode performs this operation.
             *
             * @param opcode the JVM opcode
             * @return whether the opcode belongs to this operator
             */
            public boolean matches(final int opcode) {
                for (int candidate : this.opcodes) if (candidate == opcode) return true;
                return false;
            }
        }
    }

    /**
     * A unary operation such as {@code -value} or {@code ~value}.
     */
    public static class UnaryExpression extends ExpressionAst {
        private final Operator operator;
        private final Expression expression;

        /**
         * Creates a unary expression.
         *
         * @param src        the range of the operator
         * @param operator   the operator
         * @param expression the expression matching the operand
         */
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

        /**
         * The unary operators supported by the expression language.
         */
        public enum Operator {
            /**
             * Arithmetic negation of int, long, float or double values.
             */
            MINUS,
            /**
             * Bitwise complement, encoded as xor with {@code -1}.
             */
            BITWISE_NOT
        }
    }

    /**
     * A relational comparison such as {@code left < right}.
     * Matches conditional jump instructions, including implicit comparisons against zero or null, comparisons
     * of two values and comparisons whose operands are prepared by a compare instruction like {@code LCMP}.
     * Captures the jump so the engine can rewrite the condition.
     */
    public static class ComparisonExpression extends ExpressionAst {
        private final Expression left;
        private final Operator operator;
        private final Expression right;

        /**
         * Creates a comparison.
         *
         * @param src      the range of the comparison operator
         * @param left     the expression matching the left operand
         * @param operator the comparison operator
         * @param right    the expression matching the right operand
         */
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


        /**
         * The relational operators supported by the expression language.
         */
        public enum Operator {
            /**
             * Equality ({@code ==}).
             */
            EQ,
            /**
             * Inequality ({@code !=}).
             */
            NE,
            /**
             * Less than ({@code <}).
             */
            LT,
            /**
             * Less than or equal ({@code <=}).
             */
            LE,
            /**
             * Greater than ({@code >}).
             */
            GT,
            /**
             * Greater than or equal ({@code >=}).
             */
            GE;

            /**
             * Tests if this operator is encoded directly by a jump opcode.
             *
             * @param opcode       the jump opcode
             * @param implicitNull whether the jump compares against null
             * @param complex      whether the operands were prepared by a compare instruction
             * @return whether the opcode expresses this operator without inversion
             */
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

            /**
             * Tests if this operator is encoded by the inverted jump opcode.
             *
             * @param opcode       the jump opcode
             * @param implicitNull whether the jump compares against null
             * @param complex      whether the operands were prepared by a compare instruction
             * @return whether the opcode expresses the inverse of this operator
             */
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

    /**
     * A cast such as {@code (Type) expression}.
     * Matches reference casts ({@code CHECKCAST}) as well as the primitive conversion instructions.
     */
    public static class CastExpression extends ExpressionAst {
        private final TypeIdentifier type;
        private final Expression expression;

        /**
         * Creates a cast expression.
         *
         * @param src        the range of the cast
         * @param type       the target type of the cast
         * @param expression the expression matching the cast value
         */
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

    /**
     * An instanceof check such as {@code expression instanceof Type}.
     * Matches an {@code INSTANCEOF} instruction with the expected type and value.
     */
    public static class InstanceofExpression extends ExpressionAst {
        private final Expression expression;
        private final TypeIdentifier type;

        /**
         * Creates an instanceof expression.
         *
         * @param src        the range of the check
         * @param expression the expression matching the checked value
         * @param type       the checked type
         */
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

    /**
     * A field write such as {@code receiver.member = value}.
     * Matches {@code PUTFIELD} with a matching receiver and value and {@code PUTSTATIC} with a matching value.
     */
    public static class MemberAssignmentExpression extends ExpressionAst {
        private final Expression receiver;
        private final MemberName member;
        private final Expression value;

        /**
         * Creates a field write.
         *
         * @param src      the range of the assignment
         * @param receiver the expression matching the receiver instance
         * @param member   the field name matcher
         * @param value    the expression matching the assigned value
         */
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

    /**
     * An assignment to a local variable or parameter such as {@code identifier = value}.
     * Matches a store instruction whose variable matches the identifier and whose stored value matches the
     * value expression.
     */
    public static class IdentifierAssignmentExpression extends ExpressionAst {
        private final String identifier;
        private final Expression value;

        /**
         * Creates a local variable assignment.
         *
         * @param src        the range of the assignment
         * @param identifier the declared identifier name of the variable
         * @param value      the expression matching the assigned value
         */
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

    /**
     * A return statement such as {@code return expression}.
     * Matches any of the typed return instructions.
     */
    public static class ReturnExpression extends ExpressionAst {
        private final Expression value;

        /**
         * Creates a return statement.
         *
         * @param src   the range of the {@code return} keyword
         * @param value the expression matching the returned value
         */
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

    /**
     * A throw statement such as {@code throw expression}.
     * Matches an {@code ATHROW} instruction.
     */
    public static class ThrowExpression extends ExpressionAst {
        private final Expression value;

        /**
         * Creates a throw statement.
         *
         * @param src   the range of the {@code throw} keyword
         * @param value the expression matching the thrown exception
         */
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


    /**
     * Matches the name of a field or method.
     * Used by member access nodes and method reference nodes; implementations include named and wildcard
     * matchers created by the parser.
     */
    public interface MemberName {
        /**
         * Tests if a value's field or method name matches.
         *
         * @param ctx  the context of the current match
         * @param node the value to match
         * @return whether the member name matches
         */
        boolean matches(ExpressionContext ctx, ExpressionFlow.FlowValue node);

        /**
         * Tests if a method handle name matches.
         *
         * @param ctx    the context of the current match
         * @param handle the method handle to match
         * @return whether the member name matches
         */
        boolean matches(ExpressionContext ctx, Handle handle);
    }

    /**
     * Matches a type used in the expression.
     * Implementations include named, wildcard and array type matchers created by the parser.
     */
    public interface TypeIdentifier {
        /**
         * Tests if a type matches.
         *
         * @param identifiers the identifiers used to resolve type names
         * @param type        the type to match
         * @return whether the type matches
         */
        boolean matches(ExpressionIdentifiers identifiers, Type type);
    }

}
