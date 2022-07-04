package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CWrapCatch;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.types.ARemovingTargetTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.Map;

public class CWrapCatchTransformer extends ARemovingTargetTransformer<CWrapCatch> {

    public CWrapCatchTransformer() {
        super(CWrapCatch.class, CWrapCatch::value);
    }

    @Override
    public void transform(CWrapCatch annotation, TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            boolean isStatic = Modifier.isStatic(target.access);
            throw new TransformerException(transformerMethod, transformer, "must " + (isStatic ? "" : "not ") + "be static")
                    .help(Codifier.of(transformerMethod).access(isStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
        }
        Type[] args = Type.getArgumentTypes(transformerMethod.desc);
        Type returnType = Type.getReturnType(transformerMethod.desc);
        if (args.length != 1) {
            throw new TransformerException(transformerMethod, transformer, "must have one argument (Exception to catch)")
                    .help(Codifier.of(transformerMethod).param(null).param(Type.getType(Exception.class)));
        }
        if (!ASMUtils.compareType(Type.getReturnType(target.desc), returnType)) {
            throw new TransformerException(transformerMethod, transformer, "must have the same return type as the target method")
                    .help(Codifier.of(transformerMethod).returnType(Type.getReturnType(target.desc)));
        }
        boolean cast = !Type.getReturnType(target.desc).equals(returnType);
        Type exceptionType = args[0];
        this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CWrapCatch");

        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        target.instructions.insertBefore(target.instructions.getFirst(), start);
        target.instructions.add(end);
        target.instructions.add(handler);
        if (Modifier.isStatic(target.access)) {
            target.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access)));
        } else {
            target.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            target.instructions.add(new InsnNode(Opcodes.SWAP));
            target.instructions.add(new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc));
        }
        if (cast) target.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getReturnType(target.desc).getInternalName()));
        target.instructions.add(new InsnNode(ASMUtils.getReturnOpcode(returnType)));

        target.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, exceptionType.getInternalName()));
    }

}
