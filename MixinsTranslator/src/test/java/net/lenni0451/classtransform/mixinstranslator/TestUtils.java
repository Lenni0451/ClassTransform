package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {

    public static void compareAnnotations(final AnnotationNode annotation, final AnnotationNode expected) {
        Map<String, Object> annotationValues = AnnotationUtils.listToMap(annotation.values);
        Map<String, Object> expectedValues = AnnotationUtils.listToMap(expected.values);
        for (Map.Entry<String, Object> entry : expectedValues.entrySet()) {
            Object value = annotationValues.get(entry.getKey());
            Object expectedValue = entry.getValue();
            if (value == null && expectedValue == null) continue;
            if (value == null || expectedValue == null) throw new AssertionError("Values do not match: " + entry.getKey() + " - " + value + " != " + expectedValue);
            assertEquals(expectedValue.getClass(), value.getClass(), "Value types do not match: " + entry.getKey() + " - " + value + " != " + expectedValue);
            compareObject(value, expectedValue);
        }
    }

    private static void compareObject(final Object object, final Object expected) {
        if (expected instanceof AnnotationNode) {
            compareAnnotations((AnnotationNode) object, (AnnotationNode) expected);
        } else if (expected.getClass().isArray()) {
            int expectedLength = Array.getLength(expected);
            int length = Array.getLength(object);
            assertEquals(expectedLength, length);
            for (int i = 0; i < expectedLength; i++) {
                compareObject(Array.get(object, i), Array.get(expected, i));
            }
        } else if (expected instanceof List) {
            List<?> expectedList = (List<?>) expected;
            List<?> list = (List<?>) object;
            assertEquals(expectedList.size(), list.size());
            for (int i = 0; i < expectedList.size(); i++) {
                compareObject(list.get(i), expectedList.get(i));
            }
        } else {
//                System.out.println("Checking: " + entry.getKey() + " - " + value + " == " + expectedValue);
            assertEquals(expected, object);
        }
    }

    public static <T extends Annotation> T findParameterAnnotation(final Class<T> clazz, final Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(clazz)) return (T) annotation;
        }
        throw new AssertionError("Annotation not found: " + clazz.getName());
    }

    public static AnnotationNode findParameterAnnotationNode(final MethodNode methodNode, int parameterIndex, final Class<? extends Annotation> clazz) {
        return AnnotationUtils.findParameterAnnotations(methodNode, clazz).orElseThrow(() -> new AssertionError("Annotation not found: " + clazz.getName()))[parameterIndex];
    }

}
