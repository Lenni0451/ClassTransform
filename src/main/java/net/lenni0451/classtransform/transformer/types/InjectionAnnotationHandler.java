package net.lenni0451.classtransform.transformer.types;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CDescResolver;
import net.lenni0451.classtransform.utils.InjectionSettings;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

/**
 * Base for injections which share selection, count validation and ordering.
 * <p>
 * The manager batches consecutive injection handlers: it first asks every handler whether it {@link #handles} a
 * transformer method and collects the prepared plans, then applies them sorted by
 * {@link net.lenni0451.classtransform.transformer.InjectionPhase} and order.
 * Consequently {@link #prepare(TransformerManager, ClassNode, ClassNode, MethodNode, Map)} only resolves target methods
 * and instruction selections, while the inherited immediate transform applies a single plan right away.
 * <p>
 * The handler method is removed from the transformer before it is prepared. Every matched target method is processed
 * with a clone of the handler method, and failures are wrapped in a {@link TransformerException} carrying the target
 * class and method.
 *
 * @param <T> The injection annotation
 */
public abstract class InjectionAnnotationHandler<T extends Annotation> extends RemovingTargetAnnotationHandler<T> {

    private final Function<T, String[]> methodTargets;

    /**
     * Create a new injection handler.
     *
     * @param annotationClass The injection annotation
     * @param methodTargets   Resolves the target method selector combinations from the annotation
     */
    public InjectionAnnotationHandler(final Class<T> annotationClass, final Function<T, String[]> methodTargets) {
        super(annotationClass, methodTargets);
        this.methodTargets = methodTargets;
    }

    /**
     * Check whether this handler should prepare the given method.
     *
     * @param method  The transformer method
     * @param manager The transformer manager
     * @return If the method has an enabled injection annotation
     */
    @SuppressWarnings("unchecked")
    public boolean handles(final MethodNode method, final TransformerManager manager) {
        T annotation = (T) this.getAnnotation(this.getAnnotationClass(), method, manager);
        return annotation != null && this.shouldExecute(annotation);
    }

    @Override
    public final void transform(T annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        this.prepare(annotation, transformerManager, transformedClass, transformer, transformerMethod, new IdentityHashMap<>()).apply();
    }

    /**
     * Prepare an injection without changing the target's instructions.
     *
     * @param manager     The transformer manager
     * @param target      The target class
     * @param transformer The transformer class
     * @param method      The handler method
     * @param registry    The shared instruction registry for this class
     * @return The prepared injection
     */
    @SuppressWarnings("unchecked")
    public InjectionPlan prepare(final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method, final Map<AbstractInsnNode, InjectionPlan.Target> registry) {
        T annotation = (T) this.getAnnotation(this.getAnnotationClass(), method, manager);
        return this.prepare(annotation, manager, target, transformer, method, registry);
    }

    private InjectionPlan prepare(final T annotation, final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method, final Map<AbstractInsnNode, InjectionPlan.Target> registry) {
        InjectionSettings settings = new InjectionSettings(annotation);
        Set<MethodNode> targets = this.getTargetMethods(annotation, manager, target, transformer, method);
        List<CTarget> injectionTargets = new ArrayList<>();
        for (CTarget injectionTarget : this.getInjectionTargets(annotation, manager, method)) {
            injectionTargets.add(CDescResolver.resolve(injectionTarget, manager, transformer, method));
        }
        return new InjectionPlan(manager, target, settings, transformer, method, targets, injectionTargets, registry, targetMethod -> {
            try {
                this.transform(annotation, manager, target, transformer, ASMUtils.cloneMethod(method), targetMethod);
            } catch (TransformerException e) {
                throw e.targetClass(target.name).targetMethod(targetMethod);
            } catch (Throwable t) {
                throw new TransformerException(method, transformer, "failed").targetClass(target.name).targetMethod(targetMethod).cause(t);
            }
        });
    }

    /**
     * Resolve the target methods for the given annotation.
     *
     * @param annotation  The injection annotation
     * @param manager     The transformer manager
     * @param target      The target class
     * @param transformer The transformer class
     * @param method      The handler method
     * @return The target methods
     */
    protected Set<MethodNode> getTargetMethods(final T annotation, final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method) {
        Set<MethodNode> targets = new LinkedHashSet<>();
        for (String selector : this.methodTargets.apply(annotation)) {
            if (selector.isEmpty()) throw new TransformerException(method, transformer, "Target is empty");
            if (ASMUtils.METHOD_DECLARATION_PATTERN.matcher(selector).matches()) {
                MemberDeclaration declaration = ASMUtils.splitMemberDeclaration(selector);
                if (declaration == null) throw TransformerException.invalidMemberDeclaration(method, transformer, selector);
                if (!target.name.equals(declaration.getOwner())) continue;
                selector = declaration.getName() + declaration.getDesc();
            }
            targets.addAll(ASMUtils.getMethodsFromCombi(target, selector));
        }
        return targets;
    }

    /**
     * Get the instruction selectors defined by the annotation.
     *
     * @param annotation The injection annotation
     * @param manager    The transformer manager
     * @return The instruction selectors to prepare
     */
    protected List<CTarget> getInjectionTargets(final T annotation, final TransformerManager manager) {
        return new InjectionSettings(annotation).getTargets();
    }

    /**
     * Resolve the instruction selectors to prepare for the given handler method.
     * <p>
     * Subclasses may override this to resolve selectors per method; the default delegates to
     * {@link #getInjectionTargets(Annotation, TransformerManager)}.
     *
     * @param annotation The injection annotation
     * @param manager    The transformer manager
     * @param method     The handler method
     * @return The instruction selectors to prepare
     */
    protected List<CTarget> getInjectionTargets(final T annotation, final TransformerManager manager, final MethodNode method) {
        return this.getInjectionTargets(annotation, manager);
    }

}
