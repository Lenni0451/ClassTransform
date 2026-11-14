package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * The state of one expression match.
 */
@ParametersAreNonnullByDefault
public class ExpressionContext {

    private final ExpressionIdentifiers identifiers;
    private final ExpressionContextType type;
    private final boolean allowIncompleteListInputs;
    private final List<Capture> captures = new ArrayList<>();

    public ExpressionContext(final ExpressionIdentifiers identifiers, final ExpressionContextType type, final boolean allowIncompleteListInputs) {
        this.identifiers = identifiers;
        this.type = type;
        this.allowIncompleteListInputs = allowIncompleteListInputs;
    }

    public ExpressionIdentifiers getIdentifiers() {
        return this.identifiers;
    }

    public ExpressionContextType getType() {
        return this.type;
    }

    public boolean isAllowIncompleteListInputs() {
        return this.allowIncompleteListInputs;
    }

    public List<Capture> getCaptures() {
        return this.captures;
    }

    public void capture(final ExpressionFlow.FlowValue node, final Expression expression, @Nullable final Type valueType,
                        @Nullable final Type receiverType, final Type[] argumentTypes,
                        @Nullable final org.objectweb.asm.tree.JumpInsnNode jump, @Nullable final AbstractInsnNode comparison,
                        final boolean inverted) {
        if (node.isComplex()) return;
        this.captures.add(new Capture(node.getInsn(), valueType, receiverType, argumentTypes, jump, comparison, inverted));
    }

    public void reportMatchStatus(final ExpressionFlow.FlowValue node, final Expression expression, final boolean matched) {
    }

    public void reportPartialMatch(final ExpressionFlow.FlowValue node, final Expression expression) {
    }


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

        public AbstractInsnNode getInstruction() {
            return this.instruction;
        }

        @Nullable
        public Type getValueType() {
            return this.valueType;
        }

        @Nullable
        public Type getReceiverType() {
            return this.receiverType;
        }

        public Type[] getArgumentTypes() {
            return this.argumentTypes.clone();
        }

        @Nullable
        public org.objectweb.asm.tree.JumpInsnNode getJump() {
            return this.jump;
        }

        @Nullable
        public AbstractInsnNode getComparison() {
            return this.comparison;
        }

        public boolean isInverted() {
            return this.inverted;
        }
    }


    public enum ExpressionContextType {
        INJECT, REDIRECT, WRAP_OPERATION, MODIFY_EXPRESSION_VALUE, MODIFY_RECEIVER,
        MODIFY_RETURN_VALUE, MODIFY_CONSTANT, WRAP_WITH_CONDITION, SLICE, CUSTOM
    }

}
