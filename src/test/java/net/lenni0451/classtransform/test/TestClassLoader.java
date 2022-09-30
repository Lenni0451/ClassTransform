package net.lenni0451.classtransform.test;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.ClassNode;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;

public class TestClassLoader extends ClassLoader {

    private static final IClassProvider CLASS_PROVIDER = new BasicClassProvider();
    private static final ThreadLocal<TestClassLoader> LOADERS = ThreadLocal.withInitial(TestClassLoader::new);

    public static Class<?> load(final ClassNode classNode) {
        ClassNode newNode = Remapper.remap(classNode.name, classNode.name + System.nanoTime(), classNode);
        byte[] bytes = ASMUtils.toBytes(newNode, CLASS_PROVIDER);
        return LOADERS.get().defineClass(dot(newNode.name), bytes, 0, bytes.length);
    }


    private TestClassLoader() {
        ClassLoader.registerAsParallelCapable();
    }

}
