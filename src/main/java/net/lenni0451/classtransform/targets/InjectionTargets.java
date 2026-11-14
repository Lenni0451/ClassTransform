package net.lenni0451.classtransform.targets;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.utils.CTargetImpl;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * Resolve injection points, opcode filters, ordinals and instruction offsets.
 */
@ParametersAreNonnullByDefault
public class InjectionTargets {

    /**
     * @param injectionTargets The registered injection point implementations
     * @param method           The target method
     * @param target           The selector, filters and offset
     * @param slice            The selected slice
     * @return The matching instructions in bytecode order
     */
    public static List<AbstractInsnNode> find(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CTarget target, @Nullable final CSlice slice) {
        IInjectionTarget injectionTarget = injectionTargets.get(target.value().toUpperCase(Locale.ROOT));
        if (injectionTarget == null) throw new IllegalArgumentException("Unknown injection target '" + target.value() + "'");
        CTarget unfiltered = CTargetImpl.of(target.value(), target.target(), target.shift(), -1, target.optional(), target.args(), target.desc());
        List<AbstractInsnNode> found = injectionTarget.getTargets(injectionTargets, method, unfiltered, slice);
        if (found == null) throw new IllegalArgumentException("Invalid target '" + target.target() + "'");
        List<AbstractInsnNode> result = new ArrayList<>();
        int ordinal = 0;
        for (AbstractInsnNode instruction : found) {
            if (target.opcode() != -1 && target.opcode() != instruction.getOpcode()) continue;
            if (target.ordinal() == -1 || target.ordinal() == ordinal) {
                if (target.shift() == CTarget.Shift.BY) {
                    int index = method.instructions.indexOf(instruction) + target.by();
                    if (index < 0 || index >= method.instructions.size()) throw new IllegalArgumentException("Injection point shift is outside the method");
                    instruction = method.instructions.get(index);
                }
                result.add(instruction);
            }
            ordinal++;
        }
        return result;
    }

    /**
     * @param injectionTargets The registered injection point implementations
     * @param method           The target method
     * @param target           The slice boundary selector
     * @return The instruction selected by ONE, FIRST or LAST
     */
    public static AbstractInsnNode boundary(final Map<String, IInjectionTarget> injectionTargets, final MethodNode method, final CTarget target) {
        List<AbstractInsnNode> found = find(injectionTargets, method, target, null);
        String selector = target.selector().toUpperCase(Locale.ROOT);
        if (!selector.equals("ONE") && !selector.equals("FIRST") && !selector.equals("LAST")) throw new IllegalArgumentException("Unknown slice boundary selector '" + selector + "'");
        if (found.isEmpty() || (selector.equals("ONE") && found.size() != 1)) {
            throw new IllegalArgumentException("Slice boundary '" + target.value() + "' matched " + found.size() + " instructions");
        }
        return found.get(selector.equals("LAST") ? found.size() - 1 : 0);
    }

}
