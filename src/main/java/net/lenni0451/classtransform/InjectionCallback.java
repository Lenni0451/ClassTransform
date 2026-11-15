package net.lenni0451.classtransform;

import net.lenni0451.classtransform.annotations.injection.CInject;

import javax.annotation.Nullable;

/**
 * The callback used to cancel {@link CInject} transformer.
 * <p>
 * A new instance is passed to the transformer method for every injection site invocation when the
 * method declares a parameter of this type. Cancelling skips the remaining original method logic and
 * makes the target return the value held by the callback.
 */
public class InjectionCallback {

    private final boolean cancellable;

    private boolean cancelled;
    private Object returnValue;
    private boolean returnValueSet;

    /**
     * Create a new callback without a preset return value.
     *
     * @param cancellable If the callback allows cancelling the original method
     */
    public InjectionCallback(final boolean cancellable) {
        this.cancellable = cancellable;
    }

    /**
     * Create a new callback with a preset return value.
     *
     * @param cancellable If the callback allows cancelling the original method
     * @param returnValue The initial return value, may be null
     */
    public InjectionCallback(final boolean cancellable, @Nullable final Object returnValue) {
        this.cancellable = cancellable;
        this.returnValue = returnValue;
        this.returnValueSet = true;
    }

    /**
     * Check if the callback allows cancelling the original method.
     *
     * @return If the callback is cancellable
     */
    public boolean isCancellable() {
        return this.cancellable;
    }

    /**
     * Check if the original method should be cancelled.
     *
     * @return If the original method should be cancelled
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Set the original method to be cancelled.
     *
     * @param cancelled If the original method should be cancelled
     * @throws IllegalArgumentException If the callback is not cancellable
     */
    public void setCancelled(final boolean cancelled) {
        if (cancelled && !this.cancellable) throw new IllegalArgumentException("Cannot cancel a non-cancellable callback");
        this.cancelled = cancelled;
    }

    /**
     * Get the current return value of the original method.
     * <p>
     * At the {@code RETURN} and {@code TAIL} targets this is the value being returned. At the
     * {@code THROW} target this is the method return type default, or no value for a void method.
     *
     * @return The current return value of the original method
     * @throws IllegalStateException If no return value has been set
     */
    @Nullable
    public Object getReturnValue() {
        if (!this.returnValueSet) throw new IllegalStateException("Return value not set");
        return this.returnValue;
    }

    /**
     * Get the current return value cast to the wanted type.
     *
     * @param <T> The wanted type
     * @return The current return value of the original method
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not of the wanted type
     */
    @Nullable
    public <T> T castReturnValue() {
        return (T) this.getReturnValue();
    }

    /**
     * Set the return value of the original method.<br>
     * This will also set the cancelled state to true.
     *
     * @param returnValue The new return value, may be null
     * @throws IllegalArgumentException If the callback is not cancellable
     */
    public void setReturnValue(@Nullable final Object returnValue) {
        this.setCancelled(true);
        this.returnValue = returnValue;
        this.returnValueSet = true;
    }

    /**
     * Cancel the original method without setting a return value.
     * <p>
     * For non-void methods a return value should be set with
     * {@link #setReturnValue(Object)}; otherwise reading the return value fails at runtime.
     *
     * @throws IllegalArgumentException If the callback is not cancellable
     */
    public void cancel() {
        this.setCancelled(true);
    }

    /**
     * Get the current return value as a boolean.
     *
     * @return The current return value as a boolean
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed boolean
     */
    public boolean getReturnValueZ() {
        return (Boolean) this.getReturnValue();
    }

    /**
     * Get the current return value as a byte.
     *
     * @return The current return value as a byte
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed byte
     */
    public byte getReturnValueB() {
        return (Byte) this.getReturnValue();
    }

    /**
     * Get the current return value as a char.
     *
     * @return The current return value as a char
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed char
     */
    public char getReturnValueC() {
        return (Character) this.getReturnValue();
    }

    /**
     * Get the current return value as a short.
     *
     * @return The current return value as a short
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed short
     */
    public short getReturnValueS() {
        return (Short) this.getReturnValue();
    }

    /**
     * Get the current return value as an int.
     *
     * @return The current return value as an int
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed int
     */
    public int getReturnValueI() {
        return (Integer) this.getReturnValue();
    }

    /**
     * Get the current return value as a long.
     *
     * @return The current return value as a long
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed long
     */
    public long getReturnValueJ() {
        return (Long) this.getReturnValue();
    }

    /**
     * Get the current return value as a float.
     *
     * @return The current return value as a float
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed float
     */
    public float getReturnValueF() {
        return (Float) this.getReturnValue();
    }

    /**
     * Get the current return value as a double.
     *
     * @return The current return value as a double
     * @throws IllegalStateException If no return value has been set
     * @throws ClassCastException    If the return value is not a boxed double
     */
    public double getReturnValueD() {
        return (Double) this.getReturnValue();
    }

}
