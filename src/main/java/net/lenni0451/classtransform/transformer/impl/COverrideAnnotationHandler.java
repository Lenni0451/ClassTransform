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

import static net.lenni0451.classtransform.utils.Types.argumentTypes;

/**
 * The annotation handler for the {@link COverride} annotation.
 */
@ParametersAreNonnullByDefault
public class COverrideAnnotationHandler extends RemovingTargetAnnotationHandler<COverride> {

    public COverrideAnnotationHandler() {
        super(COverride.class, COverride::value);
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
