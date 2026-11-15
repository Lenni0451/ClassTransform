package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Information about a matched expression operation.
 * One match is created for every captured instruction and describes which instruction to inject at and the types
 * involved. Condition jumps are replaced by a helper call, so matches do not carry the original jump.
 */
@ParametersAreNonnullByDefault
public class ExpressionMatch {

    private final AbstractInsnNode target;
    private final Type valueType;
    @Nullable
    private final Type receiverType;
    private final Type[] argumentTypes;
    @Nullable
    private final JumpInsnNode jump;
    @Nullable
    private final AbstractInsnNode comparison;
    private final boolean inverted;
    private final int start;
    private final int end;

    /**
     * Creates a match description.
     *
     * @param target        the instruction to inject at
     * @param valueType     the type of the value produced by the operation
     * @param receiverType  the receiver type, or null for static accesses
     * @param argumentTypes the argument types of the operation
     * @param jump          the jump of a condition match, or null
     * @param comparison    the compare instruction preceding the jump, or null
     * @param inverted      whether the condition is inverted
     * @param start         the inclusive start index of the matched instruction range
     * @param end           the inclusive end index of the matched instruction range
     */
    public ExpressionMatch(final AbstractInsnNode target, final Type valueType, @Nullable final Type receiverType, final Type[] argumentTypes,
                           @Nullable final JumpInsnNode jump, @Nullable final AbstractInsnNode comparison, final boolean inverted,
                           final int start, final int end) {
        this.target = target;
        this.valueType = valueType;
        this.receiverType = receiverType;
        this.argumentTypes = argumentTypes;
        this.jump = jump;
        this.comparison = comparison;
        this.inverted = inverted;
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the instruction to inject at.
     *
     * @return the target instruction
     */
    public AbstractInsnNode getTarget() {
        return this.target;
    }

    /**
     * Returns the type of the value produced by the matched operation.
     *
     * @return the value type, {@code void} for operations without a result
     */
    public Type getValueType() {
        return this.valueType;
    }

    /**
     * Returns the type of the receiver of the matched operation.
     *
     * @return the receiver type, or null for static accesses
     */
    @Nullable
    public Type getReceiverType() {
        return this.receiverType;
    }

    /**
     * Returns the argument types of the matched operation.
     *
     * @return a copy of the argument types
     */
    public Type[] getArgumentTypes() {
        return this.argumentTypes.clone();
    }

    /**
     * Returns the jump instruction of a matched condition.
     * <p>
     * The field is currently always null because {@link ExpressionEngine} replaces condition jumps with a helper
     * call before creating the match.
     *
     * @return the jump instruction, or null if no jump was recorded
     */
    @Nullable
    public JumpInsnNode getJump() {
        return this.jump;
    }

    /**
     * Returns the compare instruction preceding the jump of a condition.
     * <p>
     * Like {@link #getJump()}, this is currently always null for matches created by {@link ExpressionEngine}.
     *
     * @return the comparison instruction, or null for direct comparisons
     */
    @Nullable
    public AbstractInsnNode getComparison() {
        return this.comparison;
    }

    /**
     * Returns whether the matched condition is inverted.
     *
     * @return whether the jump condition has to be inverted
     */
    public boolean isInverted() {
        return this.inverted;
    }

    /**
     * Returns the inclusive index of the first matched instruction.
     *
     * @return the start instruction index in the target method
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Returns the inclusive index of the last matched instruction.
     *
     * @return the end instruction index in the target method
     */
    public int getEnd() {
        return this.end;
    }

    /**
     * Returns the jump label of a matched condition.
     *
     * @return the label the captured jump points to, or null if the match is not a condition
     */
    @Nullable
    public LabelNode getJumpLabel() {
        return this.jump == null ? null : this.jump.label;
    }

    /**
     * Returns whether this match captured a condition jump.
     *
     * @return whether {@link #getJump()} is non-null
     */
    public boolean isComparison() {
        return this.jump != null;
    }

}
