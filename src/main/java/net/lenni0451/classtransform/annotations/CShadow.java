package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create a shadow copy of a method/field/constructor to directly access the original member after injection.<br>
 * When shadowing a method you can simply make the method native to skip the need of a method body.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CShadow">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface CShadow {

    /**
     * The name of the member to shadow.<br>
     * When left empty the name and descriptor of the annotated member will be used.<br>
     * e.g. print(Ljava/lang/String;)V
     *
     * @return The name of the member to shadow
     */
    @AnnotationRemap(value = RemapType.SHORT_MEMBER, fill = FillType.KEEP_EMPTY)
    String value() default "";

    /**
     * A prefix which is stripped from the start of shadow method names.<br>
     * This is useful when the shadow method has the same arguments but a different return type than the target.
     *
     * @return The shadow method prefix
     */
    String prefix() default "shadow$";

    /**
     * Alternative target member names which are tried when the main target is not found.
     *
     * @return The aliases
     */
    String[] aliases() default {};

    /**
     * @return If the target member should be remapped
     */
    boolean remap() default true;

    /**
     * Make the shadow member public.
     *
     * @return If the shadow member should be public
     */
    boolean makePublic() default false;

    /**
     * Make the shadow member mutable.
     *
     * @return If the shadow member should be mutable
     */
    boolean makeMutable() default false;

}
