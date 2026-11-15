package net.lenni0451.classtransform.debugger;

import net.lenni0451.classtransform.debugger.timings.TimedGroup;
import net.lenni0451.classtransform.debugger.timings.TimedTransformer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A timer to measure the time it takes to transform a class.
 * <p>
 * Durations are recorded in milliseconds per transformer group and name in insertion order.
 * The class keeps a single active measurement and is not thread-safe, so one instance should
 * only be used by the thread that performs the transformation.
 */
@ParametersAreNonnullByDefault
public class TransformerTimings {

    private final Map<TimedTransformer, Integer> timings = new LinkedHashMap<>();
    private TimedGroup currentGroup;
    private String currentTransformer;
    private long currentStart;

    /**
     * Get the recorded timings.
     *
     * @return An unmodifiable view of the timings in insertion order
     */
    public Map<TimedTransformer, Integer> getTimings() {
        return Collections.unmodifiableMap(this.timings);
    }

    /**
     * Start the timing for a transformer.
     * <p>
     * This replaces the active measurement, so a preceding call that was not ended is discarded.
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
     * <p>
     * Because {@link TimedTransformer} uses identity equality, every call adds a new entry even
     * if the same group and name were measured before.
     */
    public void end() {
        long end = System.currentTimeMillis();
        this.timings.put(new TimedTransformer(this.currentGroup, this.currentTransformer), (int) (end - this.currentStart));
    }

}
