package net.lenni0451.classtransform.utils.annotations;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationUtilsTest {

    private static final Class<Override> OVERRIDE_CLASS = Override.class;
    private static final String OVERRIDE_DESCRIPTOR = typeDescriptor(Override.class);
    private static final Class<Deprecated> DEPRECATED_CLASS = Deprecated.class;
    private static final String DEPRECATED_DESCRIPTOR = typeDescriptor(Deprecated.class);

    private static List<AnnotationNode> visibleAnnotations;
    private static List<AnnotationNode> invisibleAnnotations;
    private static List<AnnotationNode>[] visibleParameterAnnotations;
    private static List<AnnotationNode>[] invisibleParameterAnnotations;
    private static ClassNode classNode;
    private static FieldNode fieldNode;
    private static MethodNode methodNode;

    @BeforeAll
    static void setUp() {
        visibleAnnotations = Collections.singletonList(new AnnotationNode(DEPRECATED_DESCRIPTOR));
        invisibleAnnotations = Collections.singletonList(new AnnotationNode(OVERRIDE_DESCRIPTOR));
        visibleParameterAnnotations = new List[]{visibleAnnotations};
        invisibleParameterAnnotations = new List[]{invisibleAnnotations};

        classNode = new ClassNode();
        classNode.visibleAnnotations = visibleAnnotations;
        classNode.invisibleAnnotations = invisibleAnnotations;

        fieldNode = new FieldNode(0, null, null, null, null);
        fieldNode.visibleAnnotations = visibleAnnotations;
        fieldNode.invisibleAnnotations = invisibleAnnotations;

        methodNode = new MethodNode();
        methodNode.visibleAnnotations = visibleAnnotations;
        methodNode.invisibleAnnotations = invisibleAnnotations;
        methodNode.visibleParameterAnnotations = visibleParameterAnnotations;
        methodNode.invisibleParameterAnnotations = invisibleParameterAnnotations;
    }

    @Test
    void forEach() {
        AnnotationUtils.forEachVisible(classNode, annotation -> assertEquals(annotation.desc, DEPRECATED_DESCRIPTOR));
        AnnotationUtils.forEachVisible(fieldNode, annotation -> assertEquals(annotation.desc, DEPRECATED_DESCRIPTOR));
        AnnotationUtils.forEachVisible(methodNode, annotation -> assertEquals(annotation.desc, DEPRECATED_DESCRIPTOR));

        AnnotationUtils.forEachInvisible(classNode, annotation -> assertEquals(annotation.desc, OVERRIDE_DESCRIPTOR));
        AnnotationUtils.forEachInvisible(fieldNode, annotation -> assertEquals(annotation.desc, OVERRIDE_DESCRIPTOR));
        AnnotationUtils.forEachInvisible(methodNode, annotation -> assertEquals(annotation.desc, OVERRIDE_DESCRIPTOR));

        int[] count = {0};
        AnnotationUtils.forEach(classNode, annotation -> {
            if (count[0] == 0) assertEquals(annotation.desc, DEPRECATED_DESCRIPTOR);
            else if (count[0] == 1) assertEquals(annotation.desc, OVERRIDE_DESCRIPTOR);
            else fail("Too many annotations");
            count[0]++;
        });

        count[0] = 0;
        AnnotationUtils.forEach(fieldNode, annotation -> {
            if (count[0] == 0) assertEquals(annotation.desc, DEPRECATED_DESCRIPTOR);
            else if (count[0] == 1) assertEquals(annotation.desc, OVERRIDE_DESCRIPTOR);
            else fail("Too many annotations");
            count[0]++;
        });

        count[0] = 0;
        AnnotationUtils.forEach(methodNode, annotation -> {
            if (count[0] == 0) assertEquals(annotation.desc, DEPRECATED_DESCRIPTOR);
            else if (count[0] == 1) assertEquals(annotation.desc, OVERRIDE_DESCRIPTOR);
            else fail("Too many annotations");
            count[0]++;
        });
    }

    @Test
    void findAnnotationInClass() {
        assertFalse(AnnotationUtils.findVisibleAnnotation(classNode, OVERRIDE_CLASS).isPresent());
        assertFalse(AnnotationUtils.findVisibleAnnotation(classNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findVisibleAnnotation(classNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findVisibleAnnotation(classNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findInvisibleAnnotation(classNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleAnnotation(classNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(classNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(classNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findAnnotation(classNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(classNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(classNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(classNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findAnnotationInField() {
        assertFalse(AnnotationUtils.findVisibleAnnotation(fieldNode, OVERRIDE_CLASS).isPresent());
        assertFalse(AnnotationUtils.findVisibleAnnotation(fieldNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findVisibleAnnotation(fieldNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findVisibleAnnotation(fieldNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findInvisibleAnnotation(fieldNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleAnnotation(fieldNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(fieldNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(fieldNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findAnnotation(fieldNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(fieldNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(fieldNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(fieldNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findAnnotationInMethod() {
        assertFalse(AnnotationUtils.findVisibleAnnotation(methodNode, OVERRIDE_CLASS).isPresent());
        assertFalse(AnnotationUtils.findVisibleAnnotation(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findVisibleAnnotation(methodNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findVisibleAnnotation(methodNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findInvisibleAnnotation(methodNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleAnnotation(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(methodNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(methodNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findAnnotation(methodNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(methodNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(methodNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findParameterAnnotationsInMethod() {
        assertFalse(AnnotationUtils.findVisibleParameterAnnotations(methodNode, OVERRIDE_CLASS).isPresent());
        assertFalse(AnnotationUtils.findVisibleParameterAnnotations(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findVisibleParameterAnnotations(methodNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findVisibleParameterAnnotations(methodNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findInvisibleParameterAnnotations(methodNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleParameterAnnotations(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleParameterAnnotations(methodNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleParameterAnnotations(methodNode, DEPRECATED_DESCRIPTOR).isPresent());

        assertTrue(AnnotationUtils.findParameterAnnotations(methodNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findParameterAnnotations(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertTrue(AnnotationUtils.findParameterAnnotations(methodNode, DEPRECATED_CLASS).isPresent());
        assertTrue(AnnotationUtils.findParameterAnnotations(methodNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findAnnotation() {
        assertTrue(AnnotationUtils.findAnnotation(invisibleAnnotations, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(invisibleAnnotations, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findAnnotation(invisibleAnnotations, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findAnnotation(invisibleAnnotations, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void classHasAnnotation() {
        assertFalse(AnnotationUtils.hasVisibleAnnotation(classNode, OVERRIDE_CLASS));
        assertFalse(AnnotationUtils.hasVisibleAnnotation(classNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasVisibleAnnotation(classNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasVisibleAnnotation(classNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasInvisibleAnnotation(classNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(classNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(classNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(classNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasAnnotation(classNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(classNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasAnnotation(classNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(classNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void fieldHasAnnotation() {
        assertFalse(AnnotationUtils.hasVisibleAnnotation(fieldNode, OVERRIDE_CLASS));
        assertFalse(AnnotationUtils.hasVisibleAnnotation(fieldNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasVisibleAnnotation(fieldNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasVisibleAnnotation(fieldNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasInvisibleAnnotation(fieldNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(fieldNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(fieldNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(fieldNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasAnnotation(fieldNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(fieldNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasAnnotation(fieldNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(fieldNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void methodHasAnnotation() {
        assertFalse(AnnotationUtils.hasVisibleAnnotation(methodNode, OVERRIDE_CLASS));
        assertFalse(AnnotationUtils.hasVisibleAnnotation(methodNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasVisibleAnnotation(methodNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasVisibleAnnotation(methodNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasInvisibleAnnotation(methodNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(methodNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(methodNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(methodNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasAnnotation(methodNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(methodNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasAnnotation(methodNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(methodNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void methodHasParameterAnnotations() {
        assertFalse(AnnotationUtils.hasVisibleParameterAnnotations(methodNode, OVERRIDE_CLASS));
        assertFalse(AnnotationUtils.hasVisibleParameterAnnotations(methodNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasVisibleParameterAnnotations(methodNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasVisibleParameterAnnotations(methodNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasInvisibleParameterAnnotations(methodNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleParameterAnnotations(methodNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleParameterAnnotations(methodNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleParameterAnnotations(methodNode, DEPRECATED_DESCRIPTOR));

        assertTrue(AnnotationUtils.hasParameterAnnotations(methodNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasParameterAnnotations(methodNode, OVERRIDE_DESCRIPTOR));
        assertTrue(AnnotationUtils.hasParameterAnnotations(methodNode, DEPRECATED_CLASS));
        assertTrue(AnnotationUtils.hasParameterAnnotations(methodNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void hasAnnotation() {
        assertTrue(AnnotationUtils.hasAnnotation(invisibleAnnotations, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(invisibleAnnotations, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasAnnotation(invisibleAnnotations, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasAnnotation(invisibleAnnotations, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void hasParameterAnnotations() {
        assertTrue(AnnotationUtils.hasParameterAnnotations(invisibleParameterAnnotations, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasParameterAnnotations(invisibleParameterAnnotations, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasParameterAnnotations(invisibleParameterAnnotations, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasParameterAnnotations(invisibleParameterAnnotations, DEPRECATED_DESCRIPTOR));
    }

}
