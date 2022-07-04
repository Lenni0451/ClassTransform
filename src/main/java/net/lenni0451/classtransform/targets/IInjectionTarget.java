package net.lenni0451.classtransform.targets;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public interface IInjectionTarget {

    List<AbstractInsnNode> getTargets(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CTarget target, final CSlice slice);

    default CTarget.Shift getShift(final CTarget target) {
        return target.shift();
    }

    default List<AbstractInsnNode> getSlice(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CSlice slice) {
        if (slice == null) return Arrays.asList(method.instructions.toArray());

        int from;
        int to;
        if (slice.from().value().isEmpty()) {
            from = 0;
        } else {
            IInjectionTarget target = injectionTargets.get(slice.from().value());
            if (target == null) throw new IllegalArgumentException("Unknown from target in slice: " + slice.from().value());
            List<AbstractInsnNode> targets = target.getTargets(injectionTargets, method, slice.from(), null);
            if (targets.size() != 1) throw new IllegalArgumentException("From target in slice has more than one match: " + slice.from().value());
            from = method.instructions.indexOf(targets.get(0));
        }
        if (slice.to().value().isEmpty()) {
            to = method.instructions.size();
        } else {
            IInjectionTarget target = injectionTargets.get(slice.to().value());
            if (target == null) throw new IllegalArgumentException("Unknown to target in slice: " + slice.to().value());
            List<AbstractInsnNode> targets = target.getTargets(injectionTargets, method, slice.to(), null);
            if (targets.size() != 1) throw new IllegalArgumentException("To target in slice has more than one match: " + slice.to().value());
            to = method.instructions.indexOf(targets.get(0));
        }

        List<AbstractInsnNode> instructions = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) instructions.add(instruction);
        Iterator<AbstractInsnNode> it = instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode instruction = it.next();
            int index = method.instructions.indexOf(instruction);
            if (index < from || index > to) it.remove();
        }

        return instructions;
    }

}
