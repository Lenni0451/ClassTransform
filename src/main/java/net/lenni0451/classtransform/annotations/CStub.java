package net.lenni0451.classtransform.annotations;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;

/**
 * Create a stub copy of a method/field/constructor to access the original member without requiring access to the original class.<br>
 * This allows calling methods of objects that are not available at compile time.
 */
public @interface CStub {

    /**
     * The member to stub.<br>
     * e.g. {@code java/lang/String#hashCode()I}
     *
     * @return The member to stub
     */
    @AnnotationRemap(RemapType.MEMBER)
    String value();

    /**
     * The access type of the member.<br>
     * By default, the access type is determined automatically.<br>
     * This <b>needs</b> to be set if member validation is turned off.<br>
     * If member validation is turned on, this will be used as extra validation.
     *
     * @return The access type of the member
     */
    Access access() default Access.AUTO;

    /**
     * Validate the member when transforming.<br>
     * Disabling this will skip the validation and allows the transformer to generate code that will <i>may</i> not work at runtime.<br>
     * If this is disabled, the access type must be set manually.<br>
     * This is useful for stubbing members that are not available at compile time, but are available at runtime (e.g. added by another transformer).
     *
     * @return Whether to validate the member
     */
    boolean memberValidation() default true;


    /**
     * The type of access to the member.
     */
    enum Access {
        /**
         * Automatically determine the access type.
         */
        AUTO,
        /**
         * Access a static member.
         */
        STATIC,
        /**
         * Access a non-static member.
         */
        NON_STATIC
    }

}
