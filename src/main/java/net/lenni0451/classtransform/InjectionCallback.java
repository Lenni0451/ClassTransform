package net.lenni0451.classtransform;

public class InjectionCallback {

    private final boolean cancellable;

    private boolean cancelled;
    private Object returnValue;
    private boolean returnValueSet;

    public InjectionCallback(final boolean cancellable) {
        this.cancellable = cancellable;
    }

    public InjectionCallback(final boolean cancellable, final Object returnValue) {
        this.cancellable = cancellable;
        this.returnValue = returnValue;
        this.returnValueSet = true;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(final boolean cancelled) {
        if (cancelled && !this.cancellable) throw new IllegalArgumentException("Cannot cancel a non-cancellable callback");
        this.cancelled = cancelled;
    }

    public boolean isCancellable() {
        return this.cancellable;
    }

    public Object getReturnValue() {
        if (!this.returnValueSet) throw new IllegalStateException("Return value not set");
        return returnValue;
    }

    public <T> T castReturnValue() {
        return (T) this.getReturnValue();
    }

    public void setReturnValue(final Object returnValue) {
        this.returnValue = returnValue;
        this.setCancelled(true);
        this.returnValueSet = true;
    }

}
