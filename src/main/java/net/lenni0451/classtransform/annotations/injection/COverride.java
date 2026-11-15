package net.lenni0451.classtransform.annotations.injection;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override a method in a class.<br>
 * The transformer method must have the same parameters and return type as the overridden method.<br>
 * The access of the transformer method has to be higher or equal to the overridden method (private {@literal <} package private {@literal <} protected {@literal <} public).<br>
 * If the overridden method is static, the transformer method has to be static as well.<br>
 * <br>
 * Overriding a method:<br>
 * <pre>
 * &#64;COverride
 * public static void print(String text) {
 *     //Do something
 * }
 * </pre>
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/COverride">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface COverride {

    /**
     * The methods that this transformer method replaces.
     * <p>
     * An entry consists of the target name and descriptor and may use wildcards, for example
     * {@code update()V} or {@code update*}.
     *
     * @return The method name and descriptor
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, allowClassPrefix = true)
    String[] value() default {};

    /**
     * Alternative target method names which are tried when the main target is not found.
     *
     * @return The aliases
     */
    String[] aliases() default {};

    /**
     * Whether the target method selectors should be remapped.
     *
     * @return If the target method should be remapped
     */
    boolean remap() default true;

    /**
     * Constraints which have to pass for this overwrite to be applied.
     *
     * @return The constraints
     */
    String constraints() default "";

}
