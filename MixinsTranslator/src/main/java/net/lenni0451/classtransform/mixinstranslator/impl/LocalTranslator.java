package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CLocalVariable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @Local} into ClassTransform's {@link CLocalVariable}.
 * <p>
 * A single non-empty name is stored as {@code name} and multiple names as {@code names}; empty names, negative
 * ordinals and negative indices are dropped, and {@code type} is removed because the parameter type selects the local.
 * {@code strict} is always enabled. If no discriminator remains, {@code implicit} is set so the only local of the
 * parameter type is selected. An unexpected value type for the name is rejected with an
 * {@link UnsupportedOperationException}.
 *
 * @see CLocalVariable
 */
@ParametersAreNonnullByDefault
public class LocalTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CLocalVariable.class);
        List<String> names = new ArrayList<>();
        Object nameValue = values.remove("name");
        if (nameValue instanceof String) {
            if (!((String) nameValue).isEmpty()) names.add((String) nameValue);
        } else if (nameValue instanceof List<?>) {
            for (Object name : (List<?>) nameValue) {
                if (!((String) name).isEmpty()) names.add((String) name);
            }
        } else if (nameValue != null) {
            throw new UnsupportedOperationException("Unsupported name type: " + nameValue.getClass().getName());
        }
        if (names.size() == 1) values.put("name", names.get(0));
        else if (!names.isEmpty()) values.put("names", names);

        this.removeNegative(values, "ordinal");
        this.removeNegative(values, "index");
        this.move(values, "argsOnly", "argsOnly");
        this.move(values, "print", "print");
        values.put("strict", true);
        values.remove("type");
        if (!values.containsKey("name") && !values.containsKey("names") && !values.containsKey("ordinal") && !values.containsKey("index")) {
            values.put("implicit", true);
        }
    }

    private void removeNegative(final Map<String, Object> values, final String key) {
        Object value = values.get(key);
        if (value instanceof Integer && (Integer) value < 0) values.remove(key);
    }

}
