package net.lenni0451.classtransform.utils.attributes;

import lombok.SneakyThrows;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@ParametersAreNonnullByDefault
public class AttributeAccessor {

    @SneakyThrows
    public static byte[] getContent(final Attribute attribute) {
        Field content = Attribute.class.getDeclaredField("content");
        content.setAccessible(true);
        return (byte[]) content.get(attribute);
    }

    @SneakyThrows
    public static ByteVector newByteVector(final byte[] data) {
        Constructor<ByteVector> constructor = ByteVector.class.getDeclaredConstructor(byte[].class);
        constructor.setAccessible(true);
        return constructor.newInstance(new Object[]{data});
    }

}
