package net.lenni0451.classtransform.transformer.types;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
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
 *
 * @param <T> The injection annotation
 */
public abstract class InjectionAnnotationHandler<T extends Annotation> extends RemovingTargetAnnotationHandler<T> {

    private final Function<T, String[]> methodTargets;

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
        List<CTarget> injectionTargets = this.getInjectionTargets(annotation, manager);
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
     * @param annotation The injection annotation
     * @param manager    The transformer manager
     * @return The instruction selectors to prepare
     */
    protected List<CTarget> getInjectionTargets(final T annotation, final TransformerManager manager) {
        return new InjectionSettings(annotation).getTargets();
    }

}
