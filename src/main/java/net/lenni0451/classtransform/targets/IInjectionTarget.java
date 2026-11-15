package net.lenni0451.classtransform.targets;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
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
 * <p>
 * Implementations scan a method for instructions that match the selector in {@link CTarget#target()}
 * and the optional descriptor in {@link CTarget#desc()}, optionally restricted to a {@link CSlice}.
 * They are registered by name in the {@code TransformerManager} and looked up through {@link InjectionTargets}.
 */
@ParametersAreNonnullByDefault
public interface IInjectionTarget extends IDynamicRemapper {

    /**
     * Get all matching target instructions.
     * <p>
     * Implementations normally honor {@link CTarget#target()}, {@link CTarget#desc()},
     * {@link CTarget#args()} and {@link CTarget#ordinal()}. The opcode filter and the
     * {@link CTarget.Shift#BY} offset are applied afterwards by {@link InjectionTargets#find}.
     * Returns the matches in bytecode order and should return an empty list when nothing matches.
     * The returned list is owned by the caller; the implementation must not reuse it.
     *
     * @param injectionTargets All existing injection targets
     * @param method           The method to search in
     * @param target           The {@link CTarget} annotation
     * @param slice            The {@link CSlice} annotation, or {@code null} for the whole method
     * @return The matching instructions, or {@code null} if the selector is invalid
     */
    List<AbstractInsnNode> getTargets(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CTarget target, @Nullable final CSlice slice);

    /**
     * Get the shift for this target.<br>
     * By default, it is defined in the {@link CTarget} annotation.
     * Targets that only support one placement override this, for example to always inject {@code BEFORE}.
     *
     * @param target The {@link CTarget} annotation
     * @return The shift
     */
    default CTarget.Shift getShift(final CTarget target) {
        return target.shift();
    }

    /**
     * Get all instructions in the given slice.
     * <p>
     * The slice boundaries are resolved with {@link InjectionTargets#boundary} and both ends are inclusive.
     * Empty {@link CSlice#from()} or {@link CSlice#to()} values select the start or the end of the method.
     * A {@code null} slice returns all instructions of the method as a fixed-size list.
     *
     * @param injectionTargets All existing injection targets
     * @param method           The method to search in
     * @param slice            The {@link CSlice} annotation, or {@code null} for the whole method
     * @return The instructions in the slice in bytecode order
     * @throws IllegalArgumentException If the slice is invalid
     */
    default List<AbstractInsnNode> getSlice(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, @Nullable final CSlice slice) {
        if (slice == null) return Arrays.asList(method.instructions.toArray());

        int from;
        int to;
        if (slice.from().value().isEmpty()) {
            from = 0;
        } else {
            from = method.instructions.indexOf(InjectionTargets.boundary(injectionTargets, method, slice.from()));
        }
        if (slice.to().value().isEmpty()) {
            to = method.instructions.size();
        } else {
            to = method.instructions.indexOf(InjectionTargets.boundary(injectionTargets, method, slice.to()));
        }
        if (from > to) throw new IllegalArgumentException("Slice start is after its end");

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

    /**
     * Remap the target selector of this injection target.
     * <p>
     * The default implementation treats the selector as a member declaration.
     * Implementations override this to remap classes or descriptors, or return {@code null} to skip remapping.
     *
     * @param mapper             The mapper instance
     * @param annotation         The annotation class
     * @param values             The raw values of the annotation
     * @param remappedMethod     The annotation method that is being remapped
     * @param transformerManager The transformer manager
     * @param target             The transformer target class
     * @param transformer        The transformer class
     * @return The new remap type, or {@code null} to skip the default remapper
     */
    @Nullable
    @Override
    default RemapType dynamicRemap(final AMapper mapper, final Class<?> annotation, final Map<String, Object> values, final Method remappedMethod, final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer) {
        return RemapType.MEMBER;
    }

}
