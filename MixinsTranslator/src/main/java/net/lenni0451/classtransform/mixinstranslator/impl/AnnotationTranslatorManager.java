package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.annotations.injection.COverride;
import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AnnotationTranslatorManager {

    private static final Map<Type, AnnotationTranslator> ANNOTATION_TRANSLATOR = new HashMap<>();

    static {
        register(Type.getType("Lorg/spongepowered/asm/mixin/Mixin;"), new MixinTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Inject;"), new InjectTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Redirect;"), new RedirectTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/ModifyConstant;"), new ModifyConstantTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/Overwrite;"), Type.getType(COverride.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/At;"), new AtTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/Shadow;"), Type.getType(CShadow.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Slice;"), new SliceTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/sugar/Share;"), Type.getType(CShared.class));
        register(Type.getType("Lcom/llamalad7/mixinextras/sugar/Local;"), new LocalTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/WrapWithCondition;"), new WrapWithConditionTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;"), new WrapWithConditionTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;"), new ModifyExpressionValueTranslator());
    }

    private static void register(final Type from, final Type to) {
        register(from, (annotation, values) -> annotation.desc = to.getDescriptor());
    }

    private static void register(final Type type, final AnnotationTranslator translator) {
        ANNOTATION_TRANSLATOR.put(type, translator);
    }

    public static AnnotationTranslator getTranslator(final Type type) {
        return ANNOTATION_TRANSLATOR.get(type);
    }

}
