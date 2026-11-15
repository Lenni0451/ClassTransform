package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A target for the given opcode.
 * <p>
 * The opcode can be specified as a number or as its name, e.g. {@code INVOKESTATIC} or {@code 184}.
 * The name is resolved against the {@link Opcodes} fields and is case-insensitive. An unknown
 * opcode makes the target return {@code null}, which is reported as an invalid target by
 * {@link net.lenni0451.classtransform.targets.InjectionTargets#find}. The ordinal selects among
 * the instructions with that opcode in bytecode order.
 */
@ParametersAreNonnullByDefault
public class OpcodeTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        int opcode = this.getOpcode(target.target());
        if (opcode == -1) return null;

        int i = 0;
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (instruction.getOpcode() != opcode) continue;
            if (target.ordinal() == -1 || target.ordinal() == i) targets.add(instruction);
            i++;
        }
        return targets;
    }

    private int getOpcode(final String opcode) {
        try {
            return Integer.parseInt(opcode);
        } catch (Throwable ignored) {
        }
        try {
            return Opcodes.class.getDeclaredField(opcode.toUpperCase(Locale.ROOT)).getInt(null);
        } catch (Throwable ignored) {
        }
        return -1;
    }

}
