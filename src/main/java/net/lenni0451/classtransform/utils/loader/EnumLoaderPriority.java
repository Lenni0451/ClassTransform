package net.lenni0451.classtransform.utils.loader;

/**
 * The priority of the {@link InjectionClassLoader}.
 */
public enum EnumLoaderPriority {

    /**
     * Try to load the classes from the injection class loader class path first.
     */
    CUSTOM_FIRST,
    /**
     * Try to load the classes from the parent class loader first.
     */
    PARENT_FIRST

}
