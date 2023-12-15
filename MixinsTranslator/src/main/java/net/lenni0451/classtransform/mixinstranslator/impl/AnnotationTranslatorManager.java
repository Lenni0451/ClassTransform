package net.lenni0451.classtransform.mixinstranslator.impl;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.annotations.injection.COverride;
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
        register(Overwrite.class, COverride.class);
        register(At.class, new AtTranslator());
        register(Shadow.class, CShadow.class);
        register(Slice.class, new SliceTranslator());
        register(Share.class, CShared.class);
        register(Local.class, new LocalTranslator());
        register(WrapWithCondition.class, new WrapWithConditionTranslator());
    }

    private static void register(final Class<? extends Annotation> from, final Class<? extends Annotation> to) {
        register(from, annotation -> annotation.desc = Type.getDescriptor(to));
    }

    private static void register(final Class<? extends Annotation> clazz, final IAnnotationTranslator translator) {
        ANNOTATION_TRANSLATOR.put(clazz.getName(), translator);
    }

    public static IAnnotationTranslator getTranslator(final Type type) {
        return ANNOTATION_TRANSLATOR.get(type.getClassName());
    }

}
