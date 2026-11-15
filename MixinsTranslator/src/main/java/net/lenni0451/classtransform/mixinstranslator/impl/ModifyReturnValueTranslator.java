package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @ModifyReturnValue} into {@link CModifyReturnValue}.
 * <p>
 * The source {@code at} injection points are translated in place and moved to {@code target}. The
 * target list stays a list even when it has a single entry, matching {@link CModifyReturnValue#target()}.
 * Remaining members are copied and slices are normalized by
 * {@link AnnotationTranslator#translateInjection(Map)}.
 * <p>
 * If the source has no {@code at} member, the target member is omitted and the {@code RETURN} default
 * applies.
 *
 * @see CModifyReturnValue
 */
@ParametersAreNonnullByDefault
public class ModifyReturnValueTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CModifyReturnValue.class);
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
        }
        this.translateInjection(values);
    }

}
