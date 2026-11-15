package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * The state of one expression match.
 * A context is created per value and expression, provides access to the {@link ExpressionIdentifiers}, and
 * collects the {@link Capture}s recorded by {@link Expression#capture(ExpressionFlow.FlowValue, ExpressionContext)}.
 * The engine creates a fresh context for every value it tests.
 */
@ParametersAreNonnullByDefault
public class ExpressionContext {

    private final ExpressionIdentifiers identifiers;
    private final ExpressionContextType type;
    private final boolean allowIncompleteListInputs;
    private final List<Capture> captures = new ArrayList<>();

    /**
     * Creates a match context.
     *
     * @param identifiers               the identifiers defined for the transformer method
     * @param type                      the kind of injection the expression is used by
     * @param allowIncompleteListInputs whether patterns may match values with fewer inputs than listed
     */
    public ExpressionContext(final ExpressionIdentifiers identifiers, final ExpressionContextType type, final boolean allowIncompleteListInputs) {
        this.identifiers = identifiers;
        this.type = type;
        this.allowIncompleteListInputs = allowIncompleteListInputs;
    }

    /**
     * Returns the identifiers available to the expression.
     *
     * @return the identifier table of the current match
     */
    public ExpressionIdentifiers getIdentifiers() {
        return this.identifiers;
    }

    /**
     * Returns the kind of injection the expression is matched for.
     * <p>
     * The value is diagnostic metadata; the built-in AST nodes do not change their matching based on it.
     *
     * @return the context type
     */
    public ExpressionContextType getType() {
        return this.type;
    }

    /**
     * Returns whether patterns may match values with fewer inputs than they list.
     * <p>
     * The flag is only stored and exposed here; the built-in AST nodes do not consult it.
     *
     * @return whether incomplete input lists are allowed
     */
    public boolean isAllowIncompleteListInputs() {
        return this.allowIncompleteListInputs;
    }

    /**
     * Returns the captures recorded so far.
     * <p>
     * The returned list is the live internal list; captures are appended by
     * {@link #capture(ExpressionFlow.FlowValue, Expression, Type, Type, Type[], org.objectweb.asm.tree.JumpInsnNode, AbstractInsnNode, boolean)}.
     *
     * @return the captures of this match
     */
    public List<Capture> getCaptures() {
        return this.captures;
    }

    /**
     * Records a capture of a matched instruction.
     * <p>
     * Complex values are ignored because they have no instruction to capture. A null value type is allowed and
     * later treated as {@code void} by {@link ExpressionEngine}.
     *
     * @param node         the matched value
     * @param expression   the expression which produced the capture
     * @param valueType    the type of the produced value, or null if unknown
     * @param receiverType the type of the receiver, or null for static accesses
     * @param argumentTypes the argument types of the matched operation
     * @param jump         the captured jump instruction of a comparison, or null
     * @param comparison   the compare instruction ({@code LCMP}, {@code FCMPL}, ...) preceding the jump, or null
     * @param inverted     whether the comparison was matched in its inverted form
     */
    public void capture(final ExpressionFlow.FlowValue node, final Expression expression, @Nullable final Type valueType,
                        @Nullable final Type receiverType, final Type[] argumentTypes,
                        @Nullable final org.objectweb.asm.tree.JumpInsnNode jump, @Nullable final AbstractInsnNode comparison,
                        final boolean inverted) {
        if (node.isComplex()) return;
        this.captures.add(new Capture(node.getInsn(), valueType, receiverType, argumentTypes, jump, comparison, inverted));
    }

    /**
     * Extension hook for reporting the result of a completed match attempt.
     * <p>
     * Called with the final outcome after all inputs of a node have been compared. The default implementation
     * does nothing, and nothing in this package calls the hook.
     *
     * @param node       the value that was matched
     * @param expression the expression that was matched
     * @param matched    whether the match succeeded
     */
    public void reportMatchStatus(final ExpressionFlow.FlowValue node, final Expression expression, final boolean matched) {
    }

    /**
     * Extension hook for reporting a partial match before the final result is known.
     * <p>
     * Intended for contexts that inspect how far a failed match progressed. The default implementation does
     * nothing, and nothing in this package calls the hook.
     *
     * @param node       the value that was partially matched
     * @param expression the expression that was being matched
     */
    public void reportPartialMatch(final ExpressionFlow.FlowValue node, final Expression expression) {
    }


    /**
     * One instruction captured by a matched expression, together with the operation details needed to build an
     * {@link ExpressionMatch}.
     */
    public static class Capture {
        private final AbstractInsnNode instruction;
        private final Type valueType;
        @Nullable
        private final Type receiverType;
        private final Type[] argumentTypes;
        @Nullable
        private final org.objectweb.asm.tree.JumpInsnNode jump;
        @Nullable
        private final AbstractInsnNode comparison;
        private final boolean inverted;

        private Capture(final AbstractInsnNode instruction, @Nullable final Type valueType, @Nullable final Type receiverType, final Type[] argumentTypes,
                        @Nullable final org.objectweb.asm.tree.JumpInsnNode jump, @Nullable final AbstractInsnNode comparison, final boolean inverted) {
            this.instruction = instruction;
            this.valueType = valueType;
            this.receiverType = receiverType;
            this.argumentTypes = argumentTypes;
            this.jump = jump;
            this.comparison = comparison;
            this.inverted = inverted;
        }

        /**
         * Returns the captured instruction.
         *
         * @return the matched instruction
         */
        public AbstractInsnNode getInstruction() {
            return this.instruction;
        }

        /**
         * Returns the type of the value produced by the captured instruction.
         *
         * @return the value type, or null if unknown
         */
        @Nullable
        public Type getValueType() {
            return this.valueType;
        }

        /**
         * Returns the type of the receiver of the captured operation.
         *
         * @return the receiver type, or null for static accesses
         */
        @Nullable
        public Type getReceiverType() {
            return this.receiverType;
        }

        /**
         * Returns the argument types of the captured operation.
         *
         * @return a copy of the argument types
         */
        public Type[] getArgumentTypes() {
            return this.argumentTypes.clone();
        }

        /**
         * Returns the jump instruction of a captured comparison.
         *
         * @return the jump instruction, or null if the capture is not a comparison
         */
        @Nullable
        public org.objectweb.asm.tree.JumpInsnNode getJump() {
            return this.jump;
        }

        /**
         * Returns the compare instruction preceding the captured jump.
         *
         * @return the comparison instruction, or null for direct comparisons
         */
        @Nullable
        public AbstractInsnNode getComparison() {
            return this.comparison;
        }

        /**
         * Returns whether the comparison was matched in inverted form.
         *
         * @return whether the jump condition has to be inverted
         */
        public boolean isInverted() {
            return this.inverted;
        }
    }


    /**
     * The injection kinds an expression can be matched for.
     * The type is derived from the annotation on the transformer method and serves as diagnostic metadata; the
     * built-in expressions and captures do not adapt their behaviour based on it.
     */
    public enum ExpressionContextType {
        /**
         * An expression used by an {@code @CInject} injection.
         */
        INJECT,
        /**
         * An expression used by an {@code @CRedirect} injection.
         */
        REDIRECT,
        /**
         * An expression used by a {@code @CWrapOperation} injection.
         */
        WRAP_OPERATION,
        /**
         * An expression used by a {@code @CModifyExpressionValue} injection.
         */
        MODIFY_EXPRESSION_VALUE,
        /**
         * An expression used by a {@code @CModifyReceiver} injection.
         */
        MODIFY_RECEIVER,
        /**
         * An expression used by a {@code @CModifyReturnValue} injection.
         */
        MODIFY_RETURN_VALUE,
        /**
         * An expression used by a {@code @CModifyConstant} injection.
         */
        MODIFY_CONSTANT,
        /**
         * An expression used by a {@code @CWrapCondition} injection.
         */
        WRAP_WITH_CONDITION,
        /**
         * A context for slice expressions; never produced by {@link ExpressionEngine}.
         */
        SLICE,
        /**
         * A context for expressions used outside a known injection annotation.
         */
        CUSTOM
    }

}
