package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CWrapMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @WrapMethod} into {@link CWrapMethod}.
 * <p>
 * The source and target annotations have matching members, so only the annotation descriptor is
 * changed and {@link AnnotationTranslator#translateInjection(Map)} supplies the default {@code require}
 * value. All other members are copied unchanged.
 *
 * @see CWrapMethod
 */
@ParametersAreNonnullByDefault
class WrapMethodTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CWrapMethod.class);
        this.translateInjection(values);
    }

}
