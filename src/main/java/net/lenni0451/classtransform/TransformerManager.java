package net.lenni0451.classtransform;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.impl.VoidMapper;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.targets.impl.*;
import net.lenni0451.classtransform.transformer.*;
import net.lenni0451.classtransform.transformer.impl.*;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class TransformerManager implements ClassFileTransformer {

    private final IClassProvider classProvider;
    private final AMapper mapper;
    private final List<ATransformer> internalTransformer = new ArrayList<>();
    private final Map<String, IInjectionTarget> injectionTargets = new HashMap<>();

    private final List<ITransformerPreprocessor> transformerPreprocessor = new ArrayList<>();
    private final List<IBytecodeTransformer> bytecodeTransformer = new ArrayList<>();
    private final Map<String, List<IRawTransformer>> rawTransformer = new HashMap<>();
    private final Map<String, List<ClassNode>> transformer = new HashMap<>();
    private final List<IPostTransformer> postTransformConsumer = new ArrayList<>();

    /**
     * @param classProvider The {@link ClassLoader} to use for transformer loading
     */
    public TransformerManager(final IClassProvider classProvider) {
        this(classProvider, new VoidMapper());
    }

    /**
     * @param classProvider The {@link ClassLoader} to use for transformer loading
     * @param mapper        The {@link AMapper} instance
     */
    public TransformerManager(final IClassProvider classProvider, final AMapper mapper) {
        this.classProvider = classProvider;
        this.mapper = mapper;
        this.mapper.load();

        //Annotation transformer
        this.internalTransformer.add(new CUpgradeTransformer());
        this.internalTransformer.add(new CASMTransformer());
        this.internalTransformer.add(new CShadowTransformer());
        this.internalTransformer.add(new COverrideTransformer());
        this.internalTransformer.add(new CWrapCatchTransformer());
        this.internalTransformer.add(new CInjectTransformer());
        this.internalTransformer.add(new CRedirectTransformer());
        this.internalTransformer.add(new CModifyConstantTransformer());
        //General transformer
        this.internalTransformer.add(new InnerClassTransformer());
        this.internalTransformer.add(new MemberCopyTransformer());

        //Injection targets
        this.injectionTargets.put("HEAD", new HeadTarget());
        this.injectionTargets.put("RETURN", new ReturnTarget());
        this.injectionTargets.put("THROW", new ThrowTarget());
        this.injectionTargets.put("TAIL", new TailTarget());
        this.injectionTargets.put("INVOKE", new InvokeTarget());
        this.injectionTargets.put("FIELD", new FieldTarget());
        this.injectionTargets.put("GETFIELD", new FieldTarget(Opcodes.GETFIELD, Opcodes.GETSTATIC));
        this.injectionTargets.put("PUTFIELD", new FieldTarget(Opcodes.PUTFIELD, Opcodes.PUTSTATIC));
        this.injectionTargets.put("NEW", new NewTarget());
        this.injectionTargets.put("OPCODE", new OpcodeTarget());
    }

    /**
     * Add a transformer preprocessor to the transformer list<br>
     * You can modify class transform annotations before they get parsed
     *
     * @param transformerPreprocessor The {@link ITransformerPreprocessor} instance
     */
    public void addTransformerPreprocessor(final ITransformerPreprocessor transformerPreprocessor) {
        this.transformerPreprocessor.add(transformerPreprocessor);
    }

    /**
     * Add a bytecode transformer to the transformer list<br>
     * Useful when needing to modify bytecode directly without ASM parsing or all {@link Class}es at once
     *
     * @param bytecodeTransformer The {@link IBytecodeTransformer} instance
     */
    public void addBytecodeTransformer(final IBytecodeTransformer bytecodeTransformer) {
        this.bytecodeTransformer.add(bytecodeTransformer);
    }

    /**
     * Add a raw class transformer to the transformer list<br>
     * Useful when needing direct access to a {@link ClassNode}
     *
     * @param className      The name of the transformer target
     * @param rawTransformer The {@link IRawTransformer} instance
     */
    public void addRawTransformer(final String className, final IRawTransformer rawTransformer) {
        this.rawTransformer.computeIfAbsent(className, n -> new ArrayList<>()).add(rawTransformer);
    }

    /**
     * Add a transformer class to the transformer list<br>
     * Use the direct class name for a single transformer <i>(e.g. <b>package.Transformer</b>)</i><br>
     * Use the package ending with '*' for all transformer in the packet (not sub packages) <i>(e.g. <b>package.*</b>)</i><br>
     * Use the package ending with '**' for all transformer in the package and sub packages <i>(e.g. <b>package.**</b>)</i><br>
     *
     * @param transformer The name of transformer class to add
     */
    public void addTransformer(final String transformer) {
        List<byte[]> classes = new ArrayList<>();
        if (transformer.endsWith(".**")) {
            String packageName = transformer.substring(0, transformer.length() - 2);
            for (Map.Entry<String, Supplier<byte[]>> entry : this.classProvider.getAllClasses().entrySet()) {
                if (entry.getKey().startsWith(packageName)) classes.add(entry.getValue().get());
            }
        } else if (transformer.endsWith(".*")) {
            String packageName = transformer.substring(0, transformer.length() - 1);
            for (Map.Entry<String, Supplier<byte[]>> entry : this.classProvider.getAllClasses().entrySet()) {
                if (entry.getKey().startsWith(packageName)) {
                    String classPackage = entry.getKey().substring(0, entry.getKey().lastIndexOf('.') + 1);
                    if (classPackage.equals(packageName)) classes.add(entry.getValue().get());
                }
            }
        } else {
            classes.add(this.classProvider.getClass(transformer));
        }
        for (byte[] bytecode : classes) {
            String name = null;
            try {
                ClassNode classNode = ASMUtils.fromBytes(bytecode);
                name = classNode.name;
                this.addTransformer(classNode);
            } catch (Throwable e) {
                if (name == null) throw new RuntimeException("Unable to parse transformer bytecode", e);
                else throw new RuntimeException("Unable to load transformer '" + name + "'", e);
            }
        }
    }

    /**
     * Add a {@link ClassNode} directly to the transformer list<br>
     * The class must still be annotated with {@link CTransformer}
     *
     * @param classNode The {@link ClassNode} to add
     */
    public void addTransformer(final ClassNode classNode) {
        for (ITransformerPreprocessor preprocessor : this.transformerPreprocessor) preprocessor.process(classNode);
        List<Object> annotation;
        if (classNode.invisibleAnnotations == null || (annotation = classNode.invisibleAnnotations.stream().filter(a -> a.desc.equals(Type.getDescriptor(CTransformer.class))).map(a -> a.values).findFirst().orElse(null)) == null) {
            throw new IllegalStateException("Transformer does not have CTransformer annotation");
        }
        for (int i = 0; i < annotation.size(); i += 2) {
            String key = (String) annotation.get(i);
            Object value = annotation.get(i + 1);

            if (key.equals("value")) {
                List<Type> classesList = (List<Type>) value;
                for (Type type : classesList) {
                    this.transformer
                            .computeIfAbsent(this.mapper.mapClassName(type.getClassName()), n -> new ArrayList<>())
                            .add(classNode);
                }
            } else if (key.equals("name")) {
                List<String> classesList = (List<String>) value;
                for (String className : classesList) {
                    this.transformer
                            .computeIfAbsent(this.mapper.mapClassName(className), n -> new ArrayList<>())
                            .add(classNode);
                }
            }
        }
    }

    /**
     * Add a post transform consumer to handle the raw byte array after all transformer have been applied<br>
     * Useful for dumping transformed classes to disk
     *
     * @param consumer The {@link BiConsumer} instance
     */
    public void addPostTransformConsumer(final IPostTransformer consumer) {
        this.postTransformConsumer.add(consumer);
    }

    /**
     * Transform the bytecode of a given class
     *
     * @param name     The name of the class
     * @param bytecode The bytecode of the class
     * @return The modified bytecode of the class (if there were transformer present)
     */
    public byte[] transform(final String name, byte[] bytecode) {
        ClassNode clazz = null;

        for (IBytecodeTransformer transformer : this.bytecodeTransformer) bytecode = transformer.transform(name, bytecode);

        List<IRawTransformer> rawTransformer = this.rawTransformer.get(name);
        if (rawTransformer != null) {
            clazz = ASMUtils.fromBytes(bytecode);
            for (IRawTransformer transformer : rawTransformer) {
                clazz = transformer.transformer(this, clazz);
            }
        }

        List<ClassNode> transformer = this.transformer.get(name);
        if (transformer != null) {
            if (clazz == null) clazz = ASMUtils.fromBytes(bytecode);
            for (ClassNode classNode : transformer) {
                try {
                    classNode = ASMUtils.cloneClass(classNode);
                    classNode = this.mapper.mapClass(this.classProvider, clazz, classNode);
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                for (ATransformer aTransformer : this.internalTransformer) {
                    try {
                        aTransformer.transform(this, this.classProvider, this.injectionTargets, clazz, classNode);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }

        if (clazz == null) return bytecode;
        byte[] transformedBytecode = ASMUtils.toBytes(clazz, this.classProvider);
        for (IPostTransformer postTransformer : this.postTransformConsumer) postTransformer.transform(name, transformedBytecode);
        return transformedBytecode;
    }

    /**
     * Get a list of all transformed classes<br>
     * This is useful to redefine already loaded classes
     */
    public List<String> getTransformedClasses() {
        List<String> transformedClasses = new ArrayList<>();
        transformedClasses.addAll(this.transformer.keySet());
        transformedClasses.addAll(this.rawTransformer.keySet());
        return Collections.unmodifiableList(transformedClasses);
    }

    /**
     * Hook an {@link Instrumentation} instance to allow for transformation using it<br>
     * This can be used to transform classes already loaded by the JVM<br>
     * You have to be careful with re-transforming classes since you can't modify the structure (e.g. adding a new method or modifying the signature of an existing one)
     *
     * @param instrumentation The instance of the {@link Instrumentation}
     * @throws UnmodifiableClassException If a class could not be redefined
     */
    public void hookInstrumentation(final Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(this, instrumentation.isRetransformClassesSupported());

        List<String> transformedClasses = this.getTransformedClasses();
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            if (loadedClass != null && transformedClasses.contains(loadedClass.getName())) {
                instrumentation.retransformClasses(loadedClass);
            }
        }
    }

    /**
     * Support method for {@link java.lang.instrument.Instrumentation}<br>
     * You can simply add the {@link TransformerManager} as a {@link ClassFileTransformer} using {@link java.lang.instrument.Instrumentation#addTransformer(ClassFileTransformer)} or call {@link TransformerManager#hookInstrumentation(Instrumentation)}
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) return null;
        try {
            byte[] newBytes = transform(className.replace("/", "."), classfileBuffer);
            if (!Arrays.equals(newBytes, classfileBuffer)) return newBytes;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

}
