package net.lenni0451.classtransform.mappings.annotation;

/**
 * The type of the annotation field to remap.
 */
public enum RemapType {

    /**
     * A short member is only the method name and optionally the descriptor.<br>
     * This also replaces wildcards with the correct values.
     */
    SHORT_MEMBER,
    /**
     * A member is the full owner, name and descriptor of a method/field.
     */
    MEMBER,
    /**
     * A field or method descriptor.
     */
    DESCRIPTOR,
    /**
     * A class is the full name of a class separated by dots.
     */
    CLASS,
    /**
     * Mark an annotation value to also be remapped.
     */
    ANNOTATION,
    /**
     * Mark a member to be remapped dynamically.<br>
     * The dynamic remapper class has to be set
     */
    DYNAMIC

}
