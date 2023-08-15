package net.lenni0451.classtransform.debugger.timings;

/**
 * The timing groups a transformer can belong to.
 */
public enum TimedGroup {

    BYTECODE_TRANSFORMER,
    RAW_TRANSFORMER,
    ANNOTATION_HANDLER,
    POST_TRANSFORMER,
    /**
     * Not really a transformer but still timed.
     */
    REMAPPER

}
