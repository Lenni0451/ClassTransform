package net.lenni0451.classtransform.transformer.types;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;

public abstract class RemovingAnnotationHandler<T extends Annotation> extends AnnotationHandler {

    private final Class<? extends Annotation> annotationClass;

    public RemovingAnnotationHandler(final Class<T> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public final void transform(TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer) {
        Iterator<MethodNode> it = transformer.methods.iterator();
        while (it.hasNext()) {
            MethodNode transformerMethod = it.next();
            T annotation = (T) this.getAnnotation(this.annotationClass, transformerMethod, classProvider);
            if (annotation == null) continue;
            it.remove();

            this.transform(annotation, transformerManager, classProvider, injectionTargets, transformedClass, transformer, ASMUtils.cloneMethod(transformerMethod));
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
     */
    public abstract void transform(final T annotation, final TransformerManager transformerManager, final IClassProvider classProvider, final Map<String, IInjectionTarget> injectionTargets, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod);

}
