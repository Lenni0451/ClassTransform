package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.expression.CDefinitions;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @Definitions} into ClassTransform's {@link CDefinitions}.
 * <p>
 * The contained definitions are translated one by one with the translator registered for {@code @Definition}.
 *
 * @see CDefinitions
 * @see DefinitionTranslator
 */
@ParametersAreNonnullByDefault
class DefinitionsTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CDefinitions.class);
        Object value = values.get("value");
        if (value instanceof List) {
            for (Object entry : (List<?>) value) {
                if (entry instanceof AnnotationNode) this.dynamicTranslate((AnnotationNode) entry);
            }
        }
    }

}
