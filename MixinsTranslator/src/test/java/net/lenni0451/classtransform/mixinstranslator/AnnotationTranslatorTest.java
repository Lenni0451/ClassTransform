package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.*;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("deprecation")
public class AnnotationTranslatorTest {

    @Unique
    @TestMarker(from = Unique.class, to = Unique.class)
    private final String uniqueTest = null;

    @Shadow
    @CShadow
    @TestMarker(from = Shadow.class, to = CShadow.class)
    private final String shadowTest = null;

    @Overwrite
    @COverride
    @TestMarker(from = Overwrite.class, to = COverride.class)
    private native void overwriteTest();

    @Mutable
    @TestMarker(from = Mutable.class, to = Mutable.class)
    private final String mutableTest = null;

    @Mixin(value = {AnnotationTranslatorTest.class, MixinTest.class}, targets = {"TranslatorTest", "MixinTest"})
    @CTransformer(value = {AnnotationTranslatorTest.class, MixinTest.class}, name = {"TranslatorTest", "MixinTest"})
    @TestMarker(from = Mixin.class, to = CTransformer.class)
    private static class MixinTest {
    }

    @Final
    @TestMarker(from = Final.class, to = Final.class)
    private final String finalTest = null;

    @Redirect(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), require = 0)
    @CRedirect(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.AFTER, ordinal = 10, optional = true), slice = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")))
    @TestMarker(from = Redirect.class, to = CRedirect.class)
    private native void redirectTest();

    @ModifyConstant(method = {"method1", "method2"}, slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), constant = @Constant(stringValue = "test"))
    @CModifyConstant(method = {"method1", "method2"}, stringValue = "test")
    @TestMarker(from = ModifyConstant.class, to = CModifyConstant.class)
    private native void modifyConstantTest();

    @Inject(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), cancellable = true, require = 0)
    @CInject(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.AFTER, ordinal = 10, optional = true), slice = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), cancellable = true)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectTest();

    @WrapWithCondition(method = {"method1", "method2"}, at = {@At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), @At(value = "test2", target = "target2", shift = At.Shift.BEFORE, ordinal = 20)}, slice = @Slice(from = @At("HEAD"), to = @At("TAIL")))
    @CWrapCondition(method = {"method1", "method2"}, target = {@CTarget(value = "test", target = "target", shift = CTarget.Shift.AFTER, ordinal = 10), @CTarget(value = "test2", target = "target2", shift = CTarget.Shift.BEFORE, ordinal = 20)}, slice = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")))
    @TestMarker(from = WrapWithCondition.class, to = CWrapCondition.class)
    private native void wrapWithConditionTest();

    @ModifyExpressionValue(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")))
    @CModifyExpressionValue(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.AFTER, ordinal = 10), slice = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")))
    @TestMarker(from = ModifyExpressionValue.class, to = CModifyExpressionValue.class)
    private native void modifyExpressionValueTest();

    @Test
    void test() throws ClassNotFoundException {
        this.iterate(AnnotationTranslatorTest.class);
    }

    private void iterate(final Class<?> clazz) throws ClassNotFoundException {
        ClassNode classNode = ASMUtils.fromBytes(new BasicClassProvider().getClass(clazz.getName()));
        if (clazz.getDeclaredAnnotation(TestMarker.class) != null) {
            TestMarker testMarker = clazz.getDeclaredAnnotation(TestMarker.class);
            System.out.println("Testing class: " + clazz.getName() + " From: " + Arrays.toString(testMarker.from()) + " To: " + Arrays.toString(testMarker.to()));
            this.runTranslator(annotation -> {
                AnnotationNode annotationNode = AnnotationUtils.findAnnotation(classNode, annotation).orElseThrow(() -> new AssertionError("Annotation not found: " + annotation.getName()));
                return AnnotationUtils.clone(annotationNode);
            }, testMarker);
        }
        for (Field field : clazz.getDeclaredFields()) {
            TestMarker testMarker = field.getDeclaredAnnotation(TestMarker.class);
            if (testMarker == null) continue;

            System.out.println("Testing field: " + field.getName() + " From: " + Arrays.toString(testMarker.from()) + " To: " + Arrays.toString(testMarker.to()));
            FieldNode fieldNode = ASMUtils.getField(classNode, field.getName(), typeDescriptor(field));
            assertNotNull(fieldNode, "Field not found: " + field.getName());
            this.runTranslator(annotation -> {
                AnnotationNode annotationNode = AnnotationUtils.findAnnotation(fieldNode, annotation).orElseThrow(() -> new AssertionError("Annotation not found: " + annotation.getName()));
                return AnnotationUtils.clone(annotationNode);
            }, testMarker);
        }
        for (Method method : clazz.getDeclaredMethods()) {
            TestMarker testMarker = method.getDeclaredAnnotation(TestMarker.class);
            if (testMarker == null) continue;

            System.out.println("Testing method: " + method.getName() + " From: " + Arrays.toString(testMarker.from()) + " To: " + Arrays.toString(testMarker.to()));
            MethodNode methodNode = ASMUtils.getMethod(classNode, method.getName(), typeDescriptor(method));
            assertNotNull(methodNode, "Method not found: " + method.getName());
            this.runTranslator(annotation -> {
                AnnotationNode annotationNode = AnnotationUtils.findAnnotation(methodNode, annotation).orElseThrow(() -> new AssertionError("Annotation not found: " + annotation.getName()));
                return AnnotationUtils.clone(annotationNode);
            }, testMarker);
        }
        for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
            this.iterate(declaredClass);
        }
    }

    private void runTranslator(final Function<Class<? extends Annotation>, AnnotationNode> annotationProvider, final TestMarker testMarker) {
        ClassNode dummyNode = ASMUtils.createEmptyClass("Dummy");
        dummyNode.visibleAnnotations = new ArrayList<>();
        for (Class<? extends Annotation> from : testMarker.from()) dummyNode.visibleAnnotations.add(annotationProvider.apply(from));
        new MixinsTranslator().process(dummyNode);
        assertEquals(testMarker.to().length, dummyNode.visibleAnnotations.size());
        for (Class<? extends Annotation> to : testMarker.to()) {
            AnnotationNode out = AnnotationUtils.findAnnotation(dummyNode, to).orElseThrow(() -> new AssertionError("Annotation not found after processing: " + to.getName()));
            TestUtils.compareAnnotations(out, annotationProvider.apply(to));
        }
    }

}
