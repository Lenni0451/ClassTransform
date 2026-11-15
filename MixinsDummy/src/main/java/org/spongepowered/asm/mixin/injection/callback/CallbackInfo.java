package org.spongepowered.asm.mixin.injection.callback;

/**
 * Compile-time stub of Mixin's cancellation handle for injected callbacks.
 * <p>
 * An injection handler may receive an instance to check whether cancelling is allowed, ask whether the
 * target was cancelled and request cancellation itself. The stub has no behavior; the Mixin translator
 * rewrites references to ClassTransform's own callback type.
 */
public class CallbackInfo {

    CallbackInfo() {
    }

    /**
     * Checks whether this callback permits cancelling the target method.
     *
     * @return If the target method may be cancelled
     */
    public final boolean isCancellable() {
        return true;
    }

    /**
     * Checks whether cancellation has already been requested.
     *
     * @return If the target method is marked as cancelled
     */
    public final boolean isCancelled() {
        return false;
    }

    /**
     * Requests cancellation of the remaining target method logic.
     * <p>
     * The translator turns this call into setting the cancelled state of the translated callback.
     */
    public void cancel() {
    }

}
