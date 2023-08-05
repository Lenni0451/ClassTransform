package net.lenni0451.classtransform.mixinstranslator.impl;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AnnotationTranslatorManager {

    private static final Map<String, IAnnotationTranslator> ANNOTATION_TRANSLATOR = new HashMap<>();

    static {
        ANNOTATION_TRANSLATOR.put(Mixin.class.getName(), new MixinTranslator());
        ANNOTATION_TRANSLATOR.put(Inject.class.getName(), new InjectTranslator());
        ANNOTATION_TRANSLATOR.put(Redirect.class.getName(), new RedirectTranslator());
        ANNOTATION_TRANSLATOR.put(ModifyConstant.class.getName(), new ModifyConstantTranslator());
        ANNOTATION_TRANSLATOR.put(Overwrite.class.getName(), new OverwriteTranslator());
        ANNOTATION_TRANSLATOR.put(At.class.getName(), new AtTranslator());
        ANNOTATION_TRANSLATOR.put(Shadow.class.getName(), new ShadowTranslator());
        ANNOTATION_TRANSLATOR.put(Slice.class.getName(), new SliceTranslator());
    }

    public static IAnnotationTranslator getTranslator(final Type type) {
        return ANNOTATION_TRANSLATOR.get(type.getClassName());
    }

}
