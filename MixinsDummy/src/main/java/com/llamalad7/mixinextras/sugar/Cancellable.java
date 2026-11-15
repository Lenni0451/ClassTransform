package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for injection handler parameters which receive a cancellable callback for the target
 * method.
 * <p>
 * The parameter type is {@code CallbackInfo} or {@code CallbackInfoReturnable}. Cancelling the callback
 * from any injector prevents the remaining target logic and supplies the return value. This compile-only
 * compatibility stub mirrors the MixinExtras annotation so that the MixinsTranslator can recognize and
 * translate it.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Cancellable {
}
