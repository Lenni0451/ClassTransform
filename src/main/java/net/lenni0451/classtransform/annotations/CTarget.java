package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.mappings.dynamic.TargetRemapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the target of the transformer method.<br>
 * The targets are specified in the {@link TransformerManager}.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CTarget">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CTarget {

    /**
     * The type of the target.<br>
     * e.g. INVOKE
     *
     * @return The type of the target
     */
    String value();

    /**
     * The target of the type.<br>
     * e.g. type=INVOKE, target=Ljava/lang/Object;hashCode()I
     *
     * @return The target of the type
     */
    @AnnotationRemap(value = RemapType.DYNAMIC, dynamicRemapper = TargetRemapper.class)
    String target() default "";

    /**
     * The shift for injecting {@code BEFORE}/{@code AFTER} the target.<br>
     * This does not work with all targets (e.g. {@code RETURN} has to be {@code BEFORE}).
     *
     * @return The shift
     */
    Shift shift() default Shift.AFTER;

    /**
     * The ordinal of target.
     *
     * @return The ordinal
     */
    int ordinal() default -1;

    /**
     * If the target is optional or an exception should be thrown if not found.
     *
     * @return If the target is optional
     */
    boolean optional() default false;

    /**
     * @return The id of the slice to use
     */
    String slice() default "";

    /**
     * Filter matches by opcode before applying the ordinal.
     *
     * @return The opcode or -1 to accept any opcode
     */
    int opcode() default -1;

    /**
     * @return The instruction offset for {@link Shift#BY}
     */
    int by() default 0;

    /**
     * If omitted, this setting is inherited from the enclosing injection annotation.
     *
     * @return If this selector should be remapped
     */
    boolean remap() default true;

    /**
     * @return An identifier for this injection point
     */
    String id() default "";

    /**
     * Select a single boundary from multiple matches.<br>
     * Supported values are {@code ONE}, {@code FIRST} and {@code LAST}.
     *
     * @return The slice boundary selector
     */
    String selector() default "ONE";

    /**
     * Additional named arguments for the injection point.<br>
     * {@code INVOKE_STRING} accepts {@code ldc=...} to select a string argument.<br>
     * {@code FIELD} accepts {@code array=get|set|length} and {@code fuzz=N} for array element accesses.
     *
     * @return The injection point arguments
     */
    String[] args() default {};


    /**
     * The shift for injecting {@code BEFORE}/{@code AFTER} the target.
     */
    enum Shift {
        /**
         * Inject the code before the target.
         */
        BEFORE,
        /**
         * Inject the code after the target.
         */
        AFTER,
        /**
         * Select an instruction at the offset specified by {@link CTarget#by()}.
         */
        BY
    }

}
