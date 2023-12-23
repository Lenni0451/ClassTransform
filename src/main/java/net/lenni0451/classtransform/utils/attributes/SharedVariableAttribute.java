package net.lenni0451.classtransform.utils.attributes;

import lombok.SneakyThrows;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SharedVariableAttribute extends Attribute {

    public static final String NAME = "ClassTransformSharedVariable";

    private final List<SharedVariable> variables = new ArrayList<>();

    public SharedVariableAttribute() {
        super(NAME);
    }

    public SharedVariableAttribute(final Attribute attribute) throws IOException {
        this();
        Serializer.read(AttributeAccessor.getContent(attribute), this);
    }

    public SharedVariable addVariable(final String transformer, final String name, final int index, final Type type, final boolean global) {
        SharedVariable sharedVariable = new SharedVariable(transformer, name, index, type, global);
        this.variables.add(sharedVariable);
        return sharedVariable;
    }

    @Nullable
    public SharedVariable getVariableIndex(final String transformer, final String name, final boolean global) {
        for (SharedVariable variable : this.variables) {
            if (variable.name.equals(name) && global == variable.global) {
                if (global) return variable;
                else if (variable.transformer.equals(transformer)) return variable;
            }
        }
        return null;
    }

    @Override
    protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
        return AttributeAccessor.newByteVector(Serializer.write(this));
    }


    public static class SharedVariable {
        private final String transformer;
        private final String name;
        private final int variableIndex;
        private final Type type;
        private final boolean global;

        private SharedVariable(final String transformer, final String name, final int variableIndex, final Type type, final boolean global) {
            this.transformer = transformer;
            this.name = name;
            this.variableIndex = variableIndex;
            this.type = type;
            this.global = global;
        }

        public String getTransformer() {
            return this.transformer;
        }

        public String getName() {
            return this.name;
        }

        public int getVariableIndex() {
            return this.variableIndex;
        }

        public Type getType() {
            return this.type;
        }

        public boolean isGlobal() {
            return this.global;
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
                dos.writeInt(sharedVariable.getVariableIndex());
                dos.writeUTF(sharedVariable.getType().getInternalName());
                dos.writeBoolean(sharedVariable.isGlobal());
            }
            return baos.toByteArray();
        }

        public static SharedVariableAttribute read(final byte[] data, final SharedVariableAttribute attribute) throws IOException {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                String transformer = dis.readUTF();
                String name = dis.readUTF();
                int variableIndex = dis.readInt();
                Type type = Type.getObjectType(dis.readUTF());
                boolean global = dis.readBoolean();
                attribute.addVariable(transformer, name, variableIndex, type, global);
            }
            return attribute;
        }
    }

}
