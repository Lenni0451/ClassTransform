package net.lenni0451.classtransform.utils.attributes;

import lombok.SneakyThrows;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Reflective access to the non-public parts of ASM needed for custom attributes.
 * <p>
 * ASM stores the raw payload of an {@link Attribute} in a private field and offers no public way to
 * build a {@link ByteVector} from existing bytes. This class reads and writes that payload directly
 * without class loading; it relies on reflective access to those members and fails with an unchecked
 * exception if the runtime does not allow it.
 */
@ParametersAreNonnullByDefault
public class AttributeAccessor {

    /**
     * Read the raw payload of an ASM attribute.
     *
     * @param attribute The attribute to read
     * @return The raw attribute content
     */
    @SneakyThrows
    public static byte[] getContent(final Attribute attribute) {
        Field content = Attribute.class.getDeclaredField("content");
        content.setAccessible(true);
        return (byte[]) content.get(attribute);
    }

    /**
     * Wrap raw bytes in a {@link ByteVector} for an attribute write method.
     *
     * @param data The bytes to wrap
     * @return A byte vector containing the given bytes
     */
    @SneakyThrows
    public static ByteVector newByteVector(final byte[] data) {
        Constructor<ByteVector> constructor = ByteVector.class.getDeclaredConstructor(byte[].class);
        constructor.setAccessible(true);
        return constructor.newInstance(new Object[]{data});
    }

}
