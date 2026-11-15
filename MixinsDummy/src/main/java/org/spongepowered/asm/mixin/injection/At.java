package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Selects an instruction or a named location where an injector runs. This compatibility stub only carries the selector
 * values and has no behavior.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface At {

    /**
     * The kind of injection point, for example {@code HEAD}, {@code INVOKE}, {@code FIELD} or {@code CONSTANT}. This
     * member is required.
     *
     * @return The injection point kind
     */
    String value();

    /**
     * Kind-specific selector whose meaning depends on {@link #value()}.
     *
     * @return The target selector
     */
    String target() default "";

    /**
     * Descriptor selector for the target member; an empty descriptor disables it.
     *
     * @return The descriptor selector
     */
    Desc desc() default @Desc(value = "");

    /**
     * Named arguments for the injection point.<br>
     * Constant value discriminators are supported for {@code CONSTANT}.
     *
     * @return The injection point arguments
     */
    String[] args() default {};

    /**
     * Placement of the injection relative to the matched instruction.
     *
     * @return The shift
     */
    Shift shift() default Shift.NONE;

    /**
     * Instruction offset used when {@link #shift()} is {@link Shift#BY}.
     *
     * @return The offset
     */
    int by() default 0;

    /**
     * Identifier of a slice declared on the enclosing injector that limits the search.
     *
     * @return The slice identifier
     */
    String slice() default "";

    /**
     * An identifier for this injection point. For {@code MIXINEXTRAS:EXPRESSION} points this also selects
     * the expression declared by {@link com.llamalad7.mixinextras.expression.Expression#id()}.
     *
     * @return The injection point identifier
     */
    String id() default "";

    /**
     * Which match to use when several instructions satisfy the selector; -1 means the ordinal is not considered.
     *
     * @return The ordinal or -1
     */
    int ordinal() default -1;

    /**
     * Bytecode opcode filter applied before {@link #ordinal()}; -1 accepts any opcode.
     *
     * @return The opcode or -1
     */
    int opcode() default -1;

    /**
     * Whether names in {@link #target()} are remapped.
     *
     * @return Whether the target is remapped
     */
    boolean remap() default true;

    /**
     * Placement of an injection point relative to the instruction it matches.
     */
    enum Shift {
        /**
         * No explicit shift; the default placement is used.
         */
        NONE,
        /**
         * Place the injection before the matched instruction.
         */
        BEFORE,
        /**
         * Place the injection after the matched instruction.
         */
        AFTER,
        /**
         * Place the injection at the offset given by {@link At#by()}.
         */
        BY
    }

}
