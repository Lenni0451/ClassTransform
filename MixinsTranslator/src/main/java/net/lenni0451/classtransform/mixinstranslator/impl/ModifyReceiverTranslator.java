package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyReceiver;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @ModifyReceiver} into {@link CModifyReceiver}.
 * <p>
 * The source {@code at} injection points are translated in place and moved to {@code target}; all
 * remaining members are copied. Common injector settings and slices are handled by
 * {@link AnnotationTranslator#translateInjection(Map)}.
 * <p>
 * The targets are not validated during translation; the generated {@link CModifyReceiver} still
 * requires each target to be a non-static method call or field access.
 *
 * @see CModifyReceiver
 */
@ParametersAreNonnullByDefault
class ModifyReceiverTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyReceiver.class);
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
        }
        this.translateInjection(values);
    }

}
