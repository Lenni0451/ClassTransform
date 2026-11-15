package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CGroup;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.expression.ExpressionMatch;
import net.lenni0451.classtransform.targets.InjectionTargets;
import net.lenni0451.classtransform.targets.impl.ExpressionTarget;
import net.lenni0451.classtransform.utils.InjectionSettings;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
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
     * Resolve the current instructions for a prepared target selector.
     * <p>
     * Outside an active plan the selector is evaluated directly against the method. Inside a plan the prepared
     * selection is used so that later injectors see the instructions left by earlier ones.
     *
     * @param manager  The transformer manager
     * @param method   The target method
     * @param target   The prepared selector
     * @param fallback The slice to use outside an active plan
     * @return The current instructions corresponding to the prepared selection
     * @throws IllegalStateException If no selection was prepared for the method and selector, or a selected
     *                               instruction was removed by another injector
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
     * Record successful injections of the active plan.
     * <p>
     * Has no effect when no plan is active.
     *
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
     * Get the operation context attached to the given instruction.
     *
     * @param instruction The instruction
     * @return The context or null
     */
    @Nullable
    public static Object getContext(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? null : target.context;
    }

    /**
     * Attach a context to the given instruction.
     *
     * @param instruction The instruction
     * @param context     The context
     */
    public static void setContext(final AbstractInsnNode instruction, final Object context) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        if (target != null) target.context = context;
    }

    /**
     * Get the operation wrapper context attached to the given instruction.
     *
     * @param instruction The instruction
     * @return The context or null
     */
    @Nullable
    public static Object getOperationContext(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? null : target.operationContext;
    }

    /**
     * Attach an operation wrapper context to the given instruction.
     *
     * @param instruction The instruction
     * @param context     The context
     */
    public static void setOperationContext(final AbstractInsnNode instruction, final Object context) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        if (target != null) target.operationContext = context;
    }

    /**
     * Get the expression match attached to the given instruction.
     *
     * @param instruction The instruction
     * @return The expression match or null
     */
    @Nullable
    public static ExpressionMatch getExpressionMatch(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? null : target.expressionMatch;
    }

    /**
     * Get the insertion anchor for the given operation.
     * <p>
     * The anchor is the instruction after all modifiers and required casts that were inserted for the operation.
     *
     * @param instruction The current operation
     * @return The insertion anchor after existing modifiers and required casts
     */
    public static AbstractInsnNode after(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? instruction : target.after;
    }

    /**
     * Get the operation selected before any replacements.
     *
     * @param instruction The current operation
     * @return The operation selected before any replacements
     */
    public static AbstractInsnNode original(final AbstractInsnNode instruction) {
        InjectionPlan plan = ACTIVE.get();
        Target target = plan == null ? null : plan.registry.get(instruction);
        return target == null ? instruction : target.original;
    }

    /**
     * Set the insertion anchor used by subsequent modifiers.
     *
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
    @Nullable
    private final String groupName;
    private final int groupMin;
    private final int groupMax;
    private final Map<MethodNode, Map<List<Object>, List<Target>>> selections = new LinkedHashMap<>();
    private final Map<AbstractInsnNode, Target> registry;
    private final Consumer<MethodNode> injection;
    private int count;

    /**
     * Prepare the injection selections for one handler method.
     * <p>
     * All target methods and instruction selectors are resolved immediately, so missing targets fail before any
     * instructions are changed. Selected instructions are shared through the class-wide registry so later plans can
     * follow replacements.
     *
     * @param manager          The transformer manager
     * @param targetClass      The transformed class
     * @param settings         The shared injection settings
     * @param transformer      The transformer class
     * @param method           The handler method
     * @param targets          The selected target methods
     * @param injectionTargets The instruction selectors
     * @param registry         The class-wide instruction registry
     * @param injection        The action to apply to each matched method
     * @throws IllegalArgumentException If a group bound is invalid
     * @throws TransformerException     If no injection target is defined or a mandatory target cannot be found
     */
    public InjectionPlan(final TransformerManager manager, final ClassNode targetClass, final InjectionSettings settings, final ClassNode transformer, final MethodNode method, final Collection<MethodNode> targets, final List<CTarget> injectionTargets, final Map<AbstractInsnNode, Target> registry, final Consumer<MethodNode> injection) {
        this.manager = manager;
        this.settings = settings;
        this.transformer = transformer;
        this.method = method;
        this.registry = registry;
        this.injection = injection;
        Optional<AnnotationNode> groupAnnotation = AnnotationUtils.findAnnotation(method, CGroup.class);
        if (groupAnnotation.isPresent()) {
            CGroup group = AnnotationParser.parse(CGroup.class, manager.getClassTree(), manager.getClassProvider(), AnnotationUtils.listToMap(groupAnnotation.get().values));
            if (group.min() != -1 && group.min() < 1) throw new IllegalArgumentException("Group min must be at least 1");
            if (group.max() != -1 && group.max() < 1) throw new IllegalArgumentException("Group max must be at least 1");
            this.groupName = group.name().isEmpty() ? "default" : group.name();
            this.groupMin = group.min();
            this.groupMax = group.max();
        } else {
            this.groupName = null;
            this.groupMin = -1;
            this.groupMax = -1;
        }
        if (injectionTargets.isEmpty()) throw new TransformerException(method, transformer, "does not have any injection targets");
        for (MethodNode target : targets) {
            Map<List<Object>, List<Target>> selections = new LinkedHashMap<>();
            ExpressionTarget.BuildContext expressionContext = new ExpressionTarget.BuildContext(this.manager, targetClass, method);
            ExpressionTarget.setContext(expressionContext);
            try {
                for (CTarget injectionTarget : injectionTargets) {
                    List<AbstractInsnNode> found;
                    try {
                        found = InjectionTargets.find(manager.getInjectionTargets(), target, injectionTarget, settings.getSlice(injectionTarget.slice()));
                    } catch (RuntimeException e) {
                        throw new TransformerException(method, transformer, "invalid injection target").targetMethod(target).cause(e);
                    }
                    if (found.isEmpty() && !injectionTarget.optional() && !settings.hasExplicitRequire()) {
                        String description = injectionTarget.id().isEmpty() ? injectionTarget.value() : injectionTarget.value() + " '" + injectionTarget.id() + "'";
                        throw new TransformerException(method, transformer, "target '" + description + "' could not be found").targetMethod(target);
                    }
                    List<Target> selected = new ArrayList<>();
                    for (AbstractInsnNode instruction : found) {
                        Target selectedTarget = registry.computeIfAbsent(instruction, Target::new);
                        ExpressionMatch expressionMatch = expressionContext.getMatches().get(instruction);
                        if (expressionMatch != null) selectedTarget.expressionMatch = expressionMatch;
                        selected.add(selectedTarget);
                    }
                    selections.put(key(injectionTarget), selected);
                }
            } finally {
                ExpressionTarget.clearContext();
            }
            this.selections.put(target, selections);
        }
    }

    /**
     * Validate the aggregated injection counts of all groups.
     *
     * @param plans The applied plans
     */
    public static void validateGroups(final Collection<InjectionPlan> plans) {
        Map<String, GroupValidation> groups = new LinkedHashMap<>();
        for (InjectionPlan plan : plans) {
            if (plan.groupName == null) continue;
            GroupValidation group = groups.computeIfAbsent(plan.groupName, name -> new GroupValidation(plan.transformer, plan.method));
            if (plan.groupMin > 0) group.min = Math.max(group.min, plan.groupMin);
            if (plan.groupMax > 0) group.max = group.max < 0 ? plan.groupMax : Math.min(group.max, plan.groupMax);
            group.total += plan.count;
        }
        for (Map.Entry<String, GroupValidation> entry : groups.entrySet()) {
            GroupValidation group = entry.getValue();
            int min = group.min > 0 ? group.min : 1;
            int max = group.max > 0 ? group.max : Integer.MAX_VALUE;
            if (group.total < min) {
                throw new TransformerException(group.method, group.transformer, "group '" + entry.getKey() + "' required at least " + min + " injections but performed " + group.total);
            }
            if (group.total > max) {
                throw new TransformerException(group.method, group.transformer, "group '" + entry.getKey() + "' allowed at most " + max + " injections but performed " + group.total);
            }
        }
    }

    /**
     * Get the phase this plan is applied in.
     *
     * @return The injection phase
     */
    public InjectionPhase getPhase() {
        return this.settings.getPhase();
    }

    /**
     * Get the application order within the phase.
     *
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
            this.settings.validate(this.manager, this.transformer, this.method, this.count, this.groupName == null);
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }


    private static class GroupValidation {
        private final ClassNode transformer;
        private final MethodNode method;
        private int min = -1;
        private int max = -1;
        private int total;

        private GroupValidation(final ClassNode transformer, final MethodNode method) {
            this.transformer = transformer;
            this.method = method;
        }
    }


    /**
     * The original and current identities of a selected operation.
     */
    public static class Target {
        private final AbstractInsnNode original;
        private AbstractInsnNode current;
        private AbstractInsnNode after;
        @Nullable
        private Object context;
        @Nullable
        private Object operationContext;
        @Nullable
        private ExpressionMatch expressionMatch;

        private Target(final AbstractInsnNode instruction) {
            this.original = instruction;
            this.current = instruction;
            this.after = instruction;
        }
    }

}
