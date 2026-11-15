package net.lenni0451.classtransform.utils.local;

import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for the native local variable reference types.
 *
 * <p>The class contains the fixed mapping from the reference classes in this package to the ASM type they represent:
 * {@link CLocalBooleanRef}, {@link CLocalByteRef}, {@link CLocalCharRef}, {@link CLocalShortRef},
 * {@link CLocalIntRef}, {@link CLocalLongRef}, {@link CLocalFloatRef} and {@link CLocalDoubleRef} map to their
 * primitive type, while {@link CLocalRef} takes its type from the generic type argument of the transformer parameter.
 * The mapping is registered once in a static map and never modified afterwards.
 */
@ParametersAreNonnullByDefault
public class LocalRefUtils {

    private static final Map<String, Type> REF_TYPES = new HashMap<>();

    static {
        REF_TYPES.put(Type.getInternalName(CLocalBooleanRef.class), Type.BOOLEAN_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalByteRef.class), Type.BYTE_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalCharRef.class), Type.CHAR_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalShortRef.class), Type.SHORT_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalIntRef.class), Type.INT_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalLongRef.class), Type.LONG_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalFloatRef.class), Type.FLOAT_TYPE);
        REF_TYPES.put(Type.getInternalName(CLocalDoubleRef.class), Type.DOUBLE_TYPE);
    }

    /**
     * Checks whether the given type is one of the native local variable reference types.
     *
     * <p>Only object types are considered; the check for {@link CLocalRef} does not inspect generic type arguments.
     *
     * @param type The type to check
     * @return If the type is a native local variable reference type
     */
    public static boolean isRef(final Type type) {
        return type.getSort() == Type.OBJECT && (type.getInternalName().equals(Type.getInternalName(CLocalRef.class)) || REF_TYPES.containsKey(type.getInternalName()));
    }

    /**
     * Gets the type of the local variable a reference type points to.
     *
     * <p>The primitive reference types ignore the type argument, so it may be {@code null} for them.
     *
     * @param refType      The reference type
     * @param typeArgument The generic type argument of {@link CLocalRef}
     * @return The target type or null if the type is not a reference type or the type argument is missing
     */
    @Nullable
    public static Type getTargetType(final Type refType, @Nullable final Type typeArgument) {
        if (!isRef(refType)) return null;
        Type primitiveType = REF_TYPES.get(refType.getInternalName());
        if (primitiveType != null) return primitiveType;
        return typeArgument;
    }

}
