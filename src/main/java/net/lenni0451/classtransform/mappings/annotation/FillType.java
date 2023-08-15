package net.lenni0451.classtransform.mappings.annotation;

/**
 * The type of action to perform when remapping an annotation.
 */
public enum FillType {

    /**
     * Replace the value of the annotation with the remapped value.
     */
    REPLACE,
    /**
     * Keep the value of the annotation empty if it already is.<br>
     * If it is not empty, it is replaced with the remapped value.
     */
    KEEP_EMPTY,
    /**
     * Skip remapping the annotation value entirely.
     */
    SKIP

}
