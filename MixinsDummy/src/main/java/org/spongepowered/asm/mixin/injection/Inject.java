package org.spongepowered.asm.mixin.injection;

import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inserts handler code at one or more points in a target method. This compatibility stub supplies the annotation values
 * to the translator and has no behavior itself.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {

    /**
     * Names or name-and-descriptor patterns of the methods to inject into.
     *
     * @return The target method patterns
     */
    String[] method() default {};

    /**
     * Descriptor selectors for the target methods, used in place of or alongside {@link #method()}.
     *
     * @return The target descriptors
     */
    Desc[] target() default {};

    /**
     * The injection points at which handler code runs. This member is required.
     *
     * @return The injection points
     */
    At[] at();

    /**
     * Slices that restrict where injection points can be found; defaults to a single empty slice.
     *
     * @return The slices
     */
    Slice[] slice() default @Slice;

    /**
     * Semicolon-separated constraint expression of the form {@code TOKEN(range)} that must pass before injecting.
     *
     * @return The constraint expression
     */
    String constraints() default "";

    /**
     * Whether the handler may cancel the target method and supply a return value.
     *
     * @return Whether cancellation is allowed
     */
    boolean cancellable() default false;

    /**
     * Controls which local variables are captured and passed to the handler.
     *
     * @return The local capture mode
     */
    LocalCapture locals() default LocalCapture.NO_CAPTURE;

    /**
     * Minimum number of successful injections required. A value of zero or less makes every translated
     * target optional; the manager default applies only when this member is absent.
     *
     * @return The minimum injection count
     */
    int require() default -1;

    /**
     * Expected injection count, checked only when count debugging is enabled.
     *
     * @return The expected injection count
     */
    int expect() default 1;

    /**
     * Maximum number of successful injections; -1 leaves the count uncapped.
     *
     * @return The maximum injection count
     */
    int allow() default -1;

    /**
     * Application order within the injection phase; lower values run first.
     *
     * @return The application order
     */
    int order() default 1000;

    /**
     * Whether the selectors in this annotation are remapped.
     *
     * @return Whether selectors are remapped
     */
    boolean remap() default true;

}
