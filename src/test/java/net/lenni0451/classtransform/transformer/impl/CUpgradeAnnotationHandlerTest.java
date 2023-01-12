package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.CUpgrade;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.transformer.AnnotationHandlerTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

class CUpgradeAnnotationHandlerTest extends AnnotationHandlerTest {

    private final CUpgradeAnnotationHandler transformer = new CUpgradeAnnotationHandler();

    @Test
    @DisplayName("Downgrade class version")
    public void downgradeClassVersion() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CUpgradeTransformerTest$SUpgradeTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Assertions.assertEquals(Opcodes.V1_8, this.staticCalculatorClass.version);
    }

    @Test
    @DisplayName("Upgrade class version")
    public void upgradeClassVersion() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CUpgradeTransformerTest$SDowngradeTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Assertions.assertEquals(Opcodes.V17, this.staticCalculatorClass.version);
    }


    @CTransformer(SCalculator.class)
    @CUpgrade(Opcodes.V1_2)
    private static class SUpgradeTestTransformer {
    }

    @CTransformer(SCalculator.class)
    @CUpgrade(Opcodes.V17)
    private static class SDowngradeTestTransformer {
    }

}