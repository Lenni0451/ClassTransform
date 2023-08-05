package net.lenni0451.classtransform.additionalclassprovider;

import com.google.common.reflect.ClassPath;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class GuavaClassPathProvider extends BasicClassProvider {

    private final ClassPath classPath;

    public GuavaClassPathProvider() {
        this(GuavaClassPathProvider.class.getClassLoader());
    }

    public GuavaClassPathProvider(final ClassLoader classLoader) {
        super(classLoader);

        try {
            this.classPath = ClassPath.from(classLoader);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize ClassPath", t);
        }
    }

    @Override
    @Nonnull
    public Map<String, Supplier<byte[]>> getAllClasses() {
        Map<String, Supplier<byte[]>> map = new HashMap<>();
        for (ClassPath.ClassInfo classInfo : this.classPath.getAllClasses()) map.put(classInfo.getName(), () -> this.getClass(classInfo.getName()));
        return map;
    }

}
