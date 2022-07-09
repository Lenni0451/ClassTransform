package net.lenni0451.classtransform.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodifierTest {

    @ParameterizedTest
    @CsvSource({
            "9, java.lang.Integer, test, java.lang.String, java.lang.Throwable, public static Integer test(String string) throws Throwable",
            "0, java.lang.String, toString, java.lang.Object, java.io.IOException, String toString(Object object) throws IOException"
    })
    @DisplayName("Create test methods")
    public void createTestMethods(final int access, final String returnType, final String name, final String parameter, final String exception, final String expected) {
        String generated = Codifier.get()
                .access(access)
                .returnType(Type.getType("L" + returnType + ";"))
                .name(name)
                .params(Type.getType("L" + parameter + ";"))
                .exceptions(Type.getType("L" + exception + ";"))
                .build();
        assertEquals(expected, generated);
    }

}