package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CCoerce;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

/**
 * Utils for checking {@link CCoerce} annotations and coercible types.
 */
@ParametersAreNonnullByDefault
public class CoerceUtils {

    /**
     * @param method         The handler method
     * @param parameterIndex The handler parameter index
     * @return If the parameter is marked as coerced
     */
    public static boolean hasCoerce(final MethodNode method, final int parameterIndex) {
        if (parameterIndex < 0) return false;
        Optional<AnnotationNode[]> annotations = AnnotationUtils.findParameterAnnotations(method, CCoerce.class);
        if (!annotations.isPresent()) return false;
        AnnotationNode[] values = annotations.get();
        return parameterIndex < values.length && values[parameterIndex] != null;
    }

    /**
     * @param method The handler method
     * @return If the return type is marked as coerced
     */
    public static boolean hasCoerceReturn(final MethodNode method) {
        return AnnotationUtils.findAnnotation(method, CCoerce.class).isPresent();
    }

    /**
     * Check whether a target value type can be passed to a handler parameter.
     *
     * @param expected The target value type
     * @param handler  The handler parameter type
     * @param coerced  If the handler parameter is marked as coerced
     * @return If the types are compatible
     */
    public static boolean canCoerce(final Type expected, final Type handler, final boolean coerced) {
        if (ASMUtils.compareType(expected, handler)) return true;
        if (!coerced) return false;
        if (isIntLike(expected) && isIntLike(handler)) return true;
        return expected.getSort() >= Type.ARRAY && handler.getSort() >= Type.ARRAY;
    }

    /**
     * Check whether a handler return value can be converted to the expected type.
     *
     * @param expected The expected value type
     * @param handler  The handler return type
     * @param coerced  If the handler method is marked as coerced
     * @return If the types are compatible
     */
    public static boolean canCoerceReturn(final Type expected, final Type handler, final boolean coerced) {
        return canCoerce(expected, handler, coerced);
    }

    /**
     * @param type The type
     * @return If the type is one of the int-like JVM types
     */
    public static boolean isIntLike(final Type type) {
        return type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.INT;
    }

}
