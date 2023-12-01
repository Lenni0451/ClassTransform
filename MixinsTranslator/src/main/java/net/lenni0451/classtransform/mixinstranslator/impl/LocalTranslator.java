package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class LocalTranslator implements IAnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation) {
        annotation.desc = Type.getDescriptor(CLocalVariable.class);
        Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
        values.remove("print");
        values.remove("argsOnly");
        values.put("modifiable", true); //In MixinExtras this does not exist/is always true
        annotation.values = AnnotationUtils.mapToList(values);
    }

}
