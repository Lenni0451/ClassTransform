package net.lenni0451.classtransform.utils.annotations;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

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

    public static String generateClassName(final String name) {
        return slash(ClassDefiner.class.getPackage().getName()) + "/" + name;
    }

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

    public Class<?> getClazz() {
        return clazz;
    }

    public T newInstance() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return this.newInstance(new Object[0]);
    }

    public T newInstance(final Object... args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();

        return this.newInstance(types, args);
    }

    public T newInstance(final Class<?>[] types, final Object[] values) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (types.length != values.length) throw new IllegalArgumentException("Types and values must be of the same length");
        Constructor<T> constructor = clazz.getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(values);
    }

}
