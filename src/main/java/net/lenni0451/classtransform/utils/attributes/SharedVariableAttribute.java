package net.lenni0451.classtransform.utils.attributes;

import lombok.SneakyThrows;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Method attribute storing the shared variables declared through {@code CShared}.
 * <p>
 * The attribute is written under {@value #NAME} as a variable count followed by, for each variable, the
 * owning transformer, name, namespace, local variable index, type descriptor and the global/reference
 * flags. A non-empty namespace makes the variable visible to every transformer using that namespace,
 * while an empty namespace either shares the variable globally or binds it to the owning transformer.
 * The variable list is the live backing list, and lookups return {@code null} when nothing matches.
 */
public class SharedVariableAttribute extends Attribute {

    /**
     * The attribute name under which the shared variables are written to the class file.
     */
    public static final String NAME = "ClassTransformSharedVariable";

    private final List<SharedVariable> variables = new ArrayList<>();

    /**
     * Create an empty attribute.
     */
    public SharedVariableAttribute() {
        super(NAME);
    }

    /**
     * Create an attribute by reading the raw payload of an existing attribute of this type.
     *
     * @param attribute The attribute to parse
     * @throws IOException If the payload is malformed
     */
    public SharedVariableAttribute(final Attribute attribute) throws IOException {
        this();
        Serializer.read(AttributeAccessor.getContent(attribute), this);
    }

    /**
     * Get the live list of shared variables of this attribute.
     *
     * @return All shared variables of this attribute
     */
    public List<SharedVariable> getVariables() {
        return this.variables;
    }

    /**
     * Add a new shared variable to this attribute.
     *
     * @param transformer The name of the transformer owning the variable
     * @param name        The name of the variable
     * @param namespace   The namespace of the variable, empty if the variable is not namespaced
     * @param index       The local variable index of the value
     * @param type        The type of the variable
     * @param global      If the variable is shared between all transformers
     * @param reference   If the variable holds a reference whose changes are visible immediately
     * @return The created shared variable
     */
    public SharedVariable addVariable(final String transformer, final String name, final String namespace, final int index, final Type type, final boolean global, final boolean reference) {
        SharedVariable sharedVariable = new SharedVariable(transformer, name, namespace, index, type, global, reference);
        this.variables.add(sharedVariable);
        return sharedVariable;
    }

    /**
     * Add a shared variable without namespace and reference support.
     *
     * @param transformer The name of the transformer owning the variable
     * @param name        The name of the variable
     * @param index       The local variable index of the value
     * @param type        The type of the variable
     * @param global      If the variable is shared between all transformers
     * @return The created shared variable
     * @deprecated Use {@link #addVariable(String, String, String, int, Type, boolean, boolean)} instead.
     */
    @Deprecated
    public SharedVariable addVariable(final String transformer, final String name, final int index, final Type type, final boolean global) {
        return this.addVariable(transformer, name, "", index, type, global, false);
    }

    /**
     * Find a shared variable matching the given scope.
     * <p>
     * A non-empty namespace matches by name and namespace, ignoring the {@code global} flag and the
     * owning transformer, so any variable in that namespace is visible. An empty namespace with
     * {@code global} set matches un-namespaced variables shared between all transformers, and otherwise
     * only un-namespaced variables owned by the given transformer are considered.
     *
     * @param transformer The name of the requesting transformer
     * @param name        The name of the variable
     * @param namespace   The namespace to look up, empty for a scoped lookup
     * @param global      If a globally shared variable should be looked up
     * @return The matching variable, or {@code null} if none matches
     */
    @Nullable
    public SharedVariable getVariable(final String transformer, final String name, final String namespace, final boolean global) {
        for (SharedVariable variable : this.variables) {
            if (!variable.name.equals(name)) continue;
            if (!namespace.isEmpty()) {
                if (variable.namespace.equals(namespace)) return variable;
            } else if (global) {
                if (variable.namespace.isEmpty() && variable.global) return variable;
            } else if (variable.namespace.isEmpty() && !variable.global && variable.transformer.equals(transformer)) {
                return variable;
            }
        }
        return null;
    }

    /**
     * Find a shared variable by transformer and name without a namespace.
     * <p>
     * Delegates with an empty namespace, so only un-namespaced variables are considered and namespaced
     * variables are never returned.
     *
     * @param transformer The name of the requesting transformer
     * @param name        The name of the variable
     * @param global      If a globally shared variable should be looked up
     * @return The matching variable, or {@code null} if none matches
     * @deprecated Use {@link #getVariable(String, String, String, boolean)} instead.
     */
    @Deprecated
    @Nullable
    public SharedVariable getVariableIndex(final String transformer, final String name, final boolean global) {
        return this.getVariable(transformer, name, "", global);
    }

    @Override
    protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
        return AttributeAccessor.newByteVector(Serializer.write(this));
    }


    /**
     * A single shared variable together with the instructions that initialize it.
     */
    public static class SharedVariable {
        private final String transformer;
        private final String name;
        private final String namespace;
        private final int variableIndex;
        private final Type type;
        private final boolean global;
        private final boolean reference;
        private final List<AbstractInsnNode> initialization = new ArrayList<>();

        private SharedVariable(final String transformer, final String name, final String namespace, final int variableIndex, final Type type, final boolean global, final boolean reference) {
            this.transformer = transformer;
            this.name = name;
            this.namespace = namespace;
            this.variableIndex = variableIndex;
            this.type = type;
            this.global = global;
            this.reference = reference;
        }

        /**
         * Identifies the transformer which declared the variable.
         *
         * @return The name of the owning transformer
         */
        public String getTransformer() {
            return this.transformer;
        }

        /**
         * Specifies the name of the variable within its transformer.
         *
         * @return The name of the variable
         */
        public String getName() {
            return this.name;
        }

        /**
         * Defines the namespace which scopes the variable, empty if the variable is not namespaced.
         *
         * @return The namespace of the variable
         */
        public String getNamespace() {
            return this.namespace;
        }

        /**
         * Indicates the local variable slot which holds the value.
         *
         * @return The local variable index of the value
         */
        public int getVariableIndex() {
            return this.variableIndex;
        }

        /**
         * Describes the type of the stored value.
         *
         * @return The type of the variable
         */
        public Type getType() {
            return this.type;
        }

        /**
         * Tells whether the variable is shared between all transformers instead of being bound to its owner.
         *
         * @return If the variable is shared between all transformers
         */
        public boolean isGlobal() {
            return this.global;
        }

        /**
         * Tells whether the variable holds a reference whose changes are visible immediately.
         *
         * @return If the variable holds a reference
         */
        public boolean isReference() {
            return this.reference;
        }

        /**
         * Record the instructions which initialize this variable.<br>
         * They are removed again when the containing method body is moved during method wrapping.
         *
         * @param instructions The initialization instructions
         */
        public void addInitialization(final AbstractInsnNode[] instructions) {
            for (AbstractInsnNode instruction : instructions) this.initialization.add(instruction);
        }

        /**
         * Get the live list of instructions which initialize this variable.
         *
         * @return The instructions which initialize this variable
         */
        public List<AbstractInsnNode> getInitialization() {
            return this.initialization;
        }
    }

    private static class Serializer {
        @SneakyThrows
        public static byte[] write(final SharedVariableAttribute attribute) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutput dos = new DataOutputStream(baos);
            dos.writeInt(attribute.variables.size());
            for (SharedVariable sharedVariable : attribute.variables) {
                dos.writeUTF(sharedVariable.getTransformer());
                dos.writeUTF(sharedVariable.getName());
                dos.writeUTF(sharedVariable.getNamespace());
                dos.writeInt(sharedVariable.getVariableIndex());
                dos.writeUTF(sharedVariable.getType().getDescriptor());
                dos.writeBoolean(sharedVariable.isGlobal());
                dos.writeBoolean(sharedVariable.isReference());
            }
            return baos.toByteArray();
        }

        public static SharedVariableAttribute read(final byte[] data, final SharedVariableAttribute attribute) throws IOException {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                String transformer = dis.readUTF();
                String name = dis.readUTF();
                String namespace = dis.readUTF();
                int variableIndex = dis.readInt();
                Type type = Type.getType(dis.readUTF());
                boolean global = dis.readBoolean();
                boolean reference = dis.readBoolean();
                attribute.addVariable(transformer, name, namespace, variableIndex, type, global, reference);
            }
            return attribute;
        }
    }

}
