package net.lenni0451.classtransform.utils.attributes;

import lombok.SneakyThrows;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SharedVariableAttribute extends Attribute {

    public static final String NAME = "ClassTransformSharedVariable";

    private final Map<String, SharedVariable> variables = new HashMap<>();

    public SharedVariableAttribute() {
        super(NAME);
    }

    public SharedVariableAttribute(final Attribute attribute) throws IOException {
        this();
        Serializer.read(AttributeAccessor.getContent(attribute), this);
    }

    public SharedVariable addVariable(final String name, final int index, final Type type) {
        SharedVariable sharedVariable = new SharedVariable(name, index, type);
        this.variables.put(name, sharedVariable);
        return sharedVariable;
    }

    @Nullable
    public SharedVariable getVariableIndex(final String name) {
        return this.variables.get(name);
    }

    @Override
    protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
        return AttributeAccessor.newByteVector(Serializer.write(this));
    }


    public static class SharedVariable {
        private final String name;
        private final int variableIndex;
        private final Type type;

        private SharedVariable(final String name, final int variableIndex, final Type type) {
            this.name = name;
            this.variableIndex = variableIndex;
            this.type = type;
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
    }

    private static class Serializer {
        @SneakyThrows
        public static byte[] write(final SharedVariableAttribute attribute) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutput dos = new DataOutputStream(baos);
            dos.writeShort(attribute.variables.size());
            for (SharedVariable sharedVariable : attribute.variables.values()) {
                dos.writeUTF(sharedVariable.getName());
                dos.writeInt(sharedVariable.getVariableIndex());
                dos.writeUTF(sharedVariable.getType().getInternalName());
            }
            return baos.toByteArray();
        }

        public static SharedVariableAttribute read(final byte[] data, final SharedVariableAttribute attribute) throws IOException {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int size = dis.readShort();
            for (int i = 0; i < size; i++) {
                String name = dis.readUTF();
                int variableIndex = dis.readInt();
                Type type = Type.getObjectType(dis.readUTF());
                attribute.addVariable(name, variableIndex, type);
            }
            return attribute;
        }
    }

}
