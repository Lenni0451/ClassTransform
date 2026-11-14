package net.lenni0451.classtransform.utils.local;

import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * Utils for the native local variable reference types.
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
     * @param type The type to check
     * @return If the type is a native local variable reference type
     */
    public static boolean isRef(final Type type) {
        return type.getSort() == Type.OBJECT && (type.getInternalName().equals(Type.getInternalName(CLocalRef.class)) || REF_TYPES.containsKey(type.getInternalName()));
    }

    /**
     * Get the type of the local variable a reference type points to.
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
