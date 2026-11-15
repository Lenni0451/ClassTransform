package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates Mixin's {@code @ModifyVariable} into {@link CModifyVariable}.
 * <p>
 * The source {@code target} descriptor selectors are translated and moved to {@code desc}, and the
 * source {@code at} injection point becomes {@code target}. Remaining members such as {@code index},
 * {@code ordinal}, {@code name} and {@code argsOnly} are copied unchanged, and common injector settings
 * are handled by {@link AnnotationTranslator#translateInjection(Map)}.
 * <p>
 * Members absent from the source annotation are omitted, leaving the {@link CModifyVariable} defaults
 * in effect.
 *
 * @see CModifyVariable
 */
@ParametersAreNonnullByDefault
class ModifyVariableTranslator implements AnnotationTranslator {

    @Override
    public void translate(final AnnotationNode annotation, final Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyVariable.class);
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
