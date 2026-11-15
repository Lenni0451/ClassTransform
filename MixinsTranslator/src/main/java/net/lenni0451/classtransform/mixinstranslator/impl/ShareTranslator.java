package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CShared;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Translates MixinExtras' {@code @Share} parameter annotation into {@link CShared}.
 * <p>
 * The shared key ({@code value}) and optional {@code namespace} are preserved. Since {@code @Share}
 * has no {@code global} equivalent, {@code global} keeps its {@code false} default, so an empty
 * namespace scopes the variable to the transformer class.
 * <p>
 * The resulting parameter refers to a shared variable keyed by {@code value} and {@code namespace}.
 * All handlers injecting into the same target invocation see the same variable: reference parameters
 * such as {@code CLocalRef} alias one instance, while the variable starts at its default value.
 *
 * @see CShared
 */
@ParametersAreNonnullByDefault
class ShareTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        annotation.desc = Type.getDescriptor(CShared.class);
        this.move(values, "namespace", "namespace");
    }

}
