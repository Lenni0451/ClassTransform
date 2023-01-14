package net.lenni0451.classtransform.utils;

public enum FailStrategy {

    /**
     * Try to continue the transformation
     */
    CONTINUE,
    /**
     * Cancel the transformation of the current class
     */
    CANCEL,
    /**
     * Exit the JVM
     */
    EXIT

}
