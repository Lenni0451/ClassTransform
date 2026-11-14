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
 * For regular methods this target behaves like {@code HEAD}.
 */
@ParametersAreNonnullByDefault
public class ConstructorHeadTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        AbstractInsnNode head = method.instructions.getFirst();
        if (!"<init>".equals(method.name)) return Collections.singletonList(head);

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
