package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Translates Mixin's {@code @Slice} into {@link CSlice}.
 * <p>
 * The {@code from} and {@code to} boundary annotations are translated to targets in place, and the
 * {@code id} member is copied unchanged. A boundary absent from the source is not synthesized, so
 * Mixin's {@code HEAD} and {@code TAIL} defaults are not carried over and the {@link CSlice} defaults
 * apply instead.
 *
 * @see CSlice
 */
@ParametersAreNonnullByDefault
class SliceTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CSlice.class);
        if (values.containsKey("from")) this.dynamicTranslate((AnnotationNode) values.get("from"));
        if (values.containsKey("to")) this.dynamicTranslate((AnnotationNode) values.get("to"));
    }

}
