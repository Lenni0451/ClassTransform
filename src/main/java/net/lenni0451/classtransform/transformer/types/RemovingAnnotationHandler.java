package net.lenni0451.classtransform.transformer.types;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.util.Iterator;

/**
 * An abstract annotation handler which handles all annotations of the given type.<br>
 * The handled transformer methods are removed from the transformer class afterward.
 *
 * @param <T> The annotation type
 */
@ParametersAreNonnullByDefault
public abstract class RemovingAnnotationHandler<T extends Annotation> extends AnnotationHandler {

    private final Class<? extends Annotation> annotationClass;

    public RemovingAnnotationHandler(final Class<T> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public final void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        Iterator<MethodNode> it = transformer.methods.iterator();
        while (it.hasNext()) {
            MethodNode transformerMethod = it.next();
            T annotation = (T) this.getAnnotation(this.annotationClass, transformerMethod, transformerManager);
            if (annotation == null) continue;
            if (!this.shouldExecute(annotation)) continue;
            it.remove();

            this.transform(annotation, transformerManager, transformedClass, transformer, ASMUtils.cloneMethod(transformerMethod));
        }
    }

    /**
     * Handle a transformer method of the transformer with the given annotation.
     *
     * @param annotation         The annotation of the transformer method
     * @param transformerManager The transformer manager
     * @param transformedClass   The target class node
     * @param transformer        The transformer class node
     * @param transformerMethod  The method node of the transformer
     */
    public abstract void transform(final T annotation, final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod);

    /**
     * Check if the transformer should be executed.<br>
     * If the transformer is not executed the method will not be removed.
     *
     * @param annotation The annotation of the transformer
     * @return If the transformer should be executed
     */
    public boolean shouldExecute(final T annotation) {
        return true;
    }

}
