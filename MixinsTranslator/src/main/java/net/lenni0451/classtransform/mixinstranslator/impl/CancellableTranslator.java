package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CCancellable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @Cancellable} into ClassTransform's {@link CCancellable}.
 * <p>
 * Only the annotation descriptor is changed; the annotation has no values.
 *
 * @see CCancellable
 */
@ParametersAreNonnullByDefault
class CancellableTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CCancellable.class);
    }

}
