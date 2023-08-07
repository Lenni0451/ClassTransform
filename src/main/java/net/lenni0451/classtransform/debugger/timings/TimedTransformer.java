package net.lenni0451.classtransform.debugger.timings;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The group and name of a timed transformer.
 */
@ParametersAreNonnullByDefault
public class TimedTransformer {

    private final TimedGroup group;
    private final String name;

    public TimedTransformer(final TimedGroup group, final String name) {
        this.group = group;
        this.name = name;
    }

    /**
     * @return The group of the transformer
     */
    public TimedGroup getGroup() {
        return this.group;
    }

    /**
     * @return The name of the transformer
     */
    public String getName() {
        return this.name;
    }

    // Do not add a hashCode() method here, this class needs to be a unique key in a map

}
