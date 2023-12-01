package net.lenni0451.classtransform.mixinstranslator.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AnnotationTranslatorManager {

    private static final Map<String, IAnnotationTranslator> ANNOTATION_TRANSLATOR = new HashMap<>();

    static {
        register(Mixin.class, new MixinTranslator());
        register(Inject.class, new InjectTranslator());
        register(Redirect.class, new RedirectTranslator());
        register(ModifyConstant.class, new ModifyConstantTranslator());
        register(Overwrite.class, new OverwriteTranslator());
        register(At.class, new AtTranslator());
        register(Shadow.class, new ShadowTranslator());
        register(Slice.class, new SliceTranslator());
        register(Share.class, new ShareTranslator());
        register(Local.class, new LocalTranslator());
    }

    private static void register(final Class<? extends Annotation> clazz, final IAnnotationTranslator translator) {
        ANNOTATION_TRANSLATOR.put(clazz.getName(), translator);
    }

    public static IAnnotationTranslator getTranslator(final Type type) {
        return ANNOTATION_TRANSLATOR.get(type.getClassName());
    }

}
