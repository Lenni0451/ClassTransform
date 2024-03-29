package net.lenni0451.classtransform.transformer.types;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.exceptions.SliceException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;

/**
 * An abstract annotation handler which handles all annotations of the given type.<br>
 * The handled transformer methods are removed from the transformer class afterward.<br>
 * The target methods of the transformer are automatically parsed.
 *
 * @param <T> The annotation type
 */
@ParametersAreNonnullByDefault
public abstract class RemovingTargetAnnotationHandler<T extends Annotation> extends RemovingAnnotationHandler<T> {

    private final Function<T, String[]> targetCombis;

    public RemovingTargetAnnotationHandler(final Class<T> annotationClass, final Function<T, String[]> targetCombis) {
        super(annotationClass);
        this.targetCombis = targetCombis;
    }

    @Override
    public final void transform(T annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        for (String targetCombi : this.targetCombis.apply(annotation)) {
            if (targetCombi.isEmpty()) throw new TransformerException(transformerMethod, transformer, "Target is empty");
            if (targetCombi.matches(ASMUtils.METHOD_DECLARATION_PATTERN)) {
                MemberDeclaration declaration = ASMUtils.splitMemberDeclaration(targetCombi);
                if (declaration == null) throw new TransformerException(transformerMethod, transformer, "Target is not a valid method declaration");
                if (!transformedClass.name.equals(declaration.getOwner())) continue;
                targetCombi = declaration.getName() + declaration.getDesc();
            }

            List<MethodNode> targets = ASMUtils.getMethodsFromCombi(transformedClass, targetCombi);
            if (targets.isEmpty()) throw new MethodNotFoundException(transformedClass, transformer, targetCombi);
            for (MethodNode target : targets) {
                try {
                    this.transform(annotation, transformerManager, transformedClass, transformer, ASMUtils.cloneMethod(transformerMethod), target);
                } catch (SliceException e) {
                    throw new TransformerException(transformerMethod, transformer, "- " + e.getMessage());
                }
            }
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
     * @param target             The target method node
     */
    public abstract void transform(final T annotation, final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target);

}
