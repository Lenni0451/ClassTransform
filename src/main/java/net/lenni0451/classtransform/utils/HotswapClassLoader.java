package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.utils.log.ILogger;

import java.lang.instrument.ClassFileTransformer;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal class loader required for transformer hotswapping.<br>
 * Fake classes with the same name as the transformer classes are loaded from this class loader.<br>
 * This causes the JVM to pass the changed transformer bytecode into the instrumentation {@link ClassFileTransformer} which the reapplies the transformer to the unmodified class bytecode.
 */
public class HotswapClassLoader extends ClassLoader {

    private final ILogger logger;
    private final Map<String, byte[]> hotswapClasses;

    public HotswapClassLoader(final ILogger logger) {
        ClassLoader.registerAsParallelCapable();

        this.logger = logger;
        this.hotswapClasses = new HashMap<>();
    }

    /**
     * Get the bytecode of a fake class.<br>
     * This fake class only contains a constructor.
     *
     * @param name The name of the class
     * @return The bytecode of the class
     */
    public byte[] getHotswapClass(final String name) {
        return this.hotswapClasses.computeIfAbsent(name, n -> ASMUtils.toStacklessBytes(ASMUtils.createEmptyClass(n)));
    }

    /**
     * Define a fake class in this class loader.
     *
     * @param name The name of the class
     */
    public void defineHotswapClass(final String name) {
        if (this.hotswapClasses.containsKey(name)) return;
        try {
            byte[] classBytes = this.getHotswapClass(name);
            Class<?> clazz = this.defineClass(name, classBytes, 0, classBytes.length);
            clazz.getDeclaredConstructor().newInstance(); //Initialize the class
        } catch (Throwable t) {
            this.logger.warn("Failed to define hotswap class '%s'. Hotswapping will not work for this transformer", name, t);
        }
    }

}
