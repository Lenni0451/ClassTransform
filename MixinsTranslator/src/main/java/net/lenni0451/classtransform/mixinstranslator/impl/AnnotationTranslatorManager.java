package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CDescs;
import net.lenni0451.classtransform.annotations.CShadow;
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
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;"), new ModifyVariableTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/ModifyArg;"), new ModifyArgTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/Overwrite;"), Type.getType(COverride.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/At;"), new AtTranslator());
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Desc;"), Type.getType(CDesc.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Descriptors;"), Type.getType(CDescs.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Next;"), Type.getType(CDesc.Next.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/Shadow;"), Type.getType(CShadow.class));
        register(Type.getType("Lorg/spongepowered/asm/mixin/injection/Slice;"), new SliceTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/sugar/Share;"), new ShareTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/sugar/Local;"), new LocalTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/sugar/Cancellable;"), new CancellableTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/WrapWithCondition;"), new WrapWithConditionTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;"), new WrapWithConditionTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;"), new ModifyExpressionValueTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;"), new ModifyReturnValueTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/ModifyReceiver;"), new ModifyReceiverTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;"), new WrapOperationTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;"), new WrapMethodTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/expression/Expression;"), new ExpressionTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/expression/Expressions;"), new ExpressionsTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/expression/Definition;"), new DefinitionTranslator());
        register(Type.getType("Lcom/llamalad7/mixinextras/expression/Definitions;"), new DefinitionsTranslator());
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
