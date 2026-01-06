package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static net.lenni0451.classtransform.utils.Types.methodDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParameterAnnotationTest {

    private native void testLocal(
            @Local(ordinal = 10, index = 5, name = "test") @CLocalVariable(name = "test", ordinal = 10, index = 5, modifiable = true) @TestMarker(from = Local.class, to = CLocalVariable.class) String test
    );

    private native void testShare(
            @Share(value = "test") @CShared(value = "test") @TestMarker(from = Share.class, to = CShared.class) String test2
    );

    @Test
    void test() throws ClassNotFoundException {
        ClassNode classNode = ASMUtils.fromBytes(new BasicClassProvider().getClass(ParameterAnnotationTest.class.getName()));
        for (Method method : ParameterAnnotationTest.class.getDeclaredMethods()) {
            boolean isTestMethod = false;
            for (Annotation[] parameterAnnotation : method.getParameterAnnotations()) {
                for (Annotation annotation : parameterAnnotation) {
                    if (annotation.annotationType().equals(TestMarker.class)) {
                        isTestMethod = true;
                        break;
                    }
                }
            }
            if (!isTestMethod) continue;

            MethodNode methodNode = ASMUtils.getMethod(classNode, method.getName(), methodDescriptor(method));
            for (int i = 0; i < method.getParameterAnnotations().length; i++) {
                Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                TestMarker testMarker = TestUtils.findParameterAnnotation(TestMarker.class, parameterAnnotations);
                System.out.println("Testing parameter " + i + " From: " + Arrays.toString(testMarker.from()) + " To: " + Arrays.toString(testMarker.to()));

                ClassNode dummyClass = ASMUtils.createEmptyClass("Dummy");
                dummyClass.visibleAnnotations = new ArrayList<>();
                for (Class<? extends Annotation> from : testMarker.from()) {
                    AnnotationNode annotation = TestUtils.findParameterAnnotationNode(methodNode, i, from);
                    dummyClass.visibleAnnotations.add(AnnotationUtils.clone(annotation));
                }
                new MixinsTranslator().process(dummyClass);
                assertEquals(testMarker.to().length, dummyClass.visibleAnnotations.size());
                for (Class<? extends Annotation> to : testMarker.to()) {
                    AnnotationNode annotation = AnnotationUtils.findAnnotation(dummyClass, to).orElseThrow(() -> new AssertionError("Annotation not found after processing: " + to.getName()));
                    AnnotationNode expected = TestUtils.findParameterAnnotationNode(methodNode, i, to);
                    TestUtils.compareAnnotations(annotation, expected);
                }
            }
        }
    }

}
