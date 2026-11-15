package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A target for the head (before first instruction) of a method.
 * <p>
 * The target value, descriptor and slice are ignored. The target itself ignores the ordinal, but
 * the ordinal is applied afterwards by the target resolver, so an ordinal other than {@code -1}
 * or {@code 0} selects no instruction. The shift is always {@code BEFORE}; exactly one instruction
 * is returned.
 */
@ParametersAreNonnullByDefault
public class HeadTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        return Collections.singletonList(method.instructions.getFirst());
    }

    @Override
    public CTarget.Shift getShift(CTarget target) {
        return CTarget.Shift.BEFORE;
    }

}
