package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CLocalDefinition;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @Definition} into ClassTransform's {@link CDefinition}.
 * <p>
 * The id, method, field, type and remap values are copied unchanged. Nested local definitions are converted to
 * {@link CLocalDefinition}: the type, ordinal, index, {@code argsOnly} and {@code print} values are kept, a single
 * {@code name} is stored as {@code names}, and multiple locals are preserved as a list.
 *
 * @see CDefinition
 * @see CLocalDefinition
 */
@ParametersAreNonnullByDefault
class DefinitionTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CDefinition.class);
        this.move(values, "id", "id");
        this.move(values, "method", "method");
        this.move(values, "field", "field");
        this.move(values, "type", "type");
        this.move(values, "remap", "remap");
        Object local = values.remove("local");
        if (local != null) {
            List<AnnotationNode> locals = new ArrayList<>();
            if (local instanceof AnnotationNode) {
                locals.add(this.translateLocal((AnnotationNode) local));
            } else if (local instanceof List) {
                for (Object entry : (List<?>) local) {
                    if (entry instanceof AnnotationNode) locals.add(this.translateLocal((AnnotationNode) entry));
                }
            }
            values.put("local", locals);
        }
    }

    private AnnotationNode translateLocal(final AnnotationNode local) {
        Map<String, Object> values = AnnotationUtils.listToMap(local.values);
        AnnotationNode translated = new AnnotationNode(Type.getDescriptor(CLocalDefinition.class));
        List<Object> result = new ArrayList<>();
        this.move(values, result, "type");
        this.move(values, result, "ordinal");
        this.move(values, result, "index");
        this.move(values, result, "argsOnly");
        this.move(values, result, "print");
        Object name = values.get("name");
        if (name instanceof List) {
            result.add("names");
            result.add(name);
        } else if (name instanceof String) {
            result.add("names");
            List<String> names = new ArrayList<>();
            names.add((String) name);
            result.add(names);
        }
        translated.values = result;
        return translated;
    }

    private void move(final Map<String, Object> values, final List<Object> target, final String key) {
        if (!values.containsKey(key)) return;
        target.add(key);
        target.add(values.get(key));
    }

}
