package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inject into a method at various targets.<br>
 * The original code flow can be cancelled and own values can be returned.<br>
 * The transformer method has to return {@code void} and can have the following arguments:<br>
 * <ul>
 *     <li>The arguments of the target method</li>
 *     <li>A {@link InjectionCallback} for cancelling the original method and returning values</li>
 * </ul>
 * If the target method is static the transformer method has to be static as well.<br>
 * The arguments have to be in the same order as the target method but are not required.
 * You can skip the original method arguments or the callback if you don't need them.
 * When injecting into multiple methods the arguments have to be the same for all of them (or skipped).<br>
 * The argument types have to match the target method arguments.
 * Object types can be replaced with {@link Object} instead of the actual type (useful if classes are not accessible).<br>
 * The arguments in the target method can not be directly changed.
 * If you want to change them you need to use {@link CLocalVariable}.<br>
 * <br>
 * Cancelling the target method is only possible if {@link #cancellable()} is set to {@code true}.
 * Not doing so will throw an exception when calling {@link InjectionCallback#setCancelled(boolean)}.
 * If the target method has a return value you have to use {@link InjectionCallback#setReturnValue(Object)} to change it.
 * Calling this method will also call {@link InjectionCallback#setCancelled(boolean)} with {@code true} internally.<br>
 * <br>
 * When injecting at the targets {@code RETURN}, {@code TAIL} and {@code THROW} the callback will contain the return value or the thrown exception.
 * It can be accessed by calling {@link InjectionCallback#getReturnValue()} or {@link InjectionCallback#castReturnValue()}.
 * The return value can be changed but skipping the return/throw is not possible.<br>
 * <br>
 * Injecting at the top of a method:<br>
 * <pre>
 * &#64;CInject(method = "print(Ljava/lang/String;)V", target = &#64;CTarget("HEAD"))
 * public void injectHead(String text, InjectionCallback callback) {
 *     //Do something at the top of the method
 * }
 * </pre>
 * Injecting above a method call:<br>
 * <pre>
 * &#64;CInject(method = "print", target = &#64;CTarget(value = "INVOKE", target = "...", shift = CTarget.Shift.BEFORE))
 * public void injectBefore(String text, InjectionCallback callback) {
 *     //Do something before the method call
 * }
 * </pre>
 * See {@link CTarget} for more information about targets.<br>
 * If your target has to be chosen more precisely you can use a {@link #slice()} to narrow down the search.
 *
 * @see CTarget
 * @see CSlice
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CInject {

    /**
     * The method name and descriptor to inject into.<br>
     * This supports multiple targets and wildcards.<br>
     * e.g. {@code print(Ljava/lang/String;)V} or {@code print*}
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] method();

    /**
     * The targets for the injection.<br>
     * This can be used to inject at multiple locations at once.
     *
     * @return The targets
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CTarget[] target();

    /**
     * The slice to narrow down the search for the target.
     *
     * @return The slice
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CSlice slice() default @CSlice;

    /**
     * Allow the target method to be cancelled.<br>
     * This is also required to change the return value.<br>
     * If this is set to false an exception will be thrown when trying to cancel the target method.<br>
     * Cancelling the method is only possible using the {@link InjectionCallback}.
     *
     * @return If the original method can be cancelled
     */
    boolean cancellable() default false;

}
