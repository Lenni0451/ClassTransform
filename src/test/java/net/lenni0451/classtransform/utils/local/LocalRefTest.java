package net.lenni0451.classtransform.utils.local;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalRefTest {

    @Test
    public void objectReference() {
        Object[] values = new Object[]{"first"};
        CLocalRef<String> ref = new CLocalRef<>(values, 0);
        assertEquals("first", ref.get());
        ref.set("second");
        assertEquals("second", values[0]);
    }

    @Test
    public void primitiveReferences() {
        Object[] values = new Object[]{false, (byte) 1, 'a', (short) 2, 3, 4L, 5F, 6D};

        CLocalBooleanRef booleanRef = new CLocalBooleanRef(values, 0);
        assertFalse(booleanRef.get());
        booleanRef.set(true);
        assertEquals(true, values[0]);

        CLocalByteRef byteRef = new CLocalByteRef(values, 1);
        assertEquals(1, byteRef.get());
        byteRef.set((byte) 7);
        assertEquals((byte) 7, values[1]);

        CLocalCharRef charRef = new CLocalCharRef(values, 2);
        assertEquals('a', charRef.get());
        charRef.set('b');
        assertEquals('b', values[2]);

        CLocalShortRef shortRef = new CLocalShortRef(values, 3);
        assertEquals(2, shortRef.get());
        shortRef.set((short) 8);
        assertEquals((short) 8, values[3]);

        CLocalIntRef intRef = new CLocalIntRef(values, 4);
        assertEquals(3, intRef.get());
        intRef.set(9);
        assertEquals(9, values[4]);

        CLocalLongRef longRef = new CLocalLongRef(values, 5);
        assertEquals(4L, longRef.get());
        longRef.set(10L);
        assertEquals(10L, values[5]);

        CLocalFloatRef floatRef = new CLocalFloatRef(values, 6);
        assertEquals(5F, floatRef.get());
        floatRef.set(11F);
        assertEquals(11F, values[6]);

        CLocalDoubleRef doubleRef = new CLocalDoubleRef(values, 7);
        assertEquals(6D, doubleRef.get());
        doubleRef.set(12D);
        assertEquals(12D, values[7]);
    }

}
