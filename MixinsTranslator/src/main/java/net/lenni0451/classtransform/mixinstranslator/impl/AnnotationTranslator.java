package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Translates a single Mixin or MixinExtras annotation into its ClassTransform equivalent.
 * <p>
 * Implementations rewrite {@link AnnotationNode#desc} to the target annotation descriptor and adjust the annotation
 * values in place. Instances are registered in {@link AnnotationTranslatorManager} and shared, so implementations must
 * not keep per-annotation state.
 */
@ParametersAreNonnullByDefault
public interface AnnotationTranslator {

    /**
     * Translate the given annotation node.
     * <p>
     * The annotation node and the values map are both mutated in place and no value is returned; the caller stores the
     * values map back into {@link AnnotationNode#values} afterwards. Implementations change {@link AnnotationNode#desc}
     * to the target descriptor and adapt value keys and nested annotations as needed. A translator is only invoked for
     * the annotation it is registered for, so unrelated annotations do not have to be handled. Values without a
     * ClassTransform equivalent are usually kept or dropped; constructs that cannot be represented throw an
     * {@link IllegalArgumentException} or {@link UnsupportedOperationException}.
     *
     * @param annotation The annotation node to translate, modified in place
     * @param values     The annotation values keyed by name, modified in place
     */
    void translate(final AnnotationNode annotation, final Map<String, Object> values);

    /**
     * Translate common injector settings and preserve Mixin's default count semantics.
     * <p>
     * Adds {@code require = -1} when absent and translates the nested slice annotations in place. A single
     * {@code slice} stays under its key; multiple slices are stored as {@code slices}.
     *
     * @param values The injector annotation values
     */
    default void translateInjection(final Map<String, Object> values) {
        values.putIfAbsent("require", -1);
        Object slices = values.remove("slice");
        if (slices instanceof AnnotationNode) {
            this.dynamicTranslate((AnnotationNode) slices);
            values.put("slice", slices);
        } else if (slices instanceof List) {
            for (AnnotationNode slice : (List<AnnotationNode>) slices) this.dynamicTranslate(slice);
            values.put("slices", slices);
        }
    }

    /**
     * Translate a nested annotation with the translator registered for it.
     * <p>
     * Does nothing if no translator is registered for the annotation. The annotation is modified in place.
     *
     * @param annotation The nested annotation to translate
     */
    default void dynamicTranslate(final AnnotationNode annotation) {
        AnnotationTranslator translator = AnnotationTranslatorManager.getTranslator(Type.getType(annotation.desc));
        if (translator != null) {
            Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
            translator.translate(annotation, values);
            annotation.values = AnnotationUtils.mapToList(values);
        }
    }

    /**
     * Read and remove a nested annotation value that ClassTransform accepts only once.
     * <p>
     * A single annotation is returned and removed from the map. A list with one element is removed and collapsed back
     * under the {@code name} key, so it remains in the map; an empty list yields {@code null} and removes the key. A
     * list with multiple elements is rejected.
     *
     * @param name           The value key
     * @param values         The annotation values, modified in place
     * @param annotationName The Mixin annotation name used in error messages
     * @return The nested annotation, or {@code null} if the value was an empty list
     * @throws IllegalStateException If the value is missing, not an annotation or list, or a list with multiple elements
     */
    default AnnotationNode getSingleAnnotation(final String name, final Map<String, Object> values, final String annotationName) {
        Object rawValue = values.remove(name);
        if (rawValue instanceof AnnotationNode) {
            return (AnnotationNode) rawValue;
        } else if (rawValue instanceof List) {
            values.remove(name);
            List<AnnotationNode> list = (List<AnnotationNode>) rawValue;
            if (list.isEmpty()) {
                return null;
            } else if (list.size() == 1) {
                values.put(name, list.get(0));
                return list.get(0);
            } else {
                throw new IllegalStateException("ClassTransform does not support multiple " + name + " in @" + annotationName + " annotations");
            }
        } else {
            throw new IllegalStateException("Unknown type for " + name + " in @" + annotationName + " annotation");
        }
    }

    /**
     * Move a value to a different key if it is present.
     * <p>
     * The value is removed from the source key.
     *
     * @param values The annotation values, modified in place
     * @param from   The current key
     * @param to     The new key
     */
    default void move(final Map<String, Object> values, final String from, final String to) {
        if (values.containsKey(from)) {
            values.put(to, values.remove(from));
        }
    }

    /**
     * Move and convert a value if it is present.
     * <p>
     * The source value is removed from the map. If the mapper returns {@code null}, the target key is not set.
     *
     * @param <F>    The source value type
     * @param <T>    The target value type
     * @param values The annotation values, modified in place
     * @param from   The current key
     * @param to     The new key
     * @param mapper The conversion function
     */
    default <F, T> void map(final Map<String, Object> values, final String from, final String to, final Function<F, T> mapper) {
        if (values.containsKey(from)) {
            T mappedValue = mapper.apply((F) values.remove(from));
            if (mappedValue != null) {
                values.put(to, mappedValue);
            }
        }
    }

    /**
     * Move a value between two maps if the source contains the key.
     *
     * @param from The source map, modified in place
     * @param to   The target map, modified in place
     * @param key  The key to move
     */
    default void move(final Map<String, Object> from, final Map<String, Object> to, final String key) {
        if (from.containsKey(key)) {
            to.put(key, from.remove(key));
        }
    }

}
