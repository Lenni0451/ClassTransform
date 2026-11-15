package net.lenni0451.classtransform.debugger.timings;

/**
 * The timing groups a transformer can belong to.
 */
public enum TimedGroup {

    /**
     * A transformer that operates on raw class bytes.
     */
    BYTECODE_TRANSFORMER,
    /**
     * A transformer that operates on parsed bytecode.
     */
    RAW_TRANSFORMER,
    /**
     * An annotation handler, including injection plan preparation.
     */
    ANNOTATION_HANDLER,
    /**
     * A transformer that runs after the regular bytecode transformers.
     */
    POST_TRANSFORMER,
    /**
     * Not really a transformer but still timed.
     */
    REMAPPER

}
