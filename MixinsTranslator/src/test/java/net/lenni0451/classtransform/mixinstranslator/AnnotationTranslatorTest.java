package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.lenni0451.classtransform.annotations.CDesc;
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

    @Shadow(prefix = "shadow$", aliases = {"first", "second"}, remap = false)
    @CShadow(prefix = "shadow$", aliases = {"first", "second"}, remap = false)
    @TestMarker(from = Shadow.class, to = CShadow.class)
    private native void shadowOptionsTest();

    @Overwrite
    @COverride
    @TestMarker(from = Overwrite.class, to = COverride.class)
    private native void overwriteTest();

    @Overwrite(constraints = "TEST(5)", aliases = {"first", "second"}, remap = false)
    @COverride(constraints = "TEST(5)", aliases = {"first", "second"}, remap = false)
    @TestMarker(from = Overwrite.class, to = COverride.class)
    private native void overwriteOptionsTest();

    @Mutable
    @TestMarker(from = Mutable.class, to = Mutable.class)
    private final String mutableTest = null;

    @Mixin(value = {AnnotationTranslatorTest.class, MixinTest.class}, targets = {"TranslatorTest", "MixinTest"})
    @CTransformer(value = {AnnotationTranslatorTest.class, MixinTest.class}, name = {"TranslatorTest", "MixinTest"})
    @TestMarker(from = Mixin.class, to = CTransformer.class)
    private static class MixinTest {
    }

    @Mixin(value = MixinOptionsTest.class, priority = 2000, remap = false)
    @CTransformer(value = MixinOptionsTest.class, priority = 2000, remap = false)
    @TestMarker(from = Mixin.class, to = CTransformer.class)
    private static class MixinOptionsTest {
    }

    @Final
    @TestMarker(from = Final.class, to = Final.class)
    private final String finalTest = null;

    @Redirect(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), require = 0)
    @CRedirect(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.BY, by = 1, ordinal = 10, optional = true), slice = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), require = 0)
    @TestMarker(from = Redirect.class, to = CRedirect.class)
    private native void redirectTest();

    @ModifyConstant(method = {"method1", "method2"}, slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), constant = @Constant(stringValue = "test"))
    @CModifyConstant(method = {"method1", "method2"}, stringValue = "test", slices = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), require = -1)
    @TestMarker(from = ModifyConstant.class, to = CModifyConstant.class)
    private native void modifyConstantTest();

    @ModifyConstant(method = "compare", constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, log = true))
    @CModifyConstant(method = "compare", intValue = 0, expandZeroConditions = CModifyConstant.Condition.LESS_THAN_ZERO, log = true, require = -1)
    @TestMarker(from = ModifyConstant.class, to = CModifyConstant.class)
    private native void modifyExpandedConstantTest();

    @ModifyConstant(method = "sum", constant = {@Constant(intValue = 10), @Constant(intValue = 20)})
    @CModifyConstant(method = "sum", constants = {@CModifyConstant.CConstant(intValue = 10), @CModifyConstant.CConstant(intValue = 20)}, require = -1)
    @TestMarker(from = ModifyConstant.class, to = CModifyConstant.class)
    private native void modifyMultipleConstantsTest();

    @Inject(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), cancellable = true, require = 0)
    @CInject(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.BY, by = 1, ordinal = 10, optional = true), slices = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), cancellable = true, require = 0)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectTest();

    @Inject(method = "compare", at = @At(value = "CONSTANT", args = {"intValue=0", "expandZeroConditions=LESS_THAN_ZERO", "log=true"}))
    @CInject(method = "compare", target = @CTarget(value = "CONSTANT", target = "int 0", shift = CTarget.Shift.BEFORE, args = {"expandZeroConditions=LESS_THAN_ZERO", "log=true"}), require = -1)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectConstantArgsTest();

    @Inject(method = "add", at = @At("HEAD"), target = @Desc(value = "add", ret = int.class, args = {int.class, int.class}))
    @CInject(method = "add", target = @CTarget("HEAD"), desc = @CDesc(value = "add", ret = int.class, args = {int.class, int.class}), require = -1)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectDescTargetTest();

    @Inject(method = "add", at = @At(value = "INVOKE", desc = @Desc(value = "add", ret = int.class, args = {int.class, int.class})))
    @CInject(method = "add", target = @CTarget(value = "INVOKE", desc = @CDesc(value = "add", ret = int.class, args = {int.class, int.class})), require = -1)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectAtDescTargetTest();

    @Inject(method = "add", at = @At("HEAD"), target = @Desc(value = "add", ret = int.class, args = {int.class, int.class}, min = 1, max = 1, next = @Next(name = "next", ret = String.class)))
    @CInject(method = "add", target = @CTarget("HEAD"), desc = @CDesc(value = "add", ret = int.class, args = {int.class, int.class}, min = 1, max = 1, next = @CDesc.Next(name = "next", ret = String.class)), require = -1)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectDescOptionsTest();

    @Inject(method = "add", at = @At(value = "HEAD", id = "head"))
    @CInject(method = "add", target = @CTarget(value = "HEAD", id = "head"), require = -1)
    @TestMarker(from = Inject.class, to = CInject.class)
    private native void injectTargetIdTest();

    @Redirect(method = "rint", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt()I"), target = @Desc(value = "rint", ret = int.class))
    @CRedirect(method = "rint", target = @CTarget(value = "INVOKE", target = "Ljava/util/Random;nextInt()I"), desc = @CDesc(value = "rint", ret = int.class), require = -1)
    @TestMarker(from = Redirect.class, to = CRedirect.class)
    private native void redirectDescTargetTest();

    @WrapWithCondition(method = {"method1", "method2"}, at = {@At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), @At(value = "test2", target = "target2", shift = At.Shift.BEFORE, ordinal = 20)}, slice = @Slice(from = @At("HEAD"), to = @At("TAIL")))
    @CWrapCondition(method = {"method1", "method2"}, target = {@CTarget(value = "test", target = "target", shift = CTarget.Shift.BY, by = 1, ordinal = 10), @CTarget(value = "test2", target = "target2", shift = CTarget.Shift.BY, by = -1, ordinal = 20)}, slices = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), require = -1)
    @TestMarker(from = WrapWithCondition.class, to = CWrapCondition.class)
    private native void wrapWithConditionTest();

    @WrapOperation(method = "constant", constant = {@Constant(intValue = 5), @Constant(stringValue = "x")})
    @CWrapOperation(method = "constant", target = {@CTarget(value = "CONSTANT", target = "int 5"), @CTarget(value = "CONSTANT", target = "string x")}, require = -1)
    @TestMarker(from = WrapOperation.class, to = CWrapOperation.class)
    private native void wrapOperationConstantsTest();

    @ModifyExpressionValue(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")))
    @CModifyExpressionValue(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.BY, by = 1, ordinal = 10), slices = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), require = -1)
    @TestMarker(from = ModifyExpressionValue.class, to = CModifyExpressionValue.class)
    private native void modifyExpressionValueTest();

    @ModifyExpressionValue(method = "method", at = {@At(value = "CONSTANT", target = "intValue=1"), @At(value = "CONSTANT", target = "intValue=2")})
    @CModifyExpressionValue(method = "method", targets = {@CTarget(value = "CONSTANT", target = "int 1", shift = CTarget.Shift.BEFORE), @CTarget(value = "CONSTANT", target = "int 2", shift = CTarget.Shift.BEFORE)}, require = -1)
    @TestMarker(from = ModifyExpressionValue.class, to = CModifyExpressionValue.class)
    private native void modifyMultipleExpressionValuesTest();

    @ModifyReturnValue(method = {"method1", "method2"}, at = {@At("RETURN"), @At(value = "RETURN", ordinal = 1)}, slice = @Slice(from = @At("HEAD"), to = @At("TAIL")))
    @CModifyReturnValue(method = {"method1", "method2"}, target = {@CTarget(value = "RETURN", shift = CTarget.Shift.BEFORE), @CTarget(value = "RETURN", ordinal = 1, shift = CTarget.Shift.BEFORE)}, slices = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), require = -1)
    @TestMarker(from = ModifyReturnValue.class, to = CModifyReturnValue.class)
    private native void modifyReturnValueTest();

    @ModifyReceiver(method = {"method1", "method2"}, at = @At(value = "test", target = "target", shift = At.Shift.AFTER, ordinal = 10), slice = @Slice(from = @At("HEAD"), to = @At("TAIL")), require = 0, order = 100)
    @CModifyReceiver(method = {"method1", "method2"}, target = @CTarget(value = "test", target = "target", shift = CTarget.Shift.BY, by = 1, ordinal = 10), slices = @CSlice(from = @CTarget("HEAD"), to = @CTarget("TAIL")), require = 0, order = 100)
    @TestMarker(from = ModifyReceiver.class, to = CModifyReceiver.class)
    private native void modifyReceiverTest();

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
