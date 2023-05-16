package net.lenni0451.classtransform.debugger;

import net.lenni0451.classtransform.debugger.timings.TimedGroup;
import net.lenni0451.classtransform.debugger.timings.TimedTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A timer to measure the time it takes to transform a class.
 */
public class TransformerTimings {

    private final Map<TimedTransformer, Integer> timings = new LinkedHashMap<>();
    private TimedGroup currentGroup;
    private String currentTransformer;
    private long currentStart;

    /**
     * @return The timings map
     */
    public Map<TimedTransformer, Integer> getTimings() {
        return Collections.unmodifiableMap(this.timings);
    }

    /**
     * Start the timing for a transformer.
     *
     * @param group       The group the transformer belongs to
     * @param transformer The name of the transformer
     */
    public void start(final TimedGroup group, final String transformer) {
        this.currentGroup = group;
        this.currentTransformer = transformer;
        this.currentStart = System.currentTimeMillis();
    }

    /**
     * End the timing for the current transformer and add it to the timings map.
     */
    public void end() {
        long end = System.currentTimeMillis();
        this.timings.put(new TimedTransformer(this.currentGroup, this.currentTransformer), (int) (end - this.currentStart));
    }

}
