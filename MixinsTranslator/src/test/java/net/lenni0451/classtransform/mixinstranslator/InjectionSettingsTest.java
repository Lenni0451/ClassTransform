package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.mappings.impl.VoidMapper;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.transformer.impl.CModifyExpressionValueAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InjectionSettingsTest {

    private static final String OWNER = "net/lenni0451/classtransform/mixinstranslator/InjectionSettingsTest$Target";
    private static final String TARGET = "L" + OWNER + ";";
    private final BasicClassProvider provider = new BasicClassProvider();

    @ParameterizedTest
    @CsvSource({"2, 1, -1, false, true", "3, 1, -1, false, false", "0, 1, 1, false, false",
            "2, 1, 1, false, true", "0, 3, -1, false, true", "0, 3, -1, true, false",
            "0, 2, 0, true, true", "-1, 1, -1, false, true"})
    void aggregateCounts(int require, int expect, int allow, boolean debug, boolean succeeds) throws Exception {
        TransformerManager manager = this.manager();
        manager.setCheckInjectionCounts(debug);
        ClassNode mixin = this.mixin("count");
        AnnotationNode annotation = AnnotationUtils.findAnnotation(mixin.methods.get(0), ModifyReturnValue.class).get();
        Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
        values.put("require", require);
        values.put("expect", expect);
        values.put("allow", allow);
        annotation.values = AnnotationUtils.mapToList(values);
        manager.addTransformer(mixin);
        if (!succeeds) {
            TransformerException error = assertThrows(TransformerException.class, () -> this.load(manager));
            assertTrue(error.getMessage().contains("but performed 2"), error.getMessage());
        } else {
            Class<?> target = this.load(manager);
            assertEquals(11, target.getMethod("one").invoke(null));
            assertEquals(12, target.getMethod("two").invoke(null));
        }
    }

    @Test
    void missingMethodsAndPointsRespectDefaults() throws Exception {
        for (String handler : Arrays.asList("missingMethod", "missingPoint")) {
            TransformerManager manager = this.manager();
            manager.addTransformer(this.mixin(handler));
            assertEquals(1, this.load(manager).getMethod("one").invoke(null));
            manager = this.manager();
            manager.setDefaultRequiredInjections(1);
            manager.addTransformer(this.mixin(handler));
            TransformerManager required = manager;
            TransformerException error = assertThrows(TransformerException.class, () -> this.load(required));
            assertTrue(error.getMessage().contains("but performed 0"), error.getMessage());
        }
    }

    @Test
    void selectsNamedSlicesAndDeduplicatesOverlaps() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("slices"));
        assertEquals(36, this.load(manager).getMethod("values").invoke(null));
    }

    @Test
    void rejectsUnknownAndDuplicateSlices() throws Exception {
        for (String name : Arrays.asList("unknownSlice", "duplicateSlice", "ambiguousBoundary")) {
            TransformerManager manager = this.manager();
            manager.addTransformer(this.mixin(name));
            assertThrows(Throwable.class, () -> this.load(manager));
        }
    }

    @Test
    void opcodeFilterPrecedesOrdinal() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("field"));
        Class<?> target = this.load(manager);
        assertEquals(7, target.getMethod("field").invoke(null));
        assertEquals(4, target.getField("number").get(null));
    }

    @Test
    void constantSliceAndCounts() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("constant"));
        assertEquals(9, this.load(manager).getMethod("values").invoke(null));
    }

    @Test
    void orderAcrossTransformersAndInjectorTypes() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("lateReturn", "expression", "redirect"));
        ClassNode earlier = this.mixin("earlyReturn", "earlierExpression");
        earlier.name += "Second";
        manager.addTransformer(earlier);
        // redirect: 3; expression: (3 + 1) * 2; returns: (8 + 1) * 10
        assertEquals(90, this.load(manager).getMethod("operation").invoke(null));
    }

    @Test
    void stableOrderUsesRegistrationThenDeclaration() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("equalAdd", "equalMultiply"));
        ClassNode second = this.mixin("equalAdd");
        second.name += "Second";
        manager.addTransformer(second);
        assertEquals(21, this.load(manager).getMethod("one").invoke(null));
    }

    @Test
    void conditionsFollowVirtualRedirectsAndKeepOriginalArguments() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("redirectWrite", "conditionWrite"));
        Class<?> target = this.load(manager);
        Object instance = target.getConstructor().newInstance();
        target.getMethod("write", long.class).invoke(instance, 7L);
        assertEquals(7L, target.getField("seen").get(null));
        assertEquals(0L, target.getField("written").get(null));
    }

    @Test
    void customHandlerPlacementCreatesBarrier() throws Exception {
        TransformerManager manager = this.manager();
        List<String> events = new ArrayList<>();
        manager.addCustomAnnotationHandlerBefore(this.observer("before", events), CModifyExpressionValueAnnotationHandler.class);
        manager.addCustomAnnotationHandlerAfter(this.observer("after", events), CModifyExpressionValueAnnotationHandler.class);
        manager.addTransformer(this.mixin("expression"));
        assertEquals(4, this.load(manager).getMethod("operation").invoke(null));
        assertEquals(Arrays.asList("before:0", "after:1"), events);
        assertThrows(IllegalArgumentException.class, () -> manager.addCustomAnnotationHandlerBefore(this.observer("", events), AnnotationHandler.class));
    }

    @Test
    void remapInheritanceAndExplicitOverrides() throws Exception {
        for (String name : Arrays.asList("mapped", "unmapped", "nestedMapped", "nestedUnmapped", "mappedSlice")) {
            VoidMapper mapper = new VoidMapper();
            mapper.getRemapper().addMethodMapping(OWNER, "oldOperation", "()I", "operation");
            mapper.getRemapper().addMethodMapping("old/Math", "oldAbs", "(I)I", "abs");
            mapper.getRemapper().addClassMapping("old/Math", "java/lang/Math");
            if (name.equals("unmapped") || name.equals("nestedMapped")) {
                mapper.getRemapper().addMethodMapping(OWNER, "operation", "()I", "missing");
            }
            if (name.equals("unmapped") || name.equals("nestedUnmapped")) {
                mapper.getRemapper().addMethodMapping("java/lang/Math", "abs", "(I)I", "missing");
            }
            TransformerManager manager = new TransformerManager(this.provider, mapper);
            manager.setFailStrategy(FailStrategy.THROW);
            manager.addTransformerPreprocessor(new MixinsTranslator());
            manager.addTransformer(this.mixin(name));
            assertEquals(3, this.load(manager).getMethod("operation").invoke(null), name);
        }
    }

    @Test
    void legacyConditionRunsBeforeRegularInjectionsButV2RunsAfter() throws Exception {
        for (boolean legacy : new boolean[]{false, true}) {
            TransformerManager manager = this.manager();
            ClassNode mixin = this.mixin("conditionPhase", "regularPhase");
            if (legacy) {
                MethodNode method = mixin.methods.stream().filter(node -> node.name.equals("conditionPhase")).findFirst().get();
                AnnotationUtils.findAnnotation(method, WrapWithCondition.class).get().desc = "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;";
            }
            manager.addTransformer(mixin);
            Class<?> target = this.load(manager);
            target.getMethod("write", long.class).invoke(target.getConstructor().newInstance(), 7L);
            assertEquals(legacy ? 0 : 1, target.getField("number").get(null));
            assertEquals(0L, target.getField("written").get(null));
        }
    }

    @ParameterizedTest
    @CsvSource({"NONE, 0, 192", "BEFORE, 0, 912", "AFTER, 0, 129", "BY, -1, 912", "BY, 1, 129"})
    void shiftsSelectActualInstructions(String shift, int by, int expected) throws Exception {
        TransformerManager manager = this.manager();
        ClassNode mixin = this.mixin("shift");
        AnnotationNode annotation = AnnotationUtils.findAnnotation(mixin.methods.get(0), Inject.class).get();
        AnnotationNode at = ((List<AnnotationNode>) AnnotationUtils.listToMap(annotation.values).get("at")).get(0);
        at.values.addAll(Arrays.asList("shift", new String[]{Type.getDescriptor(At.Shift.class), shift}, "by", by));
        manager.addTransformer(mixin);
        Class<?> target = this.load(manager);
        target.getMethod("steps").invoke(null);
        assertEquals(expected, target.getField("number").get(null));
    }

    @Test
    void jumpWithoutOpcodeAndShiftOutsideMethod() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("jump"));
        Class<?> target = this.load(manager);
        assertEquals(1, target.getMethod("branch", boolean.class).invoke(null, true));
        assertEquals(1, target.getField("number").get(null));
        manager = this.manager();
        manager.addTransformer(this.mixin("invalidShift"));
        TransformerManager invalid = manager;
        assertThrows(Throwable.class, () -> this.load(invalid));
    }

    @Test
    void stringArgumentSelectorAndAssignmentPoint() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("stringCondition", "assignment"));
        Class<?> target = this.load(manager);
        target.getMethod("strings").invoke(null);
        assertEquals(1, target.getField("number").get(null));
        assertEquals(4, target.getMethod("assigned").invoke(null));
    }

    @Test
    void expressionsFollowRedirectCasts() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("redirectString", "stringExpression"));
        assertEquals("redirected!", this.load(manager).getMethod("text").invoke(null));
    }

    @Test
    void fieldConditionsFollowVirtualRedirects() throws Exception {
        TransformerManager manager = this.manager();
        manager.addTransformer(this.mixin("redirectField", "conditionField"));
        Class<?> target = this.load(manager);
        Object instance = target.getConstructor().newInstance();
        target.getMethod("fieldWrite", long.class).invoke(instance, 7L);
        assertEquals(0L, target.getField("instanceValue").get(instance));
        target.getField("number").set(null, 1);
        target.getMethod("fieldWrite", long.class).invoke(instance, 7L);
        assertEquals(14L, target.getField("instanceValue").get(instance));
    }

    @Test
    void classConstantsAreRemappedForBothModifierTypes() throws Exception {
        for (String name : Arrays.asList("classExpression", "classConstant")) {
            VoidMapper mapper = new VoidMapper();
            mapper.getRemapper().addClassMapping("java/lang/String", "java/lang/Integer");
            TransformerManager manager = new TransformerManager(this.provider, mapper);
            manager.setFailStrategy(FailStrategy.THROW);
            manager.addTransformerPreprocessor(new MixinsTranslator());
            manager.addTransformer(this.mixin(name));
            assertEquals(Double.class, this.load(manager).getMethod("classValue").invoke(null));
        }
    }

    private AnnotationHandler observer(final String name, final List<String> events) {
        return new AnnotationHandler() {
            @Override
            public void transform(TransformerManager manager, ClassNode target, ClassNode transformer) {
                long calls = Arrays.stream(ASMUtils.getMethod(target, "operation", "()I").instructions.toArray())
                        .filter(instruction -> instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals(target.name)).count();
                events.add(name + ":" + calls);
            }
        };
    }

    private TransformerManager manager() {
        TransformerManager manager = new TransformerManager(this.provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        return manager;
    }

    private ClassNode mixin(final String... methods) throws Exception {
        ClassNode node = ASMUtils.fromBytes(this.provider.getClass(SettingsMixin.class.getName()));
        node.methods.removeIf(method -> !Arrays.asList(methods).contains(method.name));
        return node;
    }

    private Class<?> load(final TransformerManager manager) throws Exception {
        ClassNode original = ASMUtils.fromBytes(this.provider.getClass(Target.class.getName()));
        MethodNode steps = ASMUtils.getMethod(original, "steps", "()V");
        for (AbstractInsnNode instruction : steps.instructions.toArray()) {
            if (instruction.getOpcode() < 0) steps.instructions.remove(instruction);
        }
        byte[] bytes = manager.transform(Target.class.getName(), ASMUtils.toBytes(original, manager));
        assertNotNull(bytes);
        return new ClassLoader() {
            Class<?> define() {
                return this.defineClass(Target.class.getName(), bytes, 0, bytes.length);
            }
        }.define();
    }

    public static class Target {
        public static int number;
        public static long written;
        public static long seen;
        public long instanceValue;

        public static int one() {
            return 1;
        }

        public static int two() {
            return 2;
        }

        public static int values() {
            return Math.abs(1) + Math.abs(2) + Math.abs(3);
        }

        public static int operation() {
            return Math.abs(2);
        }

        public static int field() {
            number = 4;
            return number;
        }

        public void write(long value) {
            consume(value);
        }

        public static void consume(long value) {
            written = value;
        }

        public static void steps() {
            first();
            second();
        }

        public static void first() {
            number = number * 10 + 1;
        }

        public static void second() {
            number = number * 10 + 2;
        }

        public static int branch(boolean enabled) {
            if (enabled) return 1;
            return 2;
        }

        public static void strings() {
            string("first");
            string("second");
        }

        public static void string(String value) {
            number++;
        }

        public static int assigned() {
            int value = Math.abs(2);
            return value + number;
        }

        public static String text() {
            return "original".trim();
        }

        public void fieldWrite(long value) {
            this.instanceValue = value;
        }

        public static Class<?> classValue() {
            return Integer.class;
        }
    }

    @Mixin(Target.class)
    public static class SettingsMixin {
        @ModifyReturnValue(method = {"one", "two", "absent"}, at = {@At("RETURN"), @At("RETURN")})
        private static int count(int value) {
            return value + 10;
        }

        @ModifyReturnValue(method = "absent", at = @At("RETURN"))
        private static int missingMethod(int value) {
            return value;
        }

        @ModifyReturnValue(method = "one", at = @At(value = "RETURN", ordinal = 4))
        private static int missingPoint(int value) {
            return value;
        }

        @ModifyExpressionValue(method = "values", at = {
                @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", slice = "first"),
                @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", slice = "rest")},
                slice = {
                        @Slice(id = "first", to = @At(value = "INVOKE:FIRST", target = "Ljava/lang/Math;abs(I)I")),
                        @Slice(id = "rest", from = @At(value = "INVOKE:FIRST", target = "Ljava/lang/Math;abs(I)I"),
                                to = @At(value = "INVOKE:LAST", target = "Ljava/lang/Math;abs(I)I"))},
                require = 3, allow = 3)
        private static int slices(int value) {
            return value + 10;
        }

        @ModifyReturnValue(method = "one", at = @At(value = "RETURN", slice = "missing"))
        private static int unknownSlice(int value) {
            return value;
        }

        @ModifyReturnValue(method = "one", at = @At("RETURN"), slice = {@Slice(id = "a"), @Slice(id = "a")})
        private static int duplicateSlice(int value) {
            return value;
        }

        @ModifyExpressionValue(method = "values", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"),
                slice = @Slice(from = @At(value = "INVOKE:ONE", target = "Ljava/lang/Math;abs(I)I")))
        private static int ambiguousBoundary(int value) {
            return value;
        }

        @ModifyExpressionValue(method = "field", at = @At(value = "FIELD", target = TARGET + "number:I", opcode = Opcodes.GETSTATIC, ordinal = 0), require = 1, allow = 1)
        private static int field(int value) {
            return value + 3;
        }

        @ModifyConstant(method = "values", constant = @Constant(intValue = 2, slice = "middle"), require = 1,
                slice = {@Slice(id = "unused"), @Slice(id = "middle", from = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", ordinal = 0),
                        to = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", ordinal = 1))})
        private static int constant(int value) {
            return value + 3;
        }

        @ModifyReturnValue(method = "operation", at = @At("RETURN"), order = 200)
        private static int lateReturn(int value) {
            return value * 10;
        }

        @ModifyReturnValue(method = "operation", at = @At("RETURN"), order = 100)
        private static int earlyReturn(int value) {
            return value + 1;
        }

        @ModifyExpressionValue(method = "operation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), order = -100)
        private static int expression(int value) {
            return value * 2;
        }

        @ModifyExpressionValue(method = "operation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), order = -200)
        private static int earlierExpression(int value) {
            return value + 1;
        }

        @Redirect(method = "operation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), order = 30000)
        private static int redirect(int value) {
            return 3;
        }

        @ModifyReturnValue(method = "one", at = @At("RETURN"))
        private static int equalAdd(int value) {
            return value + 1;
        }

        @ModifyReturnValue(method = "one", at = @At("RETURN"))
        private static int equalMultiply(int value) {
            return value * 10;
        }

        @Redirect(method = "write", at = @At(value = "INVOKE", target = TARGET + "consume(J)V"))
        private void redirectWrite(long value) {
            Target.written = value * 2;
        }

        @WrapWithCondition(method = "write", at = @At(value = "INVOKE", target = TARGET + "consume(J)V"), require = 1)
        private boolean conditionWrite(long value) {
            Target.seen = value;
            return false;
        }

        @ModifyExpressionValue(method = "oldOperation", at = @At(value = "INVOKE", target = "Lold/Math;oldAbs(I)I"), require = 1)
        private static int mapped(int value) {
            return value + 1;
        }

        @ModifyExpressionValue(method = "operation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), remap = false, require = 1)
        private static int unmapped(int value) {
            return value + 1;
        }

        @ModifyExpressionValue(method = "operation", at = @At(value = "INVOKE", target = "Lold/Math;oldAbs(I)I", remap = true), remap = false, require = 1)
        private static int nestedMapped(int value) {
            return value + 1;
        }

        @ModifyExpressionValue(method = "oldOperation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", remap = false), require = 1)
        private static int nestedUnmapped(int value) {
            return value + 1;
        }

        @ModifyExpressionValue(method = "oldOperation", at = @At(value = "INVOKE", target = "Lold/Math;oldAbs(I)I"),
                slice = @Slice(from = @At(value = "INVOKE", target = "Lold/Math;oldAbs(I)I")), require = 1)
        private static int mappedSlice(int value) {
            return value + 1;
        }

        @WrapWithCondition(method = "write", at = @At(value = "INVOKE", target = TARGET + "consume(J)V"))
        private boolean conditionPhase(long value) {
            return false;
        }

        @Inject(method = "write", at = @At(value = "INVOKE", target = TARGET + "consume(J)V"), order = 2000, require = 1)
        private void regularPhase() {
            Target.number++;
        }

        @Inject(method = "steps", at = @At(value = "INVOKE", target = TARGET + "second()V"), require = 1)
        private static void shift() {
            Target.number = Target.number * 10 + 9;
        }

        @Inject(method = "branch", at = @At("JUMP"), require = 1, allow = 1)
        private static void jump() {
            Target.number++;
        }

        @Inject(method = "steps", at = @At(value = "HEAD", shift = At.Shift.BY, by = -10))
        private static void invalidShift() {
        }

        @WrapWithCondition(method = "strings", at = @At(value = "INVOKE_STRING", target = TARGET + "string(Ljava/lang/String;)V", args = "ldc=second"), require = 1, allow = 1)
        private static boolean stringCondition(String value) {
            return false;
        }

        @Inject(method = "assigned", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/lang/Math;abs(I)I"), require = 1)
        private static void assignment(@Local(ordinal = 0) int value) {
            Target.number = value;
        }

        @Redirect(method = "text", at = @At(value = "INVOKE", target = "Ljava/lang/String;trim()Ljava/lang/String;"))
        private static Object redirectString(String original) {
            return "redirected";
        }

        @ModifyExpressionValue(method = "text", at = @At(value = "INVOKE", target = "Ljava/lang/String;trim()Ljava/lang/String;"), require = 1)
        private static String stringExpression(String value) {
            return value + "!";
        }

        @Redirect(method = "fieldWrite", at = @At(value = "FIELD", target = TARGET + "instanceValue:J", opcode = Opcodes.PUTFIELD))
        private void redirectField(Target receiver, long value) {
            receiver.instanceValue = value * 2;
        }

        @WrapWithCondition(method = "fieldWrite", at = @At(value = "FIELD", target = TARGET + "instanceValue:J", opcode = Opcodes.PUTFIELD), require = 1)
        private boolean conditionField(Target receiver, long value) {
            return Target.number != 0;
        }

        @ModifyExpressionValue(method = "classValue", at = @At(value = "CONSTANT", args = "classValue=java.lang.String"), require = 1)
        private static Class<?> classExpression(Class<?> value) {
            return Double.class;
        }

        @ModifyConstant(method = "classValue", constant = @Constant(classValue = String.class), require = 1)
        private static Class<?> classConstant(Class<?> value) {
            return Double.class;
        }
    }

}
