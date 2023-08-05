package net.lenni0451.classtransform.utils.parser;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Parse a string to an insn list.
 */
@ParametersAreNonnullByDefault
public class StringParser {

    static final Map<String, Integer> OPCODES = new HashMap<>();

    static {
        try {
            for (Field field : Opcodes.class.getDeclaredFields()) {
                if (!Modifier.isPublic(field.getModifiers())) continue;
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if (!field.getType().equals(int.class)) continue;

                OPCODES.put(field.getName(), field.getInt(null));
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to get all opcodes", t);
        }
    }

    /**
     * Parse a string to an insn list.<br>
     * Every line represents an instruction.
     *
     * @param s The string to parse
     * @return The insn list
     */
    public static InsnList parse(final String s) {
        return parse(s.split("\n"));
    }

    /**
     * Parse a string to an insn list.
     *
     * @param s The string to parse
     * @return The insn list
     */
    public static InsnList parse(final String... s) {
        InsnList list = new InsnList();
        Map<String, LabelNode> labels = new HashMap<>();

        for (String line : s) {
            StringReader reader = new StringReader(line);
            if (reader.peekString().toLowerCase(Locale.ROOT).startsWith("label")) {
                reader.readString();
                LabelNode label = new LabelNode();
                list.add(label);
                labels.put(reader.readString(), label);
            } else {
                int opcode = reader.readOpcode();

                list.add(read(labels, opcode, reader));
                if (reader.canRead()) throw new IllegalStateException("Line '" + line + "' has extra data '" + reader.readAll() + "'");
            }
        }

        return list;
    }

    private static AbstractInsnNode read(final Map<String, LabelNode> labels, final int opcode, final StringReader reader) {
        switch (opcode) {
            default:
                return new InsnNode(opcode);

            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
            case Opcodes.NEWARRAY:
                return new IntInsnNode(opcode, reader.readInt());

            case Opcodes.LDC:
                return new LdcInsnNode(reader.readConstantPoolEntry());

            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
            case Opcodes.RET:
                return new VarInsnNode(opcode, reader.readInt());

            case Opcodes.IINC:
                return new IincInsnNode(opcode, reader.readInt());

            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.GOTO:
            case Opcodes.JSR:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                return new JumpInsnNode(opcode, labels.get(reader.readString()));

            case Opcodes.TABLESWITCH:
                int min = reader.readInt();
                int max = reader.readInt();
                LabelNode dflt = labels.get(reader.readString());
                List<LabelNode> switchLabels = new ArrayList<>();
                while (reader.canRead()) switchLabels.add(labels.get(reader.readString()));
                return new TableSwitchInsnNode(min, max, dflt, switchLabels.toArray(new LabelNode[0]));

            case Opcodes.LOOKUPSWITCH:
                dflt = labels.get(reader.readString());
                List<Integer> switchKeys = new ArrayList<>();
                switchLabels = new ArrayList<>();
                while (reader.canReadInt(false)) switchKeys.add(reader.readInt());
                while (reader.canRead()) switchLabels.add(labels.get(reader.readString()));
                if (switchKeys.size() != switchLabels.size()) throw new IllegalStateException("Switch keys and labels must be the same size");
                return new LookupSwitchInsnNode(dflt, switchKeys.stream().mapToInt(i -> i).toArray(), switchLabels.toArray(new LabelNode[0]));

            case Opcodes.GETSTATIC:
            case Opcodes.PUTSTATIC:
            case Opcodes.GETFIELD:
            case Opcodes.PUTFIELD:
                return new FieldInsnNode(opcode, reader.readString(), reader.readString(), reader.readString());

            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEINTERFACE:
                String owner = reader.readString();
                String name = reader.readString();
                String descriptor = reader.readString();
                if (reader.canRead()) return new MethodInsnNode(opcode, owner, name, descriptor, reader.readBoolean());
                else return new MethodInsnNode(opcode, owner, name, descriptor);

            case Opcodes.INVOKEDYNAMIC:
                name = reader.readString();
                descriptor = reader.readString();
                Handle bootstrapMethodHandle = reader.readHandle();
                List<Object> bootstrapMethodArguments = new ArrayList<>();
                while (reader.canRead()) bootstrapMethodArguments.add(reader.readConstantPoolEntry());
                return new InvokeDynamicInsnNode(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments.toArray(new Object[0]));

            case Opcodes.NEW:
            case Opcodes.ANEWARRAY:
            case Opcodes.CHECKCAST:
            case Opcodes.INSTANCEOF:
                return new TypeInsnNode(opcode, reader.readString());

            case Opcodes.MULTIANEWARRAY:
                return new MultiANewArrayInsnNode(reader.readString(), reader.readInt());
        }
    }

}
