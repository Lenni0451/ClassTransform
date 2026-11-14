package net.lenni0451.classtransform.annotations;

/**
 * The behaviour for capturing local variables in a {@link net.lenni0451.classtransform.annotations.injection.CInject} handler.
 */
public enum CLocalCapture {

    /**
     * Do not capture local variables.
     */
    NO_CAPTURE,
    /**
     * Do not capture local variables and print the local variable table at the injection point instead.
     */
    PRINT,
    /**
     * Capture local variables and skip the injection if they do not match the handler.
     */
    CAPTURE_FAILSOFT,
    /**
     * Capture local variables and throw an exception if they do not match the handler.
     */
    CAPTURE_FAILHARD,
    /**
     * Capture local variables and generate a method which throws an exception when called if they do not match the handler.
     */
    CAPTURE_FAILEXCEPTION

}
