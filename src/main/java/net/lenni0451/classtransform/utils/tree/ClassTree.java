package net.lenni0451.classtransform.utils.tree;

import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;

public class ClassTree {

    private static final Map<String, ClassTree> TREE = new HashMap<>();

    public static ClassTree getTreePart(final IClassProvider classProvider, String className) {
        className = dot(className);
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


    private final ClassNode node;
    private final String name;
    private final String superClass;
    private final Set<String> superClasses;
    private final int modifiers;

    public ClassTree(final ClassNode node) {
        this.node = node;
        this.name = dot(node.name);
        this.superClass = node.superName;
        this.superClasses = new HashSet<>();
        if (this.superClass != null) this.superClasses.add(dot(this.superClass));
        if (node.interfaces != null) {
            for (String inter : node.interfaces) this.superClasses.add(dot(inter));
        }
        this.modifiers = node.access;
    }

    public ClassNode getNode() {
        return this.node;
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

    public Set<ClassTree> getParsedSuperClasses(final IClassProvider classProvider) {
        Set<ClassTree> out = new HashSet<>();
        for (String superClass : this.superClasses) out.add(ClassTree.getTreePart(classProvider, superClass));
        return out;
    }

    public Set<ClassTree> walkSuperClasses(final Set<ClassTree> walkedSuperClasses, final IClassProvider classProvider, final boolean includeSelf) {
        if (walkedSuperClasses.contains(this)) return walkedSuperClasses;
        if (includeSelf) walkedSuperClasses.add(this);
        for (ClassTree superClass : getParsedSuperClasses(classProvider)) superClass.walkSuperClasses(walkedSuperClasses, classProvider, true);
        return walkedSuperClasses;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassTree classTree = (ClassTree) o;
        return Objects.equals(name, classTree.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
