package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A target for the tail (before last {@link Opcodes#RETURN}) of a method.<br>
 * When using {@link CInject} the original return value is accessible using the {@link InjectionCallback}.
 */
@ParametersAreNonnullByDefault
public class TailTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        for (int i = method.instructions.size() - 1; i >= 0; i--) {
            AbstractInsnNode instruction = method.instructions.get(i);
            if (instruction.getOpcode() >= Opcodes.IRETURN && instruction.getOpcode() <= Opcodes.RETURN) return Collections.singletonList(instruction);
        }
        return Collections.emptyList();
    }

    @Override
    public CTarget.Shift getShift(CTarget target) {
        return CTarget.Shift.BEFORE;
    }

}
