package net.lenni0451.classtransform.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.lenni0451.classtransform.utils.Types.*;
import static org.junit.jupiter.api.Assertions.*;

class TypesTest {

    @Test
    @DisplayName("Test type")
    public void testType() {
        Type object = Type.getType(Object.class);
        assertEquals(object, type("Ljava/lang/Object;"));
        assertEquals(object, type("java/lang/Object"));
        assertEquals(object, type(Object.class));

        Method method = this.getTestMethod();
        assertEquals(type(method), Type.getType(method));

        Constructor<?> constructor = this.getTestConstructor();
        assertEquals(type(constructor), Type.getType(constructor));

        assertThrows(IllegalArgumentException.class, () -> type(123));
    }

    @Test
    @DisplayName("Test return type")
    public void testReturnType() {
        Method method = this.getTestMethod();
        assertEquals(returnType("()Ljava/lang/String;"), type(String.class));
        assertEquals(returnType(method), type(char.class));
        assertEquals(returnType(method), type(method).getReturnType());
        assertThrows(IllegalArgumentException.class, () -> type(123));
    }

    @Test
    @DisplayName("Test return type")
    public void testMethodDescriptor() {
        assertEquals(methodDescriptor(void.class), "()V");
        assertEquals(methodDescriptor(void.class, int.class), "(I)V");
        assertEquals(methodDescriptor(String.class, int.class), "(I)Ljava/lang/String;");
        assertEquals(methodDescriptor(String.class), "()Ljava/lang/String;");
        assertEquals(methodDescriptor(String.class), "()Ljava/lang/String;");
    }

    private Method getTestMethod() {
        return assertDoesNotThrow(() -> String.class.getDeclaredMethod("charAt", int.class));
    }

    private Constructor<?> getTestConstructor() {
        return assertDoesNotThrow(() -> String.class.getDeclaredConstructor());
    }

}