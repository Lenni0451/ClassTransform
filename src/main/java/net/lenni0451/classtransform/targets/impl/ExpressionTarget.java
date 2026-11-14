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

    @Nullable
    public static BuildContext getContext() {
        return CONTEXT.get();
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        BuildContext context = CONTEXT.get();
        if (context == null) throw new IllegalStateException("Expression target used outside of a prepared injection");
        List<AbstractInsnNode> matches = ExpressionEngine.match(context.targetClass, method, context.transformerMethod, target.target(), context.matches);
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


    public static class BuildContext {
        private final ClassNode targetClass;
        private final MethodNode transformerMethod;
        private final Map<AbstractInsnNode, ExpressionMatch> matches = new IdentityHashMap<>();

        public BuildContext(final ClassNode targetClass, final MethodNode transformerMethod) {
            this.targetClass = targetClass;
            this.transformerMethod = transformerMethod;
        }

        public Map<AbstractInsnNode, ExpressionMatch> getMatches() {
            return this.matches;
        }

        public List<ExpressionMatch> getMatchList() {
            return new ArrayList<>(this.matches.values());
        }
    }

}
