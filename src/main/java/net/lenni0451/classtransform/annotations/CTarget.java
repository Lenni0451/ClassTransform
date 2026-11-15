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
 * @see CSlice
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki/CTarget">GitHub Wiki</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface CTarget {

    /**
     * The type of the injection target.
     * Built-in values are {@code HEAD}, {@code CTOR_HEAD}, {@code RETURN}, {@code TAIL}, {@code THROW},
     * {@code INVOKE}, {@code INVOKE_STRING}, {@code INVOKE_ASSIGN}, {@code JUMP}, {@code FIELD},
     * {@code GETFIELD}, {@code PUTFIELD}, {@code NEW}, {@code OPCODE}, {@code CONSTANT},
     * {@code INSTANCEOF}, {@code EXPRESSION}, {@code LOAD} and {@code STORE}.
     * Additional target names can be registered on the {@link TransformerManager}.
     *
     * @return The type of the target
     */
    String value();

    /**
     * The target of the selected type.
     * e.g. type=INVOKE, target=Ljava/lang/Object;hashCode()I
     * The expected format depends on {@link #value()}: member targets use an ASM member declaration,
     * {@code INSTANCEOF} uses a class name, {@code NEW} matches the constructor invocation of an allocated
     * object and accepts a class name, a class descriptor, an ASM member declaration or a method
     * descriptor, {@code EXPRESSION} uses a
     * {@link net.lenni0451.classtransform.annotations.expression.CExpression} id and {@code CONSTANT} uses
     * a type prefix followed by the value.
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
     * The ordinal of the target among all matches.
     * A value of {@code -1} selects all matches.
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
     * The id of the named slice to search in.
     * The id refers to {@code CSlice#id()} on a slice declared by the enclosing injection annotation.
     * An empty id selects the slice with an empty id, which is the default {@code slice()} of the injection
     * annotation; if no such slice is declared, the target is unrestricted. The selected slice may itself
     * narrow the search.
     *
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
     * The instruction offset used with {@link Shift#BY}.
     * Positive values move towards the end of the method and negative values towards the start.
     *
     * @return The instruction offset for {@link Shift#BY}
     */
    int by() default 0;

    /**
     * Whether the selectors of this target should be remapped.
     * If omitted, this setting is inherited from the enclosing injection annotation.
     *
     * @return If this selector should be remapped
     */
    boolean remap() default true;

    /**
     * An identifier for this injection point.
     * The id is included in error messages to distinguish multiple targets of the same type.
     *
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
     * A descriptor selector for the target member.<br>
     * If the descriptor value is not empty, it is used instead of {@link #target()}.
     *
     * @return The descriptor selector
     */
    @AnnotationRemap(RemapType.ANNOTATION)
    CDesc desc() default @CDesc(value = "");

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
