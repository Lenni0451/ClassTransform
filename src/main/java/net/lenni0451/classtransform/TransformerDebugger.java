package net.lenni0451.classtransform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformerDebugger {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerManager.LOGGER_NAME);

    private final TransformerManager transformerManager;

    TransformerDebugger(final TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
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
        for (String transformedClass : this.transformerManager.getTransformedClasses()) {
            try {
                Class<?> clazz = classLoader.loadClass(transformedClass);
                LOGGER.info("Loaded transformed class {}", clazz.getName());
            } catch (Throwable t) {
                LOGGER.error("Failed to load transformed class {}", transformedClass);
            }
        }
    }

}
