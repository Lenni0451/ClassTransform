package net.lenni0451.classtransform.utils.annotations;

import java.util.Map;

/**
 * A interface providing extra methods to a parsed annotation by the {@link AnnotationParser}.
 */
public interface IParsedAnnotation {

    /**
     * @return The raw values of the annotation
     */
    Map<String, Object> getValues();

    /**
     * Check if a value was present in the bytecode of the annotation.<br>
     * Default values are not considered as present.
     *
     * @param name The name of the value
     * @return If the value was present
     */
    boolean wasSet(final String name);

}
