package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface At {

    String value();

    String target() default "";

    Shift shift() default Shift.AFTER;

    int ordinal() default -1;

    /**
     * The opcode field is only supported with the JUMP target.<br>
     * The FIELD target in ClassTransform has too many differences to the FIELD target with opcode in Mixins.<br>
     * Please use the GETFIELD or PUTFIELD targets from ClassTransform instead.<br>
     * The GETFIELD and PUTFIELD targets from ClassTransform include their static counterparts:<br>
     * GETFIELD -> {@code Opcodes.GETSTATIC} and {@code Opcodes.GETFIELD}<br>
     * PUTFIELD -> {@code Opcodes.PUTSTATIC} and {@code Opcodes.PUTFIELD}
     */
    @Deprecated
    int opcode() default -1;

    @Deprecated
    boolean remap() default true;


    enum Shift {
        BEFORE,
        AFTER
    }

}
