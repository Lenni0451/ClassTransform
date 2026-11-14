package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.InjectionTargets;
import net.lenni0451.classtransform.utils.InjectionSettings;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

/**
 * Prepared injection points for one handler method.<br>
 * Plans for a class share instruction identities so late injectors can follow replacements.<br>
 * Plans are sorted by phase and order. Stable ties preserve transformer registration and method declaration order.
 */
@ParametersAreNonnullByDefault
public class InjectionPlan {

    private static final ThreadLocal<InjectionPlan> ACTIVE = new ThreadLocal<>();

    /**
     * @param manager  The transformer manager
     * @param method   The target method
     * @param target   The prepared selector
     * @param fallback The slice to use outside an active plan
     * @return The current instructions corresponding to the prepared selection
     */
    public static List<AbstractInsnNode> select(final TransformerManager manager, final MethodNode method, final CTarget target, @Nullable final CSlice fallback) {
        InjectionPlan plan = ACTIVE.get();
        if (plan == null) return InjectionTargets.find(manager.getInjectionTargets(), method, target, fallback);
        Map<List<Object>, List<Target>> selections = plan.selections.get(method);
        if (selections == null || !selections.containsKey(key(target))) throw new IllegalStateException("Injection target was not prepared");
        List<AbstractInsnNode> result = new ArrayList<>();
        for (Target selected : selections.get(key(target))) {
            if (!method.instructions.contains(selected.current)) throw new IllegalStateException("Injection target was removed by another injector");
            result.add(selected.current);
        }
        return result;
    }

    /**
     * @param count The number of successful calls to add to the active plan
     */
    public static void record(final int count) {
        InjectionPlan plan = ACTIVE.get();
        if (plan != null) plan.count += count;
    }

    /**
     * Update the shared operation identity after a redirect or constant modification.
     *
     * @param original    The previously selected instruction
     * @param replacement The replacement operation
     */
    public static void replace(final AbstractInsnNode original, final AbstractInsnNode replacement) {
        InjectionPlan plan = ACTIVE.get();
        if (plan == null) return;
        Target target = plan.registry.get(original);
        if (target != null) {
            target.current = replacement;
            target.after = replacement;
            plan.registry.put(replacement, target);
        }
    }

    /**
     * @param instruction The current operation
     * @return The insertion anchor after existing modifiers and required casts
     */
    public static AbstractInsnNode after(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? instruction : target.after;
    }

    /**
     * @param instruction The current operation
     * @return The operation selected before any replacements
     */
    public static AbstractInsnNode original(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? instruction : target.original;
    }

    /**
     * @param instruction The current operation
     * @param after       The new insertion anchor for subsequent modifiers
     */
    public static void setAfter(final AbstractInsnNode instruction, final AbstractInsnNode after) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        if (target != null) target.after = after;
    }

    private static List<Object> key(final CTarget target) {
        return Arrays.asList(target.value(), target.target(), target.ordinal(), target.opcode(), target.slice(), target.shift(), target.by(), Arrays.asList(target.args()));
    }


    private final TransformerManager manager;
    private final InjectionSettings settings;
    private final ClassNode transformer;
    private final MethodNode method;
    private final Map<MethodNode, Map<List<Object>, List<Target>>> selections = new LinkedHashMap<>();
    private final Map<AbstractInsnNode, Target> registry;
    private final Consumer<MethodNode> injection;
    private int count;

    /**
     * @param manager          The transformer manager
     * @param settings         The shared injection settings
     * @param transformer      The transformer class
     * @param method           The handler method
     * @param targets          The selected target methods
     * @param injectionTargets The instruction selectors
     * @param registry         The class-wide instruction registry
     * @param injection        The action to apply to each matched method
     */
    public InjectionPlan(final TransformerManager manager, final InjectionSettings settings, final ClassNode transformer, final MethodNode method, final Collection<MethodNode> targets, final List<CTarget> injectionTargets, final Map<AbstractInsnNode, Target> registry, final Consumer<MethodNode> injection) {
        this.manager = manager;
        this.settings = settings;
        this.transformer = transformer;
        this.method = method;
        this.registry = registry;
        this.injection = injection;
        if (injectionTargets.isEmpty()) throw new TransformerException(method, transformer, "does not have any injection targets");
        for (MethodNode target : targets) {
            Map<List<Object>, List<Target>> selections = new LinkedHashMap<>();
            for (CTarget injectionTarget : injectionTargets) {
                List<AbstractInsnNode> found;
                try {
                    found = InjectionTargets.find(manager.getInjectionTargets(), target, injectionTarget, settings.getSlice(injectionTarget.slice()));
                } catch (RuntimeException e) {
                    throw new TransformerException(method, transformer, "invalid injection target").targetMethod(target).cause(e);
                }
                if (found.isEmpty() && !injectionTarget.optional() && !settings.hasExplicitRequire()) {
                    throw new TransformerException(method, transformer, "target '" + injectionTarget.value() + "' could not be found").targetMethod(target);
                }
                List<Target> selected = new ArrayList<>();
                for (AbstractInsnNode instruction : found) selected.add(registry.computeIfAbsent(instruction, Target::new));
                selections.put(key(injectionTarget), selected);
            }
            this.selections.put(target, selections);
        }
    }

    /**
     * @return The injection phase
     */
    public InjectionPhase getPhase() {
        return this.settings.getPhase();
    }

    /**
     * @return The injection order within the phase
     */
    public int getOrder() {
        return this.settings.getOrder();
    }

    /**
     * Apply the prepared injections and validate their aggregate count.
     */
    public void apply() {
        InjectionPlan previous = ACTIVE.get();
        ACTIVE.set(this);
        try {
            for (Map.Entry<MethodNode, Map<List<Object>, List<Target>>> entry : this.selections.entrySet()) {
                if (entry.getValue().values().stream().anyMatch(points -> !points.isEmpty())) this.injection.accept(entry.getKey());
            }
            this.settings.validate(this.manager, this.transformer, this.method, this.count);
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }


    /**
     * The original and current identities of a selected operation.
     */
    public static class Target {
        private final AbstractInsnNode original;
        private AbstractInsnNode current;
        private AbstractInsnNode after;

        private Target(final AbstractInsnNode instruction) {
            this.original = instruction;
            this.current = instruction;
            this.after = instruction;
        }
    }

}
