package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintsTest {

    private TransformerManager transformerManager;

    @BeforeEach
    void setUp() {
        this.transformerManager = new TransformerManager(new BasicClassProvider());
        this.transformerManager.registerConstraintToken("TEST", 5);
    }

    @Test
    @DisplayName("Exact constraints")
    public void exactConstraints() {
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(5)"));
        assertThrows(IllegalArgumentException.class, () -> Constraints.check(this.transformerManager, "TEST(4)"));
    }

    @Test
    @DisplayName("Leading range constraints")
    public void leadingRangeConstraints() {
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(>4)"));
        assertThrows(IllegalArgumentException.class, () -> Constraints.check(this.transformerManager, "TEST(>5)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(>=5)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(<6)"));
        assertThrows(IllegalArgumentException.class, () -> Constraints.check(this.transformerManager, "TEST(<5)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(<=5)"));
    }

    @Test
    @DisplayName("Trailing range constraints")
    public void trailingRangeConstraints() {
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(5-)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(5+)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(5>)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(5<)"));
        assertThrows(IllegalArgumentException.class, () -> Constraints.check(this.transformerManager, "TEST(4<)"));
    }

    @Test
    @DisplayName("Bounded range constraints")
    public void boundedRangeConstraints() {
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(1-5)"));
        assertDoesNotThrow(() -> Constraints.check(this.transformerManager, "TEST(5-10)"));
        assertThrows(IllegalArgumentException.class, () -> Constraints.check(this.transformerManager, "TEST(6-10)"));
    }

}
