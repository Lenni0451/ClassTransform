package net.lenni0451.classtransform;

import net.lenni0451.classtransform.annotations.injection.CInject;

import javax.annotation.Nullable;

/**
 * The callback used to cancel {@link CInject} transformer.
 */
public class InjectionCallback {

    private final boolean cancellable;

    private boolean cancelled;
    private Object returnValue;
    private boolean returnValueSet;

    public InjectionCallback(final boolean cancellable) {
        this.cancellable = cancellable;
    }

    public InjectionCallback(final boolean cancellable, @Nullable final Object returnValue) {
        this.cancellable = cancellable;
        this.returnValue = returnValue;
        this.returnValueSet = true;
    }

    /**
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
     * @return If the callback is cancellable
     */
    public boolean isCancellable() {
        return this.cancellable;
    }

    /**
     * @return The current return value of the original method
     */
    @Nullable
    public Object getReturnValue() {
        if (!this.returnValueSet) throw new IllegalStateException("Return value not set");
        return returnValue;
    }

    /**
     * @param <T> The wanted type
     * @return The current return value of the original method
     * @throws ClassCastException If the return value is not of the wanted type
     */
    @Nullable
    public <T> T castReturnValue() {
        return (T) this.getReturnValue();
    }

    /**
     * Set the return value of the original method.<br>
     * This will also set the cancelled state to true.
     *
     * @param returnValue The new return value
     */
    public void setReturnValue(@Nullable final Object returnValue) {
        this.returnValue = returnValue;
        this.setCancelled(true);
        this.returnValueSet = true;
    }

}
