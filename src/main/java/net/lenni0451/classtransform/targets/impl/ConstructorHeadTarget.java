package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A target for the head of a constructor (after the {@code super()} or {@code this()} call).<br>
 * This target can only be used on constructors.
 * <p>
 * Constructor calls of objects allocated inside the prologue are skipped, so the selected
 * instruction is the one after the delegating call. If the delegating call is the last
 * instruction or no delegating call is found, the first instruction is returned. The shift is
 * always {@code BEFORE}. Using this target on a method that is not a constructor throws an
 * {@link IllegalArgumentException}.
 */
@ParametersAreNonnullByDefault
public class ConstructorHeadTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        AbstractInsnNode head = method.instructions.getFirst();
        if (!"<init>".equals(method.name)) throw new IllegalArgumentException("CTOR_HEAD can only be used on constructors");

        int allocations = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() == Opcodes.NEW) {
                allocations++;
            } else if (instruction.getOpcode() == Opcodes.INVOKESPECIAL && instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                if (!"<init>".equals(methodInsn.name)) continue;
                if (allocations > 0) {
                    allocations--;
                } else {
                    AbstractInsnNode postDelegate = instruction.getNext();
                    return Collections.singletonList(postDelegate == null ? head : postDelegate);
                }
            }
        }
        return Collections.singletonList(head);
    }

    @Override
    public CTarget.Shift getShift(CTarget target) {
        return CTarget.Shift.BEFORE;
    }

}
