package net.lenni0451.classtransform.utils.annotations;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationUtilsTest {

    private static final Class<Override> OVERRIDE_CLASS = Override.class;
    private static final String OVERRIDE_DESCRIPTOR = typeDescriptor(Override.class);
    private static final Class<Deprecated> DEPRECATED_CLASS = Deprecated.class;
    private static final String DEPRECATED_DESCRIPTOR = typeDescriptor(Deprecated.class);

    private static List<AnnotationNode> annotations;
    private static ClassNode classNode;
    private static FieldNode fieldNode;
    private static MethodNode methodNode;

    @BeforeAll
    static void setUp() {
        annotations = new ArrayList<>();
        annotations.add(new AnnotationNode(OVERRIDE_DESCRIPTOR));
        classNode = new ClassNode();
        classNode.visibleAnnotations = annotations;
        classNode.invisibleAnnotations = annotations;
        fieldNode = new FieldNode(0, null, null, null, null);
        fieldNode.visibleAnnotations = annotations;
        fieldNode.invisibleAnnotations = annotations;
        methodNode = new MethodNode();
        methodNode.visibleAnnotations = annotations;
        methodNode.invisibleAnnotations = annotations;
    }

    @Test
    void findInvisibleAnnotationInClass() {
        assertTrue(AnnotationUtils.findInvisibleAnnotation(classNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleAnnotation(classNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(classNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(classNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findInvisibleAnnotationInField() {
        assertTrue(AnnotationUtils.findInvisibleAnnotation(fieldNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleAnnotation(fieldNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(fieldNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(fieldNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findInvisibleAnnotationInMethod() {
        assertTrue(AnnotationUtils.findInvisibleAnnotation(methodNode, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findInvisibleAnnotation(methodNode, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(methodNode, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findInvisibleAnnotation(methodNode, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void findAnnotation() {
        assertTrue(AnnotationUtils.findAnnotation(annotations, OVERRIDE_CLASS).isPresent());
        assertTrue(AnnotationUtils.findAnnotation(annotations, OVERRIDE_DESCRIPTOR).isPresent());
        assertFalse(AnnotationUtils.findAnnotation(annotations, DEPRECATED_CLASS).isPresent());
        assertFalse(AnnotationUtils.findAnnotation(annotations, DEPRECATED_DESCRIPTOR).isPresent());
    }

    @Test
    void classHasInvisibleAnnotation() {
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(classNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(classNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(classNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(classNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void fieldHasInvisibleAnnotation() {
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(fieldNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(fieldNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(fieldNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(fieldNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void methodHasInvisibleAnnotation() {
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(methodNode, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasInvisibleAnnotation(methodNode, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(methodNode, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasInvisibleAnnotation(methodNode, DEPRECATED_DESCRIPTOR));
    }

    @Test
    void hasAnnotation() {
        assertTrue(AnnotationUtils.hasAnnotation(annotations, OVERRIDE_CLASS));
        assertTrue(AnnotationUtils.hasAnnotation(annotations, OVERRIDE_DESCRIPTOR));
        assertFalse(AnnotationUtils.hasAnnotation(annotations, DEPRECATED_CLASS));
        assertFalse(AnnotationUtils.hasAnnotation(annotations, DEPRECATED_DESCRIPTOR));
    }

}
