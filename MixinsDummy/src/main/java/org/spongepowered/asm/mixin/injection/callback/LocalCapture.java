package org.spongepowered.asm.mixin.injection.callback;

/**
 * Local variable capture modes for injection handlers.
 * <p>
 * The mode decides whether the locals visible at an injection point are captured, merely printed, or how
 * the injection fails when they cannot be matched to the handler parameters. This compile-only
 * compatibility stub has no behavior; the translator maps its values to ClassTransform's local capture
 * settings.
 */
public enum LocalCapture {

    /**
     * Do not capture local variables for the handler.
     */
    NO_CAPTURE,
    /**
     * Print the local variable table at the injection point instead of injecting.
     */
    PRINT,
    /**
     * Capture locals and skip the injection site when they do not match the handler.
     */
    CAPTURE_FAILSOFT,
    /**
     * Capture locals and fail the transformation when they do not match the handler.
     */
    CAPTURE_FAILHARD,
    /**
     * Capture locals; when they do not match the handler, generate a synthetic method that throws when
     * called.
     */
    CAPTURE_FAILEXCEPTION

}
