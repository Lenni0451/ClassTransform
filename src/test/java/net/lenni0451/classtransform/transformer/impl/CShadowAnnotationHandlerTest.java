package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.transformer.AnnotationHandlerTest;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CShadowAnnotationHandlerTest extends AnnotationHandlerTest {

    private final CShadowAnnotationHandler transformer = new CShadowAnnotationHandler();

    @Test
    @DisplayName("Strip shadow method prefix")
    public void stripShadowMethodPrefix() throws ClassNotFoundException {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CShadowAnnotationHandlerTest$SPrefixShadowTestTransformer");
        assertDoesNotThrow(() -> this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer));
        assertTrue(transformer.methods.stream().noneMatch(method -> method.name.equals("shadow$getPi")));
    }

    @Test
    @DisplayName("Use shadow alias when the main target is missing")
    public void useShadowAliasWhenMainTargetIsMissing() {
        ClassNode target = ASMUtils.fromBytes(assertDoesNotThrow(() -> this.classProvider.getClass(SCalculator.class.getName())));
        ClassNode transformer = ASMUtils.createEmptyClass("AliasTransformer");

        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE, "shadow$getPi", "()D", null, null);
        AnnotationNode shadow = new AnnotationNode(Type.getDescriptor(CShadow.class));
        shadow.values = new ArrayList<>(Arrays.asList(
                "value", "missing()D",
                "aliases", new ArrayList<>(Arrays.asList("getPi()D"))
        ));
        method.invisibleAnnotations = new ArrayList<>();
        method.invisibleAnnotations.add(shadow);
        transformer.methods.add(method);

        assertDoesNotThrow(() -> this.transformer.transform(this.transformerManager, target, transformer));
        assertFalse(transformer.methods.contains(method));
    }


    @CTransformer(SCalculator.class)
    private static class SPrefixShadowTestTransformer {

        @CShadow(prefix = "shadow$", makePublic = true)
        private static native double shadow$getPi();

    }

}
