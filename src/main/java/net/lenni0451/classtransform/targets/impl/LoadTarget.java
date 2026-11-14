package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The injection target for variable loads used by {@code @CModifyVariable}.
 */
@ParametersAreNonnullByDefault
public class LoadTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CTarget target, @Nullable final CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (!(instruction instanceof VarInsnNode)) continue;
            int opcode = instruction.getOpcode();
            if (opcode < Opcodes.ILOAD || opcode > Opcodes.ALOAD) continue;
            targets.add(instruction);
        }
        return targets;
    }

}
