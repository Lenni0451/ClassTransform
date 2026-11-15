package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyArgs;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates Mixin's {@code @ModifyArgs} into ClassTransform's {@link CModifyArgs}.
 * <p>
 * Mixin's {@code target} descriptors become {@link CModifyArgs#desc()} and the {@code at} annotation becomes
 * {@link CModifyArgs#target()}. {@link AnnotationTranslator#translateInjection(Map)} completes the common injector
 * settings.
 *
 * @see CModifyArgs
 */
@ParametersAreNonnullByDefault
class ModifyArgsTranslator implements AnnotationTranslator {

    @Override
    public void translate(final AnnotationNode annotation, final Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyArgs.class);
        Object descs = values.remove("target");
        if (descs instanceof AnnotationNode) {
            this.dynamicTranslate((AnnotationNode) descs);
            values.put("desc", descs);
        } else if (descs instanceof List) {
            for (AnnotationNode desc : (List<AnnotationNode>) descs) this.dynamicTranslate(desc);
            values.put("desc", descs);
        }
        Object at = values.remove("at");
        if (at instanceof AnnotationNode) {
            this.dynamicTranslate((AnnotationNode) at);
            values.put("target", at);
        }
        this.translateInjection(values);
    }

}
