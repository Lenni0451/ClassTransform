package net.lenni0451.classtransform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InjectionCallbackTest {

    @Test
    @DisplayName("Cancel a cancellable callback")
    @Order(0)
    public void cancelCancellableCallback() {
        InjectionCallback callback = new InjectionCallback(true);
        callback.setCancelled(true);
        assertTrue(callback.isCancelled());
    }

    @Test
    @DisplayName("Cancel a non-cancellable callback")
    @Order(0)
    public void cancelNonCancellableCallback() {
        InjectionCallback callback = new InjectionCallback(false);
        assertThrows(IllegalArgumentException.class, () -> callback.setCancelled(true));
        assertFalse(callback.isCancelled());
    }

    @Test
    @DisplayName("Set a return value of a cancellable callback")
    @Order(1)
    public void setReturnValue() {
        InjectionCallback callback = new InjectionCallback(true);
        callback.setReturnValue("test");
        assertEquals("test", callback.getReturnValue());
    }

    @Test
    @DisplayName("Set a return value of a non-cancellable callback")
    @Order(1)
    public void setReturnValueNonCancellable() {
        InjectionCallback callback = new InjectionCallback(false);
        assertThrows(IllegalArgumentException.class, () -> callback.setReturnValue("test"));
        assertFalse(callback.isCancelled());
        assertThrows(IllegalStateException.class, callback::getReturnValue);
    }

    @Test
    @DisplayName("Cast a return value of a cancellable callback")
    @Order(2)
    public void castReturnValue() {
        InjectionCallback callback = new InjectionCallback(true);
        callback.setReturnValue("test");
        String returnValue = assertDoesNotThrow(() -> callback.castReturnValue());
        assertEquals("test", returnValue);
    }

}
