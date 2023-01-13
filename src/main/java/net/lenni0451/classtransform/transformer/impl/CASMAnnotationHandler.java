package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.types.RemovingAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.annotations.ClassDefiner;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.LambdaMetafactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.*;

public class CASMAnnotationHandler extends RemovingAnnotationHandler<CASM> {

    private final CASM.Shift shift;

    public CASMAnnotationHandler(final CASM.Shift shift) {
        super(CASM.class);
        this.shift = shift;
    }

    @Override
    public void transform(CASM annotation, TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        if (!Modifier.isStatic(transformerMethod.access)) {
            throw new TransformerException(transformerMethod, transformer, "must be static")
                    .help(Codifier.of(transformerMethod).access(transformerMethod.access | Modifier.STATIC));
        }
        Type[] args = argumentTypes(transformerMethod.desc);
        Type returnType = returnType(transformerMethod.desc);
        if (!returnType.equals(Type.VOID_TYPE)) {
            throw new TransformerException(transformerMethod, transformer, "must be a void method")
                    .help(Codifier.of(transformerMethod).returnType(Type.VOID_TYPE));
        }
        if (annotation.value().length == 0) {
            if (args.length != 1 || !type(ClassNode.class).equals(args[0])) {
                throw new TransformerException(transformerMethod, transformer, "must have one argument (ClassNode)")
                        .help(Codifier.of(transformerMethod).param(null).param(type(ClassNode.class)));
            }

            ClassDefiner<?> classDefiner = this.isolateMethod(classProvider, transformer, transformerMethod);
            try {
                Object instance = classDefiner.newInstance();
                Method isolatedMethod = classDefiner.getClazz().getDeclaredMethod(transformerMethod.name, ClassNode.class);
                isolatedMethod.setAccessible(true);
                isolatedMethod.invoke(instance, transformedClass);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to call isolated method '" + transformerMethod.name + "' of transformer '" + transformer.name + "'", t);
            }
        } else {
            if (args.length != 1 || !type(MethodNode.class).equals(args[0])) {
                throw new TransformerException(transformerMethod, transformer, "must have one argument (MethodNode)")
                        .help(Codifier.of(transformerMethod).param(null).param(type(MethodNode.class)));
            }

            ClassDefiner<?> classDefiner = this.isolateMethod(classProvider, transformer, transformerMethod);
            for (String targetCombi : annotation.value()) {
                List<MethodNode> targets = ASMUtils.getMethodsFromCombi(transformedClass, targetCombi);
                if (targets.isEmpty()) throw new MethodNotFoundException(transformedClass, transformer, targetCombi);
                for (MethodNode target : targets) {
                    try {
                        Object instance = classDefiner.newInstance();
                        Method isolatedMethod = classDefiner.getClazz().getDeclaredMethod(transformerMethod.name, MethodNode.class);
                        isolatedMethod.setAccessible(true);
                        isolatedMethod.invoke(instance, target);
                    } catch (Throwable t) {
                        throw new IllegalStateException("Failed to call isolated method '" + transformerMethod.name + "' of transformer '" + transformer.name + "'", t);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldExecute(CASM annotation) {
        return annotation.shift().equals(this.shift);
    }

    private ClassDefiner<?> isolateMethod(final IClassProvider classProvider, final ClassNode transformer, final MethodNode transformerMethod) {
        try {
            ClassNode classNode = new ClassNode();
            classNode.visit(transformer.version, Opcodes.ACC_PUBLIC, ClassDefiner.generateClassName("IsolatedASMTransformer"), null, "java/lang/Object", null);

            { //<init>
                MethodVisitor init = classNode.visitMethod(Opcodes.ACC_PUBLIC, MN_Init, MD_Void, null, null);
                init.visitCode();
                init.visitVarInsn(Opcodes.ALOAD, 0);
                init.visitMethodInsn(Opcodes.INVOKESPECIAL, IN_Object, MN_Init, MD_Void, false);
                init.visitInsn(Opcodes.RETURN);
            }
            List<MethodNode> methodsToCopy = new ArrayList<>();
            methodsToCopy.add(transformerMethod);
            MethodVisitor methodVisitor = new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (owner.equals(transformer.name)) {
                        MethodNode method = ASMUtils.getMethod(transformer, name, desc);
                        if (method == null) throw new IllegalStateException("CASM transformer called method '" + name + "' not found");
                        methodsToCopy.add(method);
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    if (owner.equals(transformer.name)) throw new IllegalStateException("CASM transformer must not access fields in the transformer class");
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    if (bootstrapMethodHandle.getOwner().equals(internalName(LambdaMetafactory.class))) {
                        throw new IllegalStateException("CASM transformer can not access LambdaMetafactory");

                        //LambdaMetaFactory can not access the anonymous class, so we sadly can't use it here
//                        Handle handle = (Handle) bootstrapMethodArguments[1];
//
//                        if (!handle.getOwner().equals(transformer.name)) {
//                            throw new IllegalStateException("CASM transformer lambda target class '" + handle.getOwner() + "' must be the same as the transformer class");
//                        }
//                        MethodNode method = ASMUtils.getMethod(transformer, handle.getName(), handle.getDesc());
//                        if (method == null) throw new IllegalStateException("CASM transformer lambda target method '" + handle.getName() + "' not found");
//                        methodsToCopy.add(method);
                    }
                }
            };
            while (!methodsToCopy.isEmpty()) {
                List<MethodNode> methods = new ArrayList<>(methodsToCopy);
                methodsToCopy.clear();
                for (MethodNode methodNode : methods) {
                    Remapper.remapAndAdd(transformer, classNode, methodNode);
                    methodNode.accept(methodVisitor);
                }
            }

            return ClassDefiner.defineAnonymousClass(ASMUtils.toBytes(classNode, classProvider));
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to isolate method '" + transformerMethod.name + "' of transformer '" + transformer.name + "'", t);
        }
    }

}
