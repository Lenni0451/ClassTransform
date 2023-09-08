package net.lenni0451.classtransform.utils.tree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassTreeTest {

    @Test
    @DisplayName("Check super classes")
    public void checkSuperClasses() throws ClassNotFoundException {
        ClassTree.TreePart tree = new ClassTree().getTreePart(new BasicClassProvider(), "java.lang.reflect.Method");
        Set<String> superClasses = tree.getSuperClasses();
        assertEquals(6, superClasses.size());
        assertTrue(superClasses.contains("java.lang.Object"));
        assertTrue(superClasses.contains("java.lang.reflect.Executable"));
        assertTrue(superClasses.contains("java.lang.reflect.AnnotatedElement"));
        assertTrue(superClasses.contains("java.lang.reflect.GenericDeclaration"));
        assertTrue(superClasses.contains("java.lang.reflect.AccessibleObject"));
        assertTrue(superClasses.contains("java.lang.reflect.Member"));
    }

}
