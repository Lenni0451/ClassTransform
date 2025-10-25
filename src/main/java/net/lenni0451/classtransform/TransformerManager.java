package net.lenni0451.classtransform;

import lombok.SneakyThrows;
import net.lenni0451.classtransform.annotations.CInline;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.debugger.TransformerTimings;
import net.lenni0451.classtransform.debugger.timings.TimedGroup;
import net.lenni0451.classtransform.exceptions.TransformerLoadException;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.impl.VoidMapper;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.targets.impl.*;
import net.lenni0451.classtransform.transformer.*;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.coprocessor.impl.CLocalVariableCoprocessor;
import net.lenni0451.classtransform.transformer.coprocessor.impl.CSharedCoprocessor;
import net.lenni0451.classtransform.transformer.impl.*;
import net.lenni0451.classtransform.transformer.impl.general.InnerClassGeneralHandler;
import net.lenni0451.classtransform.transformer.impl.general.MemberCopyGeneralHandler;
import net.lenni0451.classtransform.transformer.impl.general.SyntheticMethodGeneralHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.HotswapClassLoader;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.log.Logger;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;
import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * The TransformerManager handles all things needed for class transformation.<br>
 * This class implements {@link ClassFileTransformer} so it can be used with an {@link Instrumentation} agent.
 *
 * @see <a href="https://github.com/Lenni0451/ClassTransform/wiki">GitHub Wiki</a>
 */
@ParametersAreNonnullByDefault
public class TransformerManager implements ClassFileTransformer {

    private final ClassTree classTree = new ClassTree(this);
    private final IClassProvider classProvider;
    private final AMapper mapper;
    private final List<AnnotationHandler> annotationHandler = new ArrayList<>();
    private final AnnotationCoprocessorList coprocessors = new AnnotationCoprocessorList();
    private final Map<String, IInjectionTarget> injectionTargets = new HashMap<>();
    private final TransformerDebugger debugger = new TransformerDebugger(this);
    private FailStrategy failStrategy = FailStrategy.EXIT;
    private Instrumentation instrumentation;
    private HotswapClassLoader hotswapClassLoader;

    private final List<IAnnotationHandlerPreprocessor> annotationHandlerPreprocessor = new ArrayList<>();
    final List<IBytecodeTransformer> bytecodeTransformer = new ArrayList<>();
    final Map<String, List<IRawTransformer>> rawTransformer = new HashMap<>();
    final Map<String, List<ClassNode>> transformer = new HashMap<>();
    final List<IPostTransformer> postTransformer = new ArrayList<>();

    final Set<String> registeredTransformer = new HashSet<>();
    final Set<String> transformedClasses = new HashSet<>();

    /**
     * @param classProvider The class provider used to get the class bytecode
     */
    public TransformerManager(final IClassProvider classProvider) {
        this(classProvider, new VoidMapper());
    }

    /**
     * @param classProvider The class provider used to get the class bytecode
     * @param mapper        The mapper used to remap ClassTransform annotation targets and transformers if enabled
     */
    public TransformerManager(final IClassProvider classProvider, final AMapper mapper) {
        this.classProvider = classProvider;
        this.mapper = mapper;
        this.mapper.load();

        //Annotation handler
        this.annotationHandler.add(new CASMAnnotationHandler(CASM.Shift.TOP));
        this.annotationHandler.add(new CStubAnnotationHandler());
        this.annotationHandler.add(new InnerClassGeneralHandler()); //Make inner classes public to allow access from the transformed class
        this.annotationHandler.add(new SyntheticMethodGeneralHandler()); //Rename synthetic members to be unique
        this.annotationHandler.add(new CShadowAnnotationHandler());
        this.annotationHandler.add(new CRecordComponentAnnotationHandler());
        this.annotationHandler.add(new MemberCopyGeneralHandler(true)); //Copy all interfaces, fields and initializers to the transformed class
        //HandlerPosition#PRE
        this.annotationHandler.add(new COverrideAnnotationHandler());
        this.annotationHandler.add(new CWrapCatchAnnotationHandler());
        this.annotationHandler.add(new CInjectAnnotationHandler());
        this.annotationHandler.add(new CModifyExpressionValueAnnotationHandler());
        this.annotationHandler.add(new CModifyConstantAnnotationHandler());
        this.annotationHandler.add(new CWrapConditionAnnotationHandler());
        this.annotationHandler.add(new CRedirectAnnotationHandler()); //Should be last because it replaces the method node which prevents other handlers from targeting the method
        //HandlerPosition#POST
        this.annotationHandler.add(new CUpgradeAnnotationHandler());
        this.annotationHandler.add(new MemberCopyGeneralHandler(false)); //Copy all leftover methods to the transformed class
        this.annotationHandler.add(new CInlineAnnotationHandler());
        this.annotationHandler.add(new CReplaceCallbackAnnotationHandler());
        this.annotationHandler.add(new CASMAnnotationHandler(CASM.Shift.BOTTOM));

        //Annotation coprocessors
        this.coprocessors.add(CLocalVariableCoprocessor::new);
        this.coprocessors.add(CSharedCoprocessor::new);

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
        this.injectionTargets.put("CONSTANT", new ConstantTarget());
    }

    /**
     * @return The class tree
     */
    public ClassTree getClassTree() {
        return this.classTree;
    }

    /**
     * @return The class provider
     */
    public IClassProvider getClassProvider() {
        return this.classProvider;
    }

    /**
     * @return The mapper
     */
    public AMapper getMapper() {
        return this.mapper;
    }

    /**
     * @return The names of all registered transformers
     */
    public Set<String> getRegisteredTransformer() {
        return Collections.unmodifiableSet(this.registeredTransformer);
    }

    /**
     * @return The names of all transformed classes
     */
    public Set<String> getTransformedClasses() {
        return Collections.unmodifiableSet(this.transformedClasses);
    }

    /**
     * Add a new annotation handler coprocessor.<br>
     * A new coprocessor instance will be created for each annotation handler.
     *
     * @param coprocessorSupplier The coprocessor supplier
     */
    public void addCoprocessor(final Supplier<? extends IAnnotationCoprocessor> coprocessorSupplier) {
        this.coprocessors.add(coprocessorSupplier);
    }

    /**
     * Create and get a new array of all coprocessors.
     *
     * @return The coprocessors
     */
    public AnnotationCoprocessorList getCoprocessors() {
        return this.coprocessors.build();
    }

    /**
     * @return The injection targets
     */
    public Map<String, IInjectionTarget> getInjectionTargets() {
        return Collections.unmodifiableMap(this.injectionTargets);
    }

    /**
     * Get an injection target by its name.<br>
     * The name is case-insensitive.
     *
     * @param name The name of the injection target
     * @return The injection target
     */
    public Optional<IInjectionTarget> getInjectionTarget(final String name) {
        return Optional.ofNullable(this.injectionTargets.get(name.toUpperCase(Locale.ROOT)));
    }

    /**
     * @return The debugger instance
     */
    public TransformerDebugger getDebugger() {
        return this.debugger;
    }

    /**
     * Set the fail strategy used when a transformer fails.
     *
     * @param failStrategy The fail strategy to use
     */
    public void setFailStrategy(final FailStrategy failStrategy) {
        this.failStrategy = failStrategy;
    }

    /**
     * @return The current fail strategy
     */
    public FailStrategy getFailStrategy() {
        return this.failStrategy;
    }

    /**
     * @return The instrumentation instance if hooked
     */
    @Nullable
    public Instrumentation getInstrumentation() {
        return this.instrumentation;
    }

    /**
     * Add an annotation handler preprocessor to the preprocessor list.<br>
     * You can modify class transform annotations before they get parsed.
     *
     * @param annotationHandlerPreprocessor The annotation handler preprocessor to add
     */
    public void addTransformerPreprocessor(final IAnnotationHandlerPreprocessor annotationHandlerPreprocessor) {
        this.annotationHandlerPreprocessor.add(annotationHandlerPreprocessor);
    }

    /**
     * Add a bytecode transformer to the transformer list.<br>
     * Bytecode transformer are the ClassTransform implementation of {@link ClassFileTransformer} .
     *
     * @param bytecodeTransformer The bytecode transformer to add
     */
    public void addBytecodeTransformer(final IBytecodeTransformer bytecodeTransformer) {
        this.bytecodeTransformer.add(bytecodeTransformer);
    }

    /**
     * Add a {@link ClassFileTransformer} to the transformer list.<br>
     * The {@code classLoader} is directly passed to the transformer.
     *
     * @param classLoader          The class loader of the class
     * @param classFileTransformer The class file transformer to add
     */
    public void addClassFileTransformer(final ClassLoader classLoader, final ClassFileTransformer classFileTransformer) {
        //noinspection Convert2Lambda
        this.addBytecodeTransformer(new IBytecodeTransformer() {
            @Override
            @SneakyThrows
            public byte[] transform(String className, byte[] bytecode, boolean calculateStackMapFrames) {
                return classFileTransformer.transform(classLoader, slash(className), null, null, bytecode);
            }
        });
    }

    /**
     * Add a raw class transformer to the transformer list.<br>
     * Raw class transformer are similar to bytecode transformer but only for the specified class.<br>
     * A {@link ClassNode} is passed to the transformer instead of the bytecode array.
     *
     * @param className      The name of the class to transform
     * @param rawTransformer The raw transformer to add
     */
    public void addRawTransformer(final String className, final IRawTransformer rawTransformer) {
        this.rawTransformer.computeIfAbsent(className, n -> new ArrayList<>()).add(rawTransformer);
        this.transformedClasses.add(className);
        this.retransformClasses(Collections.singleton(className));
    }

    /**
     * Add a transformer class to the transformer list.<br>
     * Use the direct class name for a single transformer <i>(e.g. <b>package.Transformer</b>)</i><br>
     * Use the package ending with '*' for all transformer in the packet (not sub packages) <i>(e.g. <b>package.*</b>)</i><br>
     * Use the package ending with '**' for all transformer in the package and sub packages <i>(e.g. <b>package.**</b>)</i><br>
     * If the class is specified directly an exception will be thrown if the class is missing the {@link CTransformer} annotation.
     *
     * @param transformer The name of transformer class to add
     * @throws IllegalStateException    If the class is specified directly and is missing the {@link CTransformer} annotation
     * @throws TransformerLoadException If the transformer could not be loaded
     * @throws RuntimeException         If the class bytecode could not be parsed using ASM
     */
    public void addTransformer(final String transformer) {
        List<byte[]> classes = new ArrayList<>();
        boolean wildcard = false;
        if (transformer.endsWith(".**")) {
            wildcard = true;
            String packageName = transformer.substring(0, transformer.length() - 2);
            for (Map.Entry<String, Supplier<byte[]>> entry : this.classProvider.getAllClasses().entrySet()) {
                try {
                    if (entry.getKey().startsWith(packageName)) classes.add(entry.getValue().get());
                } catch (Throwable t) {
                    throw new TransformerLoadException(entry.getKey(), t);
                }
            }
        } else if (transformer.endsWith(".*")) {
            wildcard = true;
            String packageName = transformer.substring(0, transformer.length() - 1);
            for (Map.Entry<String, Supplier<byte[]>> entry : this.classProvider.getAllClasses().entrySet()) {
                if (entry.getKey().startsWith(packageName)) {
                    try {
                        String classPackage = entry.getKey().substring(0, entry.getKey().lastIndexOf('.') + 1);
                        if (classPackage.equals(packageName)) classes.add(entry.getValue().get());
                    } catch (Throwable t) {
                        throw new TransformerLoadException(entry.getKey(), t);
                    }
                }
            }
        } else {
            try {
                classes.add(this.classProvider.getClass(transformer));
            } catch (ClassNotFoundException e) {
                throw new TransformerLoadException(transformer, e);
            }
        }
        for (byte[] bytecode : classes) {
            String name = null;
            try {
                ClassNode classNode = ASMUtils.fromBytes(bytecode);
                name = classNode.name;
                Set<String> transformedClasses = this.addTransformer(classNode, !wildcard, false);
                if (!transformedClasses.isEmpty()) this.retransformClasses(transformedClasses);
                else if (!wildcard) Logger.warn("Transformer '{}' does not transform any classes", name);
            } catch (Throwable e) {
                if (name == null) throw new RuntimeException("Unable to parse transformer bytecode", e);
                else throw new TransformerLoadException(name, e);
            }
        }
    }

    /**
     * Add a {@link ClassNode} directly to the transformer list.<br>
     * The class must still be annotated with {@link CTransformer}.
     *
     * @param classNode The class node to add
     * @return A set of all transformed classes
     * @throws IllegalStateException If the class is missing the {@link CTransformer} annotation
     */
    public Set<String> addTransformer(final ClassNode classNode) {
        return this.addTransformer(classNode, true);
    }

    /**
     * Add a {@link ClassNode} directly to the transformer list.<br>
     * The class must still be annotated with {@link CTransformer}.
     *
     * @param classNode         The class node to add
     * @param requireAnnotation If an exception should be thrown if the class is missing the {@link CTransformer} annotation
     * @return A set of all transformed classes
     * @throws IllegalStateException If the class is missing the {@link CTransformer} annotation and {@code requireAnnotation} is {@code true}
     */
    public Set<String> addTransformer(ClassNode classNode, final boolean requireAnnotation) {
        return this.addTransformer(classNode, requireAnnotation, false);
    }

    /**
     * Add a {@link ClassNode} directly to the transformer list.<br>
     * The class must still be annotated with {@link CTransformer}.
     *
     * @param classNode          The class node to add
     * @param requireAnnotation  If an exception should be thrown if the class is missing the {@link CTransformer} annotation
     * @param retransformClasses Whether to retransform classes after adding the transformer (if instrumentation is hooked)
     * @return A set of all transformed classes
     * @throws IllegalStateException If the class is missing the {@link CTransformer} annotation and {@code requireAnnotation} is {@code true}
     */
    public Set<String> addTransformer(ClassNode classNode, final boolean requireAnnotation, final boolean retransformClasses) {
        for (IAnnotationHandlerPreprocessor preprocessor : this.annotationHandlerPreprocessor) {
            preprocessor.process(classNode);
            classNode = preprocessor.replace(classNode);
        }
        Optional<AnnotationNode> opt = AnnotationUtils.findAnnotation(classNode, CTransformer.class);
        if (!opt.isPresent()) {
            if (requireAnnotation) throw new IllegalStateException("Transformer does not have CTransformer annotation");
            else return Collections.emptySet();
        }
        List<Object> annotation = opt.map(a -> a.values).orElseGet(Collections::emptyList);
        Set<String> transformedClasses = new HashSet<>();
        for (int i = 0; i < annotation.size(); i += 2) {
            String key = (String) annotation.get(i);
            Object value = annotation.get(i + 1);

            if (key.equals("value")) {
                List<Type> classesList = (List<Type>) value;
                for (Type type : classesList) this.addTransformer(transformedClasses, this.mapper.mapClassName(type.getClassName()), classNode);
            } else if (key.equals("name")) {
                List<String> classesList = (List<String>) value;
                for (String className : classesList) this.addTransformer(transformedClasses, this.mapper.mapClassName(className), classNode);
            }
        }
        this.transformedClasses.addAll(transformedClasses);

        String name = dot(classNode.name);
        this.registeredTransformer.add(name);
        if (this.hotswapClassLoader != null) this.hotswapClassLoader.defineHotswapClass(name);
        if (!transformedClasses.isEmpty() && retransformClasses) this.retransformClasses(transformedClasses);
        return transformedClasses;
    }

    private void addTransformer(final Set<String> transformedClasses, final String className, final ClassNode transformer) {
        List<ClassNode> transformerList = this.transformer.computeIfAbsent(className, n -> new ArrayList<>());
        transformerList.removeIf(cn -> cn.name.equals(transformer.name));
        transformerList.add(transformer);

        transformedClasses.add(className);
    }

    /**
     * Add a post transformer to handle the raw bytecode after all transformer have been applied.<br>
     * Useful for dumping transformed classes to disk.
     *
     * @param postTransformer The {@link BiConsumer} instance
     */
    public void addPostTransformConsumer(final IPostTransformer postTransformer) {
        this.postTransformer.add(postTransformer);
    }

    /**
     * Add a custom annotation handler into the handler chain.
     *
     * @param transformer     The annotation handler to add
     * @param handlerPosition The position where to add the handler
     */
    public void addCustomAnnotationHandler(final AnnotationHandler transformer, final HandlerPosition handlerPosition) {
        handlerPosition.add(this.annotationHandler, transformer);
    }

    /**
     * Add a new injection target for use with the {@link CTarget} annotation.
     *
     * @param name   The name of the injection target
     * @param target The injection target
     */
    public void addInjectionTarget(final String name, final IInjectionTarget target) {
        this.injectionTargets.put(name.toUpperCase(Locale.ROOT), target);
    }

    /**
     * Transform the bytecode of the given class.<br>
     * The name must be in the class format (e.g. {@code java.lang.String}).
     *
     * @param name     The name of the class
     * @param bytecode The bytecode of the class
     * @return The modified bytecode of the class or null if not changed
     */
    @Nullable
    public byte[] transform(final String name, byte[] bytecode) {
        return this.transform(name, bytecode, true);
    }

    /**
     * Transform the bytecode of the given class.<br>
     * The name must be in the class format (e.g. {@code java.lang.String}).
     *
     * @param name                    The name of the class
     * @param bytecode                The bytecode of the class
     * @param calculateStackMapFrames If the stack map frames should be calculated
     * @return The modified bytecode of the class or null if not changed
     */
    @Nullable
    public byte[] transform(final String name, byte[] bytecode, final boolean calculateStackMapFrames) {
        TransformerTimings timings = new TransformerTimings();
        try {
            boolean transformed = false;
            ClassNode clazz = null;

            for (IBytecodeTransformer transformer : this.bytecodeTransformer) {
                timings.start(TimedGroup.BYTECODE_TRANSFORMER, transformer.getClass().getName());
                byte[] transformedBytecode = transformer.transform(name, bytecode, calculateStackMapFrames);
                timings.end();
                if (transformedBytecode != null) {
                    transformed = true;
                    bytecode = transformedBytecode;
                }
            }

            List<IRawTransformer> rawTransformer = this.rawTransformer.get(name);
            if (rawTransformer != null) {
                clazz = ASMUtils.fromBytes(bytecode);
                for (IRawTransformer transformer : rawTransformer) {
                    timings.start(TimedGroup.RAW_TRANSFORMER, transformer.getClass().getName());
                    clazz = transformer.transform(this, clazz);
                    timings.end();
                }
            }

            List<ClassNode> transformer = this.transformer.get(name);
            if (transformer != null) {
                if (clazz == null) clazz = ASMUtils.fromBytes(bytecode);
                for (ClassNode classNode : transformer) {
                    timings.start(TimedGroup.REMAPPER, classNode.name);
                    try {
                        classNode = ASMUtils.cloneClass(classNode);
                        classNode = this.mapper.mapClass(this.classTree, this.classProvider, clazz, classNode);
                    } catch (Throwable t) {
                        Logger.error("Failed to remap and fill annotation details of transformer '{}'", classNode.name, t);
                        if (FailStrategy.CANCEL.equals(this.failStrategy)) return null;
                        else if (FailStrategy.EXIT.equals(this.failStrategy)) System.exit(-1);
                    }
                    timings.end();

                    for (AnnotationHandler annotationHandler : this.annotationHandler) {
                        timings.start(TimedGroup.ANNOTATION_HANDLER, annotationHandler.getClass().getName());
                        try {
                            annotationHandler.transform(this, clazz, classNode);
                        } catch (Throwable t) {
                            Logger.error("Transformer '{}' failed to transform class '{}'", annotationHandler.getClass().getSimpleName(), clazz.name, t);
                            if (FailStrategy.CANCEL.equals(this.failStrategy)) return null;
                            else if (FailStrategy.EXIT.equals(this.failStrategy)) System.exit(-1);
                        }
                        timings.end();
                    }
                }
            }

            if (clazz == null) {
                if (transformed) return bytecode;
                return null;
            }
            byte[] transformedBytecode;
            if (calculateStackMapFrames) transformedBytecode = ASMUtils.toBytes(clazz, this.classTree, this.classProvider);
            else transformedBytecode = ASMUtils.toStacklessBytes(clazz);
            for (IPostTransformer postTransformer : this.postTransformer) {
                timings.start(TimedGroup.POST_TRANSFORMER, postTransformer.getClass().getName());
                byte[] replacementBytecode = postTransformer.replace(name, transformedBytecode);
                if (replacementBytecode != null) transformedBytecode = replacementBytecode;
                timings.end();
            }
            if (this.debugger.isDumpClasses()) {
                try {
                    Path path = Paths.get(".", ".classtransform", "dump", name.replace(".", FileSystems.getDefault().getSeparator()) + ".class");
                    Files.createDirectories(path.getParent());
                    Files.write(path, transformedBytecode);
                } catch (Throwable t) {
                    Logger.error("Failed to dump class '{}'", name, t);
                }
            }
            return transformedBytecode;
        } catch (Throwable t) {
            Logger.error("Failed to transform class '{}'", name, t);
            if (FailStrategy.EXIT.equals(this.failStrategy)) System.exit(-1);
            throw t;
        } finally {
            this.getDebugger().addTimings(name, timings.getTimings());
        }
    }

    /**
     * Hook an {@link Instrumentation} instance to allow for transformation using it.<br>
     * This allows to transform classes already loaded by the JVM.<br>
     * You have to be careful with re-transforming classes since you can't modify the class structure (e.g. adding a new method or modifying the signature of an existing one). You can use the {@link CInline} annotation to prevent adding methods to a loaded class.<br>
     * When using this method all loaded classes will be re-transformed if there is a transformer for them.<br>
     * Hotswapping transformer is disabled by default as it causes a bit more overhead and memory usage.
     *
     * @param instrumentation The instrumentation instance to hook
     */
    public void hookInstrumentation(final Instrumentation instrumentation) {
        this.hookInstrumentation(instrumentation, false);
    }

    /**
     * Hook an {@link Instrumentation} instance to allow for transformation using it.<br>
     * This allows to transform classes already loaded by the JVM.<br>
     * You have to be careful with re-transforming classes since you can't modify the class structure (e.g. adding a new method or modifying the signature of an existing one). You can use the {@link CInline} annotation to prevent adding methods to a loaded class.<br>
     * When using this method all loaded classes will be re-transformed if there is a transformer for them.
     *
     * @param instrumentation The instrumentation instance to hook
     * @param hotswappable    Whether to enable transformer hotswapping
     */
    public void hookInstrumentation(final Instrumentation instrumentation, final boolean hotswappable) {
        this.instrumentation = instrumentation;
        if (hotswappable) {
            this.hotswapClassLoader = new HotswapClassLoader();
            for (String transformerClass : this.registeredTransformer) this.hotswapClassLoader.defineHotswapClass(transformerClass);
        }
        instrumentation.addTransformer(this, instrumentation.isRetransformClassesSupported());

        this.retransformClasses(null);
    }

    private void retransformClasses(@Nullable final Set<String> classesToRetransform) {
        if (this.instrumentation != null && this.instrumentation.isRetransformClassesSupported()) {
            List<Class<?>> classes = new ArrayList<>();
            Set<String> classSet;
            if (classesToRetransform != null) classSet = classesToRetransform;
            else classSet = this.transformedClasses;
            for (Class<?> loadedClass : this.instrumentation.getAllLoadedClasses()) {
                if (loadedClass != null && classSet.contains(loadedClass.getName())) classes.add(loadedClass);
            }
            if (!classes.isEmpty()) {
                try {
                    this.instrumentation.retransformClasses(classes.toArray(new Class[0]));
                } catch (Throwable t) {
                    Logger.error("Failed to retransform classes '{}'", classes.stream().map(Class::getName).collect(Collectors.joining(", ")), t);
                    if (FailStrategy.EXIT.equals(this.failStrategy)) System.exit(-1);
                }
            }
        }
    }

    private void redefineClasses(final Set<String> classesToRedefine) throws UnmodifiableClassException, ClassNotFoundException {
        List<ClassDefinition> classDefinitions = new ArrayList<>();
        for (Class<?> loadedClass : this.instrumentation.getAllLoadedClasses()) {
            if (loadedClass != null && classesToRedefine.contains(loadedClass.getName())) {
                byte[] transformedBytecode = this.transform(loadedClass.getName(), this.classProvider.getClass(loadedClass.getName()));
                if (transformedBytecode != null) classDefinitions.add(new ClassDefinition(loadedClass, transformedBytecode));
            }
        }
        if (!classDefinitions.isEmpty()) this.instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));
    }

    /**
     * Support method for hooking an instrumentation instance.<br>
     * You can simply add the TransformerManager as a transformer using {@link Instrumentation#addTransformer(ClassFileTransformer)} or call {@link TransformerManager#hookInstrumentation(Instrumentation)}.
     */
    @Override
    @Nullable
    public byte[] transform(@Nullable ClassLoader loader, @Nullable String className, @Nullable Class<?> classBeingRedefined, @Nullable ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) return null;
        try {
            className = dot(className);
            if (this.hotswapClassLoader != null && this.registeredTransformer.contains(className)) {
                try {
                    ClassNode transformer = ASMUtils.fromBytes(classfileBuffer);
                    Set<String> transformedClasses = this.addTransformer(transformer);
                    this.redefineClasses(transformedClasses);

                    return this.hotswapClassLoader.getHotswapClass(transformer.name);
                } catch (Throwable t) {
                    Logger.error("Failed to hotswap transformer '{}'", className, t);
                    return new byte[]{1}; //Tells the IDE something went wrong
                }
            }

            byte[] newBytes = this.transform(className, classfileBuffer);
            if (newBytes != null) return newBytes;
        } catch (Throwable t) {
            Logger.error("Failed to transform class '{}'", className, t);
            if (FailStrategy.EXIT.equals(this.failStrategy)) System.exit(-1);
        }
        return null;
    }

}
