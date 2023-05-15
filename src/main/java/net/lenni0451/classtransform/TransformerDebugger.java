package net.lenni0451.classtransform;

import net.lenni0451.classtransform.utils.log.Logger;

public class TransformerDebugger {

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
        for (String transformedClass : this.transformerManager.getTransformedClasses().toArray(new String[0])) {
            try {
                Class<?> clazz = classLoader.loadClass(transformedClass);
                Logger.info("Loaded transformed class {}", clazz.getName());
            } catch (Throwable t) {
                Logger.error("Failed to load transformed class {}", transformedClass);
            }
        }
    }

}
