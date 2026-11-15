package net.lenni0451.classtransform.utils.tree;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;

/**
 * A class tree which dynamically loads tree parts on demand.
 *
 * <p>Parts are cached by dot-separated class name for the lifetime of the tree. When a {@link TransformerManager} is
 * set, bytecode is transformed before it is parsed, so the tree mirrors the classes after transformation. Super
 * classes and interfaces are resolved transitively while a part is loaded. The part is cached before its super types
 * are resolved, so a missing type propagates its {@link ClassNotFoundException} to the caller while the partially
 * resolved part stays cached and is returned by later calls.
 *
 * <p>{@link #getTreePart(IClassProvider, String)} is synchronized, but the cached class nodes are shared mutable
 * objects and must not be modified by several threads at once.
 */
@ParametersAreNonnullByDefault
public class ClassTree {

    private final Map<String, TreePart> tree = new HashMap<>();
    private final TransformerManager transformerManager;

    /**
     * Creates a class tree that does not transform classes.
     */
    public ClassTree() {
        this(null);
    }

    /**
     * Creates a class tree that optionally transforms classes before parsing them.
     *
     * @param transformerManager The manager used to transform class bytecode, or {@code null} to disable transforming
     */
    public ClassTree(@Nullable final TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }

    /**
     * Checks whether this tree was created with a transformer manager.
     *
     * @return If this class tree can transform classes
     */
    public boolean canTransform() {
        return this.transformerManager != null;
    }

    /**
     * Get a tree part from a class by name.
     *
     * <p>The part is loaded and cached on first access, so later calls with the same name return the same instance
     * regardless of the provider passed in.
     *
     * @param classProvider The class provider to get the bytecode from
     * @param className     The name of the class
     * @return The tree part
     * @throws ClassNotFoundException If the class could not be found
     */
    @Nonnull
    public synchronized TreePart getTreePart(final IClassProvider classProvider, String className) throws ClassNotFoundException {
        className = dot(className);
        if (this.tree.containsKey(className)) return this.tree.get(className);

        byte[] bytecode = classProvider.getClass(className);
        if (this.transformerManager != null) {
            byte[] transformed = this.transformerManager.transform(className, bytecode, false);
            if (transformed != null) bytecode = transformed;
        }
        ClassNode node = ASMUtils.fromBytes(bytecode);
        TreePart part = new TreePart(node);
        this.tree.put(className, part);

        int oldSize;
        do {
            oldSize = part.superClasses.size();
            for (String superClass : part.superClasses.toArray(new String[0])) {
                TreePart superTree = this.getTreePart(classProvider, superClass);
                part.superClasses.addAll(superTree.superClasses);
            }
        } while (oldSize != part.superClasses.size());

        return part;
    }


    /**
     * A single class in the tree with its resolved super types and access flags.
     *
     * <p>Instances are created and cached by {@link ClassTree#getTreePart(IClassProvider, String)}. Equality and hash
     * code are based on the dot-separated class name only, so parts loaded through different providers compare equal.
     */
    @ParametersAreNonnullByDefault
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
            this.superClasses = new LinkedHashSet<>();
            if (this.superClass != null) this.superClasses.add(dot(this.superClass));
            if (node.interfaces != null) {
                for (String inter : node.interfaces) this.superClasses.add(dot(inter));
            }
            this.modifiers = node.access;
        }

        /**
         * <b>The bytecode of the class may not valid since it is missing stack map frames. Use at your own risk.</b>
         *
         * @return The class node of this tree part
         */
        public ClassNode getNode() {
            return this.node;
        }

        /**
         * Gets the name of the class.
         *
         * @return The name of the class with dots instead of slashes
         */
        public String getName() {
            return this.name;
        }

        /**
         * Gets the super class of this class as a tree part.
         *
         * @param classProvider The class provider to get the bytecode from
         * @return The tree part of the super class, or {@code null} if this class has no super class
         * @throws ClassNotFoundException If the class could not be found
         */
        @Nullable
        public TreePart parseSuperClass(final IClassProvider classProvider) throws ClassNotFoundException {
            if (this.superClass == null) return null;
            return ClassTree.this.getTreePart(classProvider, this.superClass);
        }

        /**
         * Gets all super classes and their super classes, including interfaces.
         *
         * <p>The returned set is an unmodifiable view of the cached names and does not contain this class itself.
         *
         * @return A set of all super classes and their super classes including interfaces
         */
        public Set<String> getSuperClasses() {
            return Collections.unmodifiableSet(this.superClasses);
        }

        /**
         * Get the class tree parts of all super classes of this class.<br>
         * This includes the super class and all interfaces.
         *
         * <p>The returned set is newly created and mutable, so callers may modify it without affecting the tree.
         *
         * @param classProvider The class provider to get the bytecode from
         * @param includeSelf   Add the current class to the set
         * @return A set of all super classes and their super classes including interfaces
         * @throws ClassNotFoundException If any of the classes could not be found
         */
        public Set<TreePart> getParsedSuperClasses(final IClassProvider classProvider, final boolean includeSelf) throws ClassNotFoundException {
            Set<TreePart> out = new LinkedHashSet<>();
            if (includeSelf) out.add(this);
            for (String superClass : this.superClasses) out.add(ClassTree.this.getTreePart(classProvider, superClass));
            return out;
        }

        /**
         * Gets the ASM access flags of the class.
         *
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
            return Objects.equals(this.name, treePart.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name);
        }

    }

}
