package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.expression.ExpressionEngine;
import net.lenni0451.classtransform.expression.ExpressionMatch;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The target for {@code @CTarget(value = "EXPRESSION")}.<br>
 * The {@link CTarget#target()} is the id of the selected {@code @CExpression}.
 * <p>
 * A {@link BuildContext} has to be installed with {@link #setContext} before the target is resolved
 * and removed with {@link #clearContext()} afterwards. The context is kept per thread. Matches are
 * returned in target method order and are filtered by the slice when one is given.
 */
@ParametersAreNonnullByDefault
public class ExpressionTarget implements IInjectionTarget {

    private static final ThreadLocal<BuildContext> CONTEXT = new ThreadLocal<>();

    /**
     * Set the context for the next expression match.
     *
     * @param context The context
     */
    public static void setContext(final BuildContext context) {
        CONTEXT.set(context);
    }

    /**
     * Clear the current expression match context.
     */
    public static void clearContext() {
        CONTEXT.remove();
    }

    /**
     * Get the context installed for the current thread.
     *
     * @return The context, or {@code null} if none is installed
     */
    @Nullable
    public static BuildContext getContext() {
        return CONTEXT.get();
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        BuildContext context = CONTEXT.get();
        if (context == null) throw new IllegalStateException("Expression target used outside of a prepared injection");
        List<AbstractInsnNode> matches = ExpressionEngine.match(context.transformerManager, context.targetClass, method, context.transformerMethod, target.target(), context.matches);
        if (slice == null) return matches;
        List<AbstractInsnNode> inSlice = this.getSlice(injectionTargets, method, slice);
        List<AbstractInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode match : matches) {
            if (inSlice.contains(match)) result.add(match);
        }
        return result;
    }

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        return null;
    }


    /**
     * The per-thread state needed to resolve one expression target.
     * <p>
     * The context records the target class and the transformer method and collects the
     * instructions matched by the expression engine.
     */
    public static class BuildContext {
        private final TransformerManager transformerManager;
        private final ClassNode targetClass;
        private final MethodNode transformerMethod;
        private final Map<AbstractInsnNode, ExpressionMatch> matches = new IdentityHashMap<>();

        /**
         * Create a context without a transformer manager.
         *
         * @param targetClass       The class that is being transformed
         * @param transformerMethod The transformer method declaring the expression
         */
        public BuildContext(final ClassNode targetClass, final MethodNode transformerMethod) {
            this(null, targetClass, transformerMethod);
        }

        /**
         * Create a context with an optional transformer manager.
         *
         * @param transformerManager The transformer manager, or {@code null}
         * @param targetClass        The class that is being transformed
         * @param transformerMethod  The transformer method declaring the expression
         */
        public BuildContext(@Nullable final TransformerManager transformerManager, final ClassNode targetClass, final MethodNode transformerMethod) {
            this.transformerManager = transformerManager;
            this.targetClass = targetClass;
            this.transformerMethod = transformerMethod;
        }

        /**
         * Get the collected matches keyed by instruction identity.
         *
         * @return The mutable match map
         */
        public Map<AbstractInsnNode, ExpressionMatch> getMatches() {
            return this.matches;
        }

        /**
         * Get a snapshot of the collected matches.
         *
         * @return A new list containing the match values
         */
        public List<ExpressionMatch> getMatchList() {
            return new ArrayList<>(this.matches.values());
        }
    }

}
