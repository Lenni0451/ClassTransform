package net.lenni0451.classtransform.utils.attributes;

import net.lenni0451.classtransform.utils.attributes.SharedVariableAttribute.SharedVariable;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SharedVariableAttributeTest {

    @Test
    public void legacyAddAndLookupStillWork() {
        SharedVariableAttribute attribute = new SharedVariableAttribute();
        SharedVariable variable = attribute.addVariable("transformer", "local", 3, Type.INT_TYPE, false);

        assertSame(variable, attribute.getVariableIndex("transformer", "local", false));
        assertNull(attribute.getVariableIndex("other", "local", false));
        assertNull(attribute.getVariable("transformer", "local", "namespace", false));
    }

    @Test
    public void legacyGlobalLookupIgnoresTransformer() {
        SharedVariableAttribute attribute = new SharedVariableAttribute();
        SharedVariable variable = attribute.addVariable("transformer", "local", 3, Type.INT_TYPE, true);

        assertSame(variable, attribute.getVariableIndex("other", "local", true));
    }

}
