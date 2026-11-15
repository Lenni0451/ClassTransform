package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;

/**
 * Applies the {@link COverride} annotation by replacing the target method with the transformer method, remapped into the transformed class.
 * <p>
 * The transformer method must have matching static access, the same argument types and at least the access level of the target; aliases are tried in order until a target matches.
 */
@ParametersAreNonnullByDefault
public class COverrideAnnotationHandler extends RemovingTargetAnnotationHandler<COverride> {

    /**
     * Creates a handler for the {@link COverride} annotation.
     */
    public COverrideAnnotationHandler() {
        super(COverride.class, COverride::value);
    }

    @Override
    public void transform(final COverride annotation, final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod) {
        this.checkConstraints(transformerManager, annotation.constraints(), transformerMethod, transformer);
        super.transform(annotation, transformerManager, transformedClass, transformer, transformerMethod);
    }

    @Override
    protected List<MethodNode> getTargetMethods(final COverride annotation, final ClassNode target, final String selector) {
        List<MethodNode> targets = super.getTargetMethods(annotation, target, selector);
        for (String alias : annotation.aliases()) {
            if (!targets.isEmpty()) break;
            if (!alias.isEmpty()) targets = super.getTargetMethods(annotation, target, alias);
        }
        return targets;
    }

    @Override
    public void transform(COverride annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        if (!ASMUtils.compareTypes(argumentTypes(target.desc), argumentTypes(transformerMethod.desc))) {
            throw TransformerException.wrongArguments(transformerMethod, transformer, argumentTypes(target.desc));
        }
        if (ASMUtils.isAccessLower(transformerMethod.access, target.access)) {
            throw new TransformerException(transformerMethod, transformer, "must have higher/equal access than original method");
        }
        transformerMethod.name = target.name;
        transformerMethod.desc = target.desc;
        transformedClass.methods.remove(target);
        this.prepareForCopy(transformer, transformerMethod);
        Remapper.remapAndAdd(transformer, transformedClass, transformerMethod);
    }

}
