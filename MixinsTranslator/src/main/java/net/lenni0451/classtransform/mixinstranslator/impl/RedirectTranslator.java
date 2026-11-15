package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CRedirect;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates Mixin's {@code @Redirect} into {@link CRedirect}.
 * <p>
 * The source {@code target} descriptor selectors move to {@code desc}, and the source {@code at}
 * injection point moves to {@code target}. When the source explicitly sets {@code require} to a value
 * of zero or less, the generated target is marked {@code optional}; the {@code require} member itself
 * is also retained. Nested annotations are translated in place, and remaining members are copied.
 *
 * @see CRedirect
 */
@ParametersAreNonnullByDefault
class RedirectTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CRedirect.class);
        Boolean optional = null;
        if (values.containsKey("require")) optional = ((int) values.get("require")) <= 0;
        this.move(values, "target", "desc");
        Object descs = values.get("desc");
        if (descs instanceof AnnotationNode) {
            this.dynamicTranslate((AnnotationNode) descs);
        } else if (descs instanceof List) {
            for (AnnotationNode desc : (List<AnnotationNode>) descs) this.dynamicTranslate(desc);
        }
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            AnnotationNode target = (AnnotationNode) values.get("target");
            this.dynamicTranslate(target);
            if (optional != null) {
                target.values.add("optional");
                target.values.add(optional);
            }
        }
        this.translateInjection(values);
    }

}
