package net.lenni0451.classtransform.utils.annotations;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * An interface providing extra methods to a parsed annotation by the {@link AnnotationParser}.
 * <p>
 * Annotations returned by the parser are generated classes implementing both their annotation
 * interface and this one. The values map contains explicit members merged with the annotation
 * defaults and is the live backing map rather than a copy, so later changes are visible through
 * the annotation methods. Generated wrappers do not implement value-based equality.
 */
public interface IParsedAnnotation {

    /**
     * Get the raw member values backing this annotation instance.
     *
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
    boolean wasSet(@Nonnull final String name);

}
