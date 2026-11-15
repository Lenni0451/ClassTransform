package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.InjectionType;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.InjectionPhase;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Shared settings for injection annotations.
 */
@ParametersAreNonnullByDefault
public class InjectionSettings {

    private final Annotation annotation;
    private final Map<String, CSlice> slices = new LinkedHashMap<>();

    /**
     * Create the settings for a parsed injection annotation.
     *
     * @param annotation The parsed injection annotation
     * @throws IllegalArgumentException If a slice id is declared more than once
     */
    public InjectionSettings(final Annotation annotation) {
        this.annotation = annotation;
        CSlice slice = this.get("slice", (CSlice) null);
        if (slice != null && this.wasSet("slice")) this.addSlice(slice);
        for (CSlice namedSlice : this.get("slices", new CSlice[0])) this.addSlice(namedSlice);
        if (slice != null && !this.slices.containsKey("")) this.slices.put("", slice.id().isEmpty() ? slice : null);
    }

    private void addSlice(final CSlice slice) {
        if (this.slices.containsKey(slice.id())) throw new IllegalArgumentException("Duplicate slice id '" + slice.id() + "'");
        this.slices.put(slice.id(), slice);
    }

    /**
     * Get a slice by id.
     *
     * @param id The slice identifier
     * @return The selected slice, or null for the unrestricted default slice
     * @throws IllegalArgumentException If the identifier is unknown
     */
    @Nullable
    public CSlice getSlice(final String id) {
        if (!this.slices.containsKey(id)) throw new IllegalArgumentException("Unknown slice id '" + id + "'");
        return this.slices.get(id);
    }

    /**
     * Get the injection phase.
     *
     * @return The injection phase
     */
    public InjectionPhase getPhase() {
        return this.get("phase", this.annotation.annotationType().getAnnotation(InjectionType.class).value());
    }

    /**
     * Get the application order within the injection phase.
     *
     * @return The application order within the injection phase
     */
    public int getOrder() {
        return this.get("order", 1000);
    }

    /**
     * Get the selectors from both the single-target and multiple-target members.
     *
     * @return The selectors; a new mutable list owned by the caller
     */
    public List<CTarget> getTargets() {
        List<CTarget> targets = new ArrayList<>();
        Object target = this.get("target", (Object) null);
        if (target instanceof CTarget && !((CTarget) target).value().isEmpty()) targets.add((CTarget) target);
        else if (target instanceof CTarget[]) Collections.addAll(targets, (CTarget[]) target);
        Collections.addAll(targets, this.get("targets", new CTarget[0]));
        return targets;
    }

    /**
     * Check whether aggregate count semantics were explicitly requested.
     *
     * @return If aggregate count semantics were explicitly requested
     */
    public boolean hasExplicitRequire() {
        return this.wasSet("require");
    }

    /**
     * Validate successful injections across all target methods.<br>
     * Native annotations without an explicit require retain their mandatory-target behavior.
     *
     * @param manager     The transformer manager
     * @param transformer The transformer class
     * @param method      The handler method
     * @param count       The number of successfully injected calls
     */
    public void validate(final TransformerManager manager, final ClassNode transformer, final MethodNode method, final int count) {
        this.validate(manager, transformer, method, count, true);
    }

    /**
     * Validate successful injections across all target methods.
     *
     * @param manager          The transformer manager
     * @param transformer      The transformer class
     * @param method           The handler method
     * @param count            The number of successfully injected calls
     * @param enforceImplicit  If the implicit mandatory-target behavior should be applied
     */
    public void validate(final TransformerManager manager, final ClassNode transformer, final MethodNode method, final int count, final boolean enforceImplicit) {
        int require = this.get("require", -1);
        if (require < 0) require = manager.getDefaultRequiredInjections();
        if (enforceImplicit && !this.hasExplicitRequire() && !this.get("optional", false)) {
            List<CTarget> targets = this.getTargets();
            if (targets.isEmpty() || targets.stream().anyMatch(target -> !target.optional())) require = Math.max(1, require);
        }
        if (count < require) throw this.countError(transformer, method, "required at least " + require, count);
        int expect = this.get("expect", 1);
        if (manager.isCheckInjectionCounts() && count < expect) throw this.countError(transformer, method, "expected at least " + expect, count);
        int allow = this.get("allow", -1);
        if (allow >= 1 && count > Math.max(require, allow)) throw this.countError(transformer, method, "allowed at most " + Math.max(require, allow), count);
    }

    private TransformerException countError(final ClassNode transformer, final MethodNode method, final String constraint, final int count) {
        return new TransformerException(method, transformer, constraint + " injections but performed " + count);
    }

    private boolean wasSet(final String name) {
        return !(this.annotation instanceof IParsedAnnotation) || ((IParsedAnnotation) this.annotation).wasSet(name);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(final String name, @Nullable final T fallback) {
        try {
            return (T) this.annotation.annotationType().getDeclaredMethod(name).invoke(this.annotation);
        } catch (NoSuchMethodException e) {
            return fallback;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read injection setting '" + name + "'", e);
        }
    }

}
