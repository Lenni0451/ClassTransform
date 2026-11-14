package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.mappings.impl.ProguardMapper;
import net.lenni0451.classtransform.mappings.impl.SrgMapper;
import net.lenni0451.classtransform.mappings.impl.TinyV2Mapper;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionMappingFormatsTest {

    private static final String TARGET = "net/lenni0451/classtransform/transformer/impl/ProductionMappingFormatsTest$MappingTarget";
    private static final String TARGET_DESC = "L" + TARGET + ";";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void srgMappingsRemapMethodAndField() throws Exception {
        String mapping = "MD: " + TARGET + "/mapped ()I " + TARGET + "/actual ()I\n"
                + "FD: " + TARGET + "/mappedField " + TARGET + "/fieldValue\n";
        Class<?> target = this.transform(MappingTransformer.class, new SrgMapper(MapperConfig.create(), stream(mapping)));
        assertEquals(15, target.getMethod("actual").invoke(null));
        assertEquals(42, target.getMethod("readField").invoke(null));
    }

    @Test
    public void tinyMappingsRemapMethodAndField() throws Exception {
        String mapping = "tiny\t2\t0\tofficial\tnamed\n"
                + "c\t" + TARGET + "\t" + TARGET + "\n"
                + "\tm\t()I\tmapped\tactual\n"
                + "\tf\tI\tmappedField\tfieldValue\n";
        Class<?> target = this.transform(MappingTransformer.class, new TinyV2Mapper(MapperConfig.create(), stream(mapping), "official", "named"));
        assertEquals(15, target.getMethod("actual").invoke(null));
        assertEquals(42, target.getMethod("readField").invoke(null));
    }

    @Test
    public void proguardMappingsRemapMethodAndField() throws Exception {
        String mapping = TARGET.replace('/', '.') + " -> " + TARGET.replace('/', '.') + ":\n"
                + "    1:1:int mapped() -> actual\n"
                + "    int mappedField -> fieldValue\n";
        Class<?> target = this.transform(MappingTransformer.class, new ProguardMapper(MapperConfig.create(), stream(mapping)));
        assertEquals(15, target.getMethod("actual").invoke(null));
        assertEquals(42, target.getMethod("readField").invoke(null));
    }

    private Class<?> transform(final Class<?> transformerClass, final AMapper mapper) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider, mapper);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        byte[] bytes = manager.transform(MappingTarget.class.getName(), this.classProvider.getClass(MappingTarget.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }

    private static ByteArrayInputStream stream(final String mapping) {
        return new ByteArrayInputStream(mapping.getBytes(StandardCharsets.UTF_8));
    }


    public static class MappingTarget {

        public static int fieldValue = 7;

        public static int actual() {
            return 5;
        }

        public static int readField() {
            return fieldValue;
        }

    }

    @CTransformer(MappingTarget.class)
    public static class MappingTransformer {

        @CModifyReturnValue(method = "mapped")
        public static int modify(final int original) {
            return original + 10;
        }

        @CRedirect(method = "readField", target = @CTarget(value = "FIELD", target = TARGET_DESC + "mappedField:I"))
        public static int redirect() {
            return 42;
        }

    }

}
