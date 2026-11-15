package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @ModifyExpressionValue} into {@link CModifyExpressionValue}.
 * <p>
 * Every source {@code at} injection point is translated to a target annotation; one entry is stored as
 * {@code target} and multiple entries as {@code targets}. Remaining members are copied and slices are
 * normalized by {@link AnnotationTranslator#translateInjection(Map)}.
 * <p>
 * If the source has no {@code at} member, neither target member is emitted and the
 * {@link CModifyExpressionValue} default applies.
 *
 * @see CModifyExpressionValue
 */
public class ModifyExpressionValueTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyExpressionValue.class);
        if (values.containsKey("at")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.remove("at");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
            if (targets.size() == 1) values.put("target", targets.get(0));
            else values.put("targets", targets);
        }
        this.translateInjection(values);
    }

}
