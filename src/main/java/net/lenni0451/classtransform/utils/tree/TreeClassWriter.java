package net.lenni0451.classtransform.utils.tree;

import lombok.SneakyThrows;
import org.objectweb.asm.ClassWriter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;
import static net.lenni0451.classtransform.utils.Types.IN_Object;

/**
 * A class writer implementation using the {@link ClassTree} for stack frame calculation.
 *
 * <p>Common super classes are resolved through {@link ClassTree#getTreePart(IClassProvider, String)} instead of a
 * class loader, so classes that only exist in the class provider can still be written. Unresolvable types and
 * interfaces without a common super class fall back to {@code java/lang/Object}. A type missing from the provider
 * makes the calculation fail with a {@link ClassNotFoundException} that propagates as an undeclared checked
 * exception.
 */
@ParametersAreNonnullByDefault
public class TreeClassWriter extends ClassWriter {

    private final ClassTree classTree;
    private final IClassProvider classProvider;

    /**
     * Creates a writer that computes stack map frames.
     *
     * @param classTree     The tree used to resolve common super classes
     * @param classProvider The provider used to load classes for the tree
     */
    public TreeClassWriter(final ClassTree classTree, final IClassProvider classProvider) {
        this(ClassWriter.COMPUTE_FRAMES, classTree, classProvider);
    }

    /**
     * Creates a writer with the given ASM flags.
     *
     * @param flags         The flags passed to {@link ClassWriter}
     * @param classTree     The tree used to resolve common super classes
     * @param classProvider The provider used to load classes for the tree
     */
    public TreeClassWriter(final int flags, final ClassTree classTree, final IClassProvider classProvider) {
        super(flags);

        this.classTree = classTree;
        this.classProvider = classProvider;
    }

    @Override
    @SneakyThrows
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(IN_Object) || type2.equals(IN_Object)) return IN_Object;

        ClassTree.TreePart class1 = this.classTree.getTreePart(this.classProvider, type1);
        ClassTree.TreePart class2 = this.classTree.getTreePart(this.classProvider, type2);

        if (class2.getSuperClasses().contains(class1.getName())) {
            return type1;
        } else if (class1.getSuperClasses().contains(class2.getName())) {
            return type2;
        } else if (!Modifier.isInterface(class1.getModifiers()) && !Modifier.isInterface(class2.getModifiers())) {
            do {
                class1 = class1.parseSuperClass(this.classProvider);
                if (class1 == null) return IN_Object;
            } while (!class2.getSuperClasses().contains(class1.getName()));

            return slash(class1.getName());
        } else {
            return IN_Object;
        }
    }

}
