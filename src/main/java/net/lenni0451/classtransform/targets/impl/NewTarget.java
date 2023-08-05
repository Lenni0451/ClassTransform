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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * A target for {@link Opcodes#NEW} instructions.<br>
 * e.g. {@code java/lang/String}
 */
@ParametersAreNonnullByDefault
public class NewTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        int i = 0;
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (instruction.getOpcode() != Opcodes.INVOKESPECIAL) continue;
            MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
            if (!this.isTarget(methodInsnNode.owner, target.target())) continue;
            if (target.ordinal() == -1 || target.ordinal() == i) targets.add(instruction);
            i++;
        }
        return targets;
    }

    private boolean isTarget(final String owner, String target) {
        if (target.startsWith("L") && target.endsWith(";")) target = target.substring(1, target.length() - 1);
        return owner.equals(slash(target));
    }

}
