package net.lenni0451.classtransform.targets;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.junit.jupiter.api.BeforeEach;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class ATargetTest {

    protected final IClassProvider classProvider = new BasicClassProvider();
    protected Map<String, IInjectionTarget> injectionTargets;
    protected MethodNode method;
    protected CSlice emptySlice;

    @BeforeEach
    public void setUp() {
        try {
            Field f = TransformerManager.class.getDeclaredField("injectionTargets");
            f.setAccessible(true);
            this.injectionTargets = (Map<String, IInjectionTarget>) f.get(new TransformerManager(this.classProvider));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to get injection targets", t);
        }
        this.method = new MethodNode(0, "test", "()V", null, null);
        this.emptySlice = AnnotationParser.parse(CSlice.class, this.classProvider, new HashMap<>());

        this.method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "Test", "static", "I"));
        this.method.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, "Test", "static", "I"));
        this.method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "Test", "virtual", "Z"));
        this.method.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "Test", "virtual", "Z"));
        this.method.instructions.add(new InsnNode(Opcodes.IRETURN));
        this.method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "Test", "invokeInterface", "()V"));
        this.method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "Test", "invokeVirtual", "(Ljava/lang/String;)Z"));
        this.method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Test", "invokeSpecial", "()Ljava/io/FileInputStream;"));
        this.method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "Test", "invokeStatic", "(Ljava/lang/Integer;)I"));
        this.method.instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getType(String.class).getInternalName()));
        this.method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        this.method.instructions.add(new InsnNode(Opcodes.ATHROW));
        this.method.instructions.add(new InsnNode(Opcodes.DRETURN));
    }

    protected CTarget getTarget(final String target, final CTarget.Shift shift, final int ordinal) {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "value");
        map.put("target", target);
        map.put("shift", shift);
        map.put("ordinal", ordinal);
        return AnnotationParser.parse(CTarget.class, this.classProvider, map);
    }

}
