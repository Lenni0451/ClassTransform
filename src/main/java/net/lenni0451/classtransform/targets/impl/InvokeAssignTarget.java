package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Select the instruction after a value-returning invocation and its immediate local assignment.
 * <p>
 * Invocations returning {@code void} are skipped. If the next real instruction is a local store,
 * the instruction after the store is selected; otherwise the instruction after the invocation
 * itself is used. Calls without a following real instruction are ignored, and the shift is
 * always {@code BEFORE}.
 */
@ParametersAreNonnullByDefault
public class InvokeAssignTarget extends InvokeTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> calls = super.getTargets(injectionTargets, method, target, slice);
        if (calls == null) return null;
        List<AbstractInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode call : calls) {
            if (Type.getReturnType(((MethodInsnNode) call).desc).equals(Type.VOID_TYPE)) continue;
            AbstractInsnNode next = this.getNextInstruction(call);
            if (next != null && next.getOpcode() >= Opcodes.ISTORE && next.getOpcode() <= Opcodes.ASTORE) next = this.getNextInstruction(next);
            if (next != null) result.add(next);
        }
        return result;
    }

    @Nullable
    private AbstractInsnNode getNextInstruction(@Nullable final AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }

    @Override
    public CTarget.Shift getShift(CTarget target) {
        return CTarget.Shift.BEFORE;
    }

}
