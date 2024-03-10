package net.lenni0451.classtransform.targets;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.exceptions.SliceException;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.mappings.dynamic.IDynamicRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The interface which is used to define a target for an injection.
 */
@ParametersAreNonnullByDefault
public interface IInjectionTarget extends IDynamicRemapper {

    /**
     * Get all matching target instructions.
     *
     * @param injectionTargets All existing injection targets
     * @param method           The method to search in
     * @param target           The {@link CTarget} annotation
     * @param slice            The {@link CSlice} annotation
     * @return The matching instructions
     */
    List<AbstractInsnNode> getTargets(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CTarget target, @Nullable final CSlice slice);

    /**
     * Get the shift for this target.<br>
     * By default, it is defined in the {@link CTarget} annotation.
     *
     * @param target The {@link CTarget} annotation
     * @return The shift
     */
    default CTarget.Shift getShift(final CTarget target) {
        return target.shift();
    }

    /**
     * Get all instructions in the given slice.
     *
     * @param injectionTargets All existing injection targets
     * @param method           The method to search in
     * @param slice            The {@link CSlice} annotation
     * @return The instructions in the slice
     * @throws IllegalArgumentException If the slice is invalid
     */
    default List<AbstractInsnNode> getSlice(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, @Nullable final CSlice slice) {
        if (slice == null) return Arrays.asList(method.instructions.toArray());

        int from;
        int to;
        if (slice.from().value().isEmpty()) {
            from = 0;
        } else {
            IInjectionTarget target = injectionTargets.get(slice.from().value());
            if (target == null) throw SliceException.unknown("from", slice.from().value());
            List<AbstractInsnNode> targets = target.getTargets(injectionTargets, method, slice.from(), null);
            if (targets.size() != 1) throw SliceException.count("From", slice.from().value(), targets.size());
            from = method.instructions.indexOf(targets.get(0));
        }
        if (slice.to().value().isEmpty()) {
            to = method.instructions.size();
        } else {
            IInjectionTarget target = injectionTargets.get(slice.to().value());
            if (target == null) throw SliceException.unknown("to", slice.to().value());
            List<AbstractInsnNode> targets = target.getTargets(injectionTargets, method, slice.to(), null);
            if (targets.size() != 1) throw SliceException.count("To", slice.to().value(), targets.size());
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

    @Nullable
    @Override
    default RemapType dynamicRemap(final AMapper mapper, final Class<?> annotation, final Map<String, Object> values, final Method remappedMethod, final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer) {
        return RemapType.MEMBER;
    }

}
