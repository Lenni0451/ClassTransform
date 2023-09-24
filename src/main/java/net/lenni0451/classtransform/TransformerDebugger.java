package net.lenni0451.classtransform;

import net.lenni0451.classtransform.debugger.timings.TimedTransformer;
import net.lenni0451.classtransform.utils.log.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
public class TransformerDebugger {

    private final TransformerManager transformerManager;
    private final Map<String, Map<TimedTransformer, Integer>> timings;

    TransformerDebugger(final TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
        this.timings = new ConcurrentHashMap<>();
    }

    /**
     * Load all transformed classes from the current class loader.
     */
    public void loadTransformedClasses() {
        this.loadTransformedClasses(this.getClass().getClassLoader());
    }

    /**
     * Load all transformed classes from the given class loader.
     *
     * @param classLoader The used class loader
     */
    public void loadTransformedClasses(final ClassLoader classLoader) {
        for (String transformedClass : this.transformerManager.getTransformedClasses().toArray(new String[0])) {
            try {
                Class<?> clazz = classLoader.loadClass(transformedClass);
                Logger.info("Loaded transformed class {}", clazz.getName());
            } catch (Throwable t) {
                Logger.error("Failed to load transformed class {}", transformedClass, t);
            }
        }
    }

    /**
     * @return The timings for all transformed classes
     */
    public Map<String, Map<TimedTransformer, Integer>> getTimings() {
        return Collections.unmodifiableMap(this.timings);
    }


    void addTimings(final String className, final Map<TimedTransformer, Integer> timings) {
        if (!timings.isEmpty()) this.timings.put(className, timings);
    }

}
