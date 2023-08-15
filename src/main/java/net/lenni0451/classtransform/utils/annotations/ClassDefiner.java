package net.lenni0451.classtransform.utils.annotations;

import sun.misc.Unsafe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * Define anonymous classes at runtime.<br>
 * This class uses Unsafe for older java versions and method handles for newer versions.
 *
 * @param <T> The type of the class
 */
@ParametersAreNonnullByDefault
public class ClassDefiner<T> {

    private static Unsafe UNSAFE;

    private static Unsafe getUnsafe() {
        if (UNSAFE == null) {
            for (Field field : Unsafe.class.getDeclaredFields()) {
                if (field.getType() == Unsafe.class) {
                    field.setAccessible(true);
                    try {
                        UNSAFE = (Unsafe) field.get(null);
                        break;
                    } catch (Throwable t) {
                        throw new RuntimeException("Unable to get unsafe instance", t);
                    }
                }
            }
        }
        return UNSAFE;
    }

    /**
     * Generate a name for the defined class.<br>
     * When using method handles to define a class the package name has to be the same as the caller class.
     *
     * @param name The name of the class
     * @return The generated name
     */
    public static String generateClassName(final String name) {
        return slash(ClassDefiner.class.getPackage().getName()) + "/" + name;
    }

    /**
     * Define an anonymous class.
     *
     * @param bytecode The bytecode of the class
     * @param <T>      The type of the class
     * @return The defined class
     * @throws ClassCastException When the defined class is not of the given type
     * @throws RuntimeException   When the class could not be defined
     */
    public static <T> ClassDefiner<T> defineAnonymousClass(final byte[] bytecode) {
        Throwable error;
        try {
            Method defineAnonymousClass = Unsafe.class.getDeclaredMethod("defineAnonymousClass", Class.class, byte[].class, Object[].class);
            return new ClassDefiner<>((Class<?>) defineAnonymousClass.invoke(getUnsafe(), ClassDefiner.class, bytecode, new Object[0]));
        } catch (Throwable t) {
            error = t;
        }
        try {
            Class<?> classOptionClass = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption");
            Object emptyClassOptionArray = Array.newInstance(classOptionClass, 0);
            Method lookupDefineHiddenClass = MethodHandles.Lookup.class.getDeclaredMethod("defineHiddenClass", byte[].class, Boolean.TYPE, emptyClassOptionArray.getClass());
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) lookupDefineHiddenClass.invoke(MethodHandles.lookup(), bytecode, false, emptyClassOptionArray);
            return new ClassDefiner<>(lookup.lookupClass());
        } catch (Throwable t) {
            t.addSuppressed(error);
            error = t;
        }
        throw new RuntimeException("Unable to define anonymous class", error);
    }


    private final Class<T> clazz;

    private ClassDefiner(final Class<?> clazz) {
        this.clazz = (Class<T>) clazz;
    }

    /**
     * @return The defined class
     */
    public Class<?> getClazz() {
        return this.clazz;
    }

    /**
     * Create a new instance of the defined class.
     *
     * @return The new instance
     * @throws InvocationTargetException When the constructor throws an exception
     * @throws NoSuchMethodException     When the class does not have a default constructor
     * @throws InstantiationException    When the class is abstract
     * @throws IllegalAccessException    When the class is not public
     */
    public T newInstance() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return this.newInstance(new Object[0]);
    }

    /**
     * Create a new instance of the defined class using the given arguments.
     *
     * @param args The arguments for the constructor
     * @return The new instance
     * @throws InvocationTargetException When the constructor throws an exception
     * @throws NoSuchMethodException     When the class does not have a constructor with the given arguments
     * @throws InstantiationException    When the class is abstract
     * @throws IllegalAccessException    When the class is not public
     */
    public T newInstance(final Object... args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();

        return this.newInstance(types, args);
    }

    /**
     * Create a new instance of the defined class using the given arguments.
     *
     * @param types  The types of the arguments
     * @param values The arguments for the constructor
     * @return The new instance
     * @throws NoSuchMethodException     When the class does not have a constructor with the given arguments
     * @throws InvocationTargetException When the constructor throws an exception
     * @throws InstantiationException    When the class is abstract
     * @throws IllegalAccessException    When the class is not public
     */
    public T newInstance(final Class<?>[] types, final Object[] values) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (types.length != values.length) throw new IllegalArgumentException("Types and values must be of the same length");
        Constructor<T> constructor = this.clazz.getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }

}
