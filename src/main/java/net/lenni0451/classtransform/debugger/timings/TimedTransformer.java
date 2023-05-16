package net.lenni0451.classtransform.debugger.timings;

/**
 * The group and name of a timed transformer.
 */
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

}
