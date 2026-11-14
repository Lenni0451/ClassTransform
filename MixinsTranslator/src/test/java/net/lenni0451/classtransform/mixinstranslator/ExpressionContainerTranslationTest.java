package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CDefinitions;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.expression.CExpressions;
import net.lenni0451.classtransform.annotations.expression.CLocalDefinition;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExpressionContainerTranslationTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void translateRepeatedExpressions() throws Exception {
        ClassNode node = this.translatedMixin();
        MethodNode method = ASMUtils.getMethod(node, "modify", "(I)I");
        assertNotNull(method);

        AnnotationNode container = AnnotationUtils.findInvisibleAnnotation(method, CExpressions.class)
                .orElseThrow(() -> new AssertionError("Translated @Expressions annotation not found"));
        List<?> expressions = (List<?>) AnnotationUtils.listToMap(container.values).get("value");
        assertEquals(2, expressions.size());
        for (Object expression : expressions) {
            AnnotationNode annotation = assertInstanceOf(AnnotationNode.class, expression);
            assertEquals(Type.getDescriptor(CExpression.class), annotation.desc);
        }
    }

    @Test
    public void translateRepeatedDefinitionsAndSingleLocal() throws Exception {
        ClassNode node = this.translatedMixin();
        MethodNode method = ASMUtils.getMethod(node, "modify", "(I)I");
        assertNotNull(method);

        AnnotationNode container = AnnotationUtils.findInvisibleAnnotation(method, CDefinitions.class)
                .orElseThrow(() -> new AssertionError("Translated @Definitions annotation not found"));
        List<?> definitions = (List<?>) AnnotationUtils.listToMap(container.values).get("value");
        assertEquals(2, definitions.size());

        AnnotationNode localDefinition = null;
        for (Object definition : definitions) {
            AnnotationNode annotation = assertInstanceOf(AnnotationNode.class, definition);
            assertEquals(Type.getDescriptor(CDefinition.class), annotation.desc);
            Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
            if ("local".equals(values.get("id"))) localDefinition = annotation;
        }
        assertNotNull(localDefinition, "Local definition was not translated");

        List<?> locals = (List<?>) AnnotationUtils.listToMap(localDefinition.values).get("local");
        assertEquals(1, locals.size());
        AnnotationNode local = assertInstanceOf(AnnotationNode.class, locals.get(0));
        assertEquals(Type.getDescriptor(CLocalDefinition.class), local.desc);
        assertEquals(Arrays.asList("value"), AnnotationUtils.listToMap(local.values).get("names"));
    }

    private ClassNode translatedMixin() throws ClassNotFoundException {
        ClassNode node = ASMUtils.fromBytes(this.classProvider.getClass(ContainerMixin.class.getName()));
        new MixinsTranslator().process(node);
        return node;
    }


    public static class Target {

        public static int compute(final int value) {
            return value * 2;
        }

    }

    @Mixin(Target.class)
    public static class ContainerMixin {

        @Definition(id = "compute", method = "compute(I)I")
        @Definition(id = "local", local = @Local(type = int.class, name = "value"))
        @Expression("compute(?)")
        @Expression("local")
        private static int modify(final int value) {
            return value;
        }

    }

}
