package org.spongepowered.asm.mixin.injection.callback;

/**
 * Compile-time stub of Mixin's callback handle that carries a typed return value.
 * <p>
 * Handlers use it to read the target method's current result and to provide a replacement value when
 * the callback is cancelled. The stub has no behavior; the Mixin translator replaces it with
 * ClassTransform's injection callback.
 *
 * @param <R> The return type of the target method
 */
public class CallbackInfoReturnable<R> extends CallbackInfo {

    CallbackInfoReturnable() {
    }

    /**
     * Sets the value the target method returns and marks the callback as cancelled.
     *
     * @param returnValue The replacement return value
     */
    public void setReturnValue(R returnValue) {
    }

    /**
     * Returns the current return value as the target method's type.
     *
     * @return The current return value
     */
    public R getReturnValue() {
        return null;
    }

    /**
     * Returns the current return value as a {@code byte}.
     *
     * @return The return value as a byte
     */
    public byte getReturnValueB() {
        return 0;
    }

    /**
     * Returns the current return value as a {@code char}.
     *
     * @return The return value as a char
     */
    public char getReturnValueC() {
        return '\0';
    }

    /**
     * Returns the current return value as a {@code double}.
     *
     * @return The return value as a double
     */
    public double getReturnValueD() {
        return 0;
    }

    /**
     * Returns the current return value as a {@code float}.
     *
     * @return The return value as a float
     */
    public float getReturnValueF() {
        return 0;
    }

    /**
     * Returns the current return value as an {@code int}.
     *
     * @return The return value as an int
     */
    public int getReturnValueI() {
        return 0;
    }

    /**
     * Returns the current return value as a {@code long}.
     *
     * @return The return value as a long
     */
    public long getReturnValueJ() {
        return 0;
    }

    /**
     * Returns the current return value as a {@code short}.
     *
     * @return The return value as a short
     */
    public short getReturnValueS() {
        return 0;
    }

    /**
     * Returns the current return value as a {@code boolean}.
     *
     * @return The return value as a boolean
     */
    public boolean getReturnValueZ() {
        return false;
    }

}
