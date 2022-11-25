package net.lenni0451.classtransform.transformer.types;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class ARemovingTargetTransformer<T extends Annotation> extends ARemovingTransformer<T> {

    private final Function<T, String[]> targetCombis;

    public ARemovingTargetTransformer(final Class<T> annotationClass, final Function<T, String[]> targetCombis) {
        super(annotationClass);
        this.targetCombis = targetCombis;
    }

    @Override
    public final void transform(T annotation, TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        for (String targetCombi : this.targetCombis.apply(annotation)) {
            List<MethodNode> targets = ASMUtils.getMethodsFromCombi(transformedClass, targetCombi);
            if (targets.isEmpty()) throw new MethodNotFoundException(transformedClass, transformer, targetCombi);
            for (MethodNode target : targets) {
                this.transform(annotation, transformerManager, classProvider, injectionTargets, transformedClass, transformer, ASMUtils.cloneMethod(transformerMethod), target);
            }
        }
    }

    /**
     * Transform the target class using the given transformer class
     *
     * @param annotation         The annotation of the transformer
     * @param transformerManager The transformer manager
     * @param classProvider      The class provider
     * @param injectionTargets   The available injection targets
     * @param transformedClass   The target {@link ClassNode}
     * @param transformer        The transformer {@link ClassNode}
     * @param transformerMethod  The current {@link MethodNode} of the transformer
     * @param target             The currently transformed target {@link MethodNode}
     */
    public abstract void transform(final T annotation, final TransformerManager transformerManager, final IClassProvider classProvider, final Map<String, IInjectionTarget> injectionTargets, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target);

}
