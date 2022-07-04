package net.lenni0451.classtransform.utils;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class MemberDeclaration {

    private final String owner;
    private final String name;
    private final String desc;

    public MemberDeclaration(final String owner, final String name, final String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public String getOwner() {
        return this.owner;
    }

    public String getName() {
        return this.name;
    }

    public String getDesc() {
        return this.desc;
    }


    public boolean is(final FieldInsnNode fieldInsnNode) {
        return this.owner.equals(fieldInsnNode.owner) && this.name.equals(fieldInsnNode.name) && this.desc.equals(fieldInsnNode.desc);
    }

    public boolean is(final MethodInsnNode methodInsnNode) {
        return this.owner.equals(methodInsnNode.owner) && this.name.equals(methodInsnNode.name) && this.desc.equals(methodInsnNode.desc);
    }

    public boolean isFieldMapping() {
        return !this.desc.startsWith("(");
    }

}
