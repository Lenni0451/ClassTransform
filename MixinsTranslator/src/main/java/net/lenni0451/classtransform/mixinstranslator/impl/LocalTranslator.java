package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CLocalVariable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class LocalTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CLocalVariable.class);
        this.map(values, "name", "name", o -> {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof List<?>) {
                List<?> names = (List<?>) o;
                if (names.isEmpty()) {
                    return null; //Remove the name if none is given
                } else if (names.size() == 1) {
                    return names.get(0);
                } else {
                    throw new UnsupportedOperationException("Multiple names are not supported: " + names);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported name type: " + o.getClass().getName());
            }
        });
        values.remove("print");
        values.remove("argsOnly");
        values.put("modifiable", true); //In MixinExtras this does not exist/is always true
    }

}
