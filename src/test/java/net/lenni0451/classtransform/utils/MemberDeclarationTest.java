package net.lenni0451.classtransform.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.junit.jupiter.api.Assertions.*;

class MemberDeclarationTest {

    @ParameterizedTest
    @CsvSource({
            "net/lenni0451/classtransform/utils/MemberDeclaration, getOwner, ()Ljava/lang/String;, true",
            "net/lenni0451/classtransform/utils/MemberDeclaration, owner, Ljava/lang/String;, false"
    })
    @DisplayName("Check return values")
    public void checkReturnValues(final String owner, final String name, final String desc, final boolean method) {
        MemberDeclaration memberDeclaration = new MemberDeclaration(owner, name, desc);
        assertEquals(owner, memberDeclaration.getOwner());
        assertEquals(name, memberDeclaration.getName());
        assertEquals(desc, memberDeclaration.getDesc());

        if (method) {
            MethodInsnNode methodInsnNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc);
            assertTrue(memberDeclaration.is(methodInsnNode));
            assertFalse(memberDeclaration.isFieldMapping());
        } else {
            FieldInsnNode fieldInsnNode = new FieldInsnNode(Opcodes.GETSTATIC, owner, name, desc);
            assertTrue(memberDeclaration.is(fieldInsnNode));
            assertTrue(memberDeclaration.isFieldMapping());
        }
    }

}
