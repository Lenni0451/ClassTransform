package net.lenni0451.classtransform.utils.tree;

import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;

/**
 * A class tree which dynamically loads tree parts on demand.
 */
public class ClassTree {

    private final Map<String, TreePart> TREE = new HashMap<>();

    /**
     * Get a tree part from a class by name.
     *
     * @param classProvider The class provider to get the bytecode from
     * @param className     The name of the class
     * @return The tree part
     */
    public TreePart getTreePart(final IClassProvider classProvider, String className) {
        className = dot(className);
        if (TREE.containsKey(className)) return TREE.get(className);

        byte[] bytecode = classProvider.getClass(className);
        ClassNode node = ASMUtils.fromBytes(bytecode);
        TreePart tree = new TreePart(node);
        TREE.put(className, tree);

        int oldSize;
        do {
            oldSize = tree.superClasses.size();
            for (String superClass : tree.superClasses.toArray(new String[0])) {
                TreePart superTree = getTreePart(classProvider, superClass);
                if (superTree != null) tree.superClasses.addAll(superTree.superClasses);
            }
        } while (oldSize != tree.superClasses.size());

        return tree;
    }


    public class TreePart {

        private final ClassNode node;
        private final String name;
        private final String superClass;
        private final Set<String> superClasses;
        private final int modifiers;

        private TreePart(final ClassNode node) {
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

        /**
         * @return The class node of this tree part
         */
        public ClassNode getNode() {
            return this.node;
        }

        /**
         * @return The name of the class
         */
        public String getName() {
            return this.name;
        }

        /**
         * Get the class node of the super class of this class.
         *
         * @param classProvider The class provider to get the bytecode from
         * @return The class node of the super class
         */
        public TreePart parseSuperClass(final IClassProvider classProvider) {
            if (this.superClass == null) return null;
            return ClassTree.this.getTreePart(classProvider, this.superClass);
        }

        /**
         * @return A set of all super classes and their super classes including interfaces
         */
        public Set<String> getSuperClasses() {
            return Collections.unmodifiableSet(this.superClasses);
        }

        /**
         * Get the class tree parts of all super classes of this class.<br>
         * This includes the super class and all interfaces.
         *
         * @param classProvider The class provider to get the bytecode from
         * @param includeSelf   Add the current class to the set
         * @return A set of all super classes and their super classes including interfaces
         */
        public Set<TreePart> getParsedSuperClasses(final IClassProvider classProvider, final boolean includeSelf) {
            Set<TreePart> out = new HashSet<>();
            if (includeSelf) out.add(this);
            for (String superClass : this.superClasses) out.add(ClassTree.this.getTreePart(classProvider, superClass));
            return out;
        }

        /**
         * @return The modifiers of the class
         */
        public int getModifiers() {
            return this.modifiers;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreePart treePart = (TreePart) o;
            return Objects.equals(name, treePart.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

    }

}
