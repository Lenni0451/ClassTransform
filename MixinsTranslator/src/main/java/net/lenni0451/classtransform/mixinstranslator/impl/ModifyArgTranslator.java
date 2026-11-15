package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates Mixin's {@code @ModifyArg} into {@link CModifyArg}.
 * <p>
 * Descriptor selectors in the source {@code target} member are translated and moved to {@code desc};
 * the source {@code at} injection point becomes {@code target}. Nested annotations are translated in
 * place, and all remaining members are copied unchanged. Common injector settings and slices are
 * handled by {@link AnnotationTranslator#translateInjection(Map)}.
 * <p>
 * Members that are absent from the source annotation are omitted, leaving the {@link CModifyArg} defaults in
 * effect.
 *
 * @see CModifyArg
 */
@ParametersAreNonnullByDefault
class ModifyArgTranslator implements AnnotationTranslator {

    @Override
    public void translate(final AnnotationNode annotation, final Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyArg.class);
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
