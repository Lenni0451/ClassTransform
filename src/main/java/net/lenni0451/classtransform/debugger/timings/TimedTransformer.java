package net.lenni0451.classtransform.debugger.timings;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The group and name of a timed transformer.
 * <p>
 * Instances use identity equality and are therefore unique keys in a timing map.
 */
@ParametersAreNonnullByDefault
public class TimedTransformer {

    private final TimedGroup group;
    private final String name;

    /**
     * Create a timed transformer key.
     *
     * @param group The group the transformer belongs to
     * @param name  The name of the transformer
     */
    public TimedTransformer(final TimedGroup group, final String name) {
        this.group = group;
        this.name = name;
    }

    /**
     * Get the group of the transformer.
     *
     * @return The group of the transformer
     */
    public TimedGroup getGroup() {
        return this.group;
    }

    /**
     * Get the name of the transformer.
     *
     * @return The name of the transformer
     */
    public String getName() {
        return this.name;
    }

    // Do not add a hashCode() method here, this class needs to be a unique key in a map

}
