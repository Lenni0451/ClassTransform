package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.transformer.InjectionPhase;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Translates both MixinExtras {@code @WrapWithCondition} variants into {@link CWrapCondition}.
 * <p>
 * The legacy {@code com.llamalad7.mixinextras.injector.WrapWithCondition} form is emitted with
 * {@link InjectionPhase#REGULAR} for compatibility, while the {@code v2} form keeps the
 * {@link CWrapCondition#phase()} default of {@link InjectionPhase#CONDITION}.
 * <p>
 * Source {@code at} entries are translated and moved to {@code target}; remaining members are copied and
 * slices are normalized by {@link AnnotationTranslator#translateInjection(Map)}.
 *
 * @see CWrapCondition
 * @see InjectionPhase
 */
@ParametersAreNonnullByDefault
public class WrapWithConditionTranslator implements AnnotationTranslator {

    @Override
    public void translate(AnnotationNode annotation, Map<String, Object> values) {
        if (annotation.desc.equals("Lcom/llamalad7/mixinextras/injector/WrapWithCondition;")) {
            values.put("phase", new String[]{Type.getDescriptor(InjectionPhase.class), "REGULAR"});
        }
        annotation.desc = Type.getDescriptor(CWrapCondition.class);
        this.move(values, "at", "target");
        if (values.containsKey("target")) {
            List<AnnotationNode> targets = (List<AnnotationNode>) values.get("target");
            for (AnnotationNode target : targets) this.dynamicTranslate(target);
        }
        this.translateInjection(values);
    }

}
