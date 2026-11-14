package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Information about a matched expression operation.
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

    public AbstractInsnNode getTarget() {
        return this.target;
    }

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
    public JumpInsnNode getJump() {
        return this.jump;
    }

    @Nullable
    public AbstractInsnNode getComparison() {
        return this.comparison;
    }

    public boolean isInverted() {
        return this.inverted;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    @Nullable
    public LabelNode getJumpLabel() {
        return this.jump == null ? null : this.jump.label;
    }

    public boolean isComparison() {
        return this.jump != null;
    }

}
