package net.lenni0451.classtransform.utils.tree;

import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

public class ClassTree {

    private static final Map<String, ClassTree> TREE = new HashMap<>();

    public static ClassTree getTreePart(final IClassProvider classProvider, String className) {
        className = className.replace("/", ".");
        if (TREE.containsKey(className)) return TREE.get(className);

        byte[] bytecode = classProvider.getClass(className);
        ClassNode node = ASMUtils.fromBytes(bytecode);
        ClassTree tree = new ClassTree(node);
        TREE.put(className, tree);

        int oldSize;
        do {
            oldSize = tree.superClasses.size();
            for (String superClass : tree.superClasses.toArray(new String[0])) {
                ClassTree superTree = getTreePart(classProvider, superClass);
                if (superTree != null) tree.superClasses.addAll(superTree.superClasses);
            }
        } while (oldSize != tree.superClasses.size());

        return tree;
    }


    private final String name;
    private final String superClass;
    private final Set<String> superClasses;
    private final int modifiers;

    public ClassTree(final ClassNode classNode) {
        this.name = classNode.name.replace("/", ".");
        this.superClass = classNode.superName;
        this.superClasses = new HashSet<>();
        if (this.superClass != null) this.superClasses.add(this.superClass.replace("/", "."));
        if (classNode.interfaces != null) {
            for (String inter : classNode.interfaces) this.superClasses.add(inter.replace("/", "."));
        }
        this.modifiers = classNode.access;
    }

    public String getName() {
        return this.name;
    }

    public ClassTree parseSuperClass(final IClassProvider classProvider) {
        if (this.superClass == null) return null;
        return ClassTree.getTreePart(classProvider, this.superClass);
    }

    public Set<String> getSuperClasses() {
        return Collections.unmodifiableSet(this.superClasses);
    }

    public int getModifiers() {
        return this.modifiers;
    }

}
