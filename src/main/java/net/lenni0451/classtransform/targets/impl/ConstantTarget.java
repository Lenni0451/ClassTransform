package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.log.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Function;
import java.lang.reflect.Method;

/**
 * A target for constants in the bytecode.<br>
 * Valid constants:<br>
 * - null<br>
 * - int<br>
 * - long<br>
 * - float<br>
 * - double<br>
 * - {@link String}<br>
 * - {@link Class}<br>
 * <br>
 * The constant is defined in {@link CTarget#target()}.<br>
 * e.g. {@code "int 1"} or {@code "String Hello World"}<br>
 * The constant type is case-insensitive.<br>
 * {@code "string "} matches the empty string and {@code "null false"} matches no constants.
 * <p>
 * The {@code expandZeroConditions=...} argument matches zero-comparison branches instead of constants.
 * Supported conditions are {@code LESS_THAN_ZERO}, {@code GREATER_THAN_OR_EQUAL_TO_ZERO},
 * {@code LESS_THAN_OR_EQUAL_TO_ZERO} and {@code GREATER_THAN_ZERO}. For {@code int 0}, explicit
 * zero constants are matched as well unless {@code implicitZero=true} is given, and {@code log=true}
 * prints the matches. The ordinal is applied to the matches in bytecode order; an out-of-range
 * ordinal yields an empty list.
 */
@ParametersAreNonnullByDefault
public class ConstantTarget implements IInjectionTarget {

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        String value = (String) values.get("target");
        if (value != null && value.toLowerCase(Locale.ROOT).startsWith("type ")) {
            values.put("target", "type " + mapper.getRemapper().mapDesc(value.substring(5)));
        }
        return null;
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        List<AbstractInsnNode> instructions = this.getSlice(injectionTargets, method, slice);
        boolean log = this.hasArgument(target.args(), "log=true");
        Set<Integer> expandOpcodes = this.getExpandOpcodes(target.args());
        if (log) Logger.info("BeforeConstant is searching for constants in method {}{}", method.name, method.desc);

        if (!expandOpcodes.isEmpty()) {
            boolean matchZero = target.target().equalsIgnoreCase("int 0") && !this.hasArgument(target.args(), "implicitZero=true");
            this.findExpanded(targets, instructions, matchZero, expandOpcodes, log);
        } else if (target.target().equalsIgnoreCase("null false")) return targets;
        else if (target.target().equalsIgnoreCase("null")) this.findNull(targets, instructions);
        else if (target.target().toLowerCase(Locale.ROOT).startsWith("int")) this.findInt(targets, instructions, target.target());
        else if (target.target().toLowerCase(Locale.ROOT).startsWith("long")) this.findLong(targets, instructions, target.target());
        else if (target.target().toLowerCase(Locale.ROOT).startsWith("float")) this.findFloat(targets, instructions, target.target());
        else if (target.target().toLowerCase(Locale.ROOT).startsWith("double")) this.findDouble(targets, instructions, target.target());
        else if (target.target().toLowerCase(Locale.ROOT).startsWith("string")) this.findString(targets, instructions, target.target());
        else if (target.target().toLowerCase(Locale.ROOT).startsWith("type")) this.findType(targets, instructions, target.target());
        else throw new IllegalArgumentException("Unknown constant type '" + target.target() + "'");

        if (target.ordinal() != -1) {
            if (target.ordinal() < 0 || target.ordinal() >= targets.size()) return Collections.emptyList();
            return Collections.singletonList(targets.get(target.ordinal()));
        }
        return targets;
    }

    private void findExpanded(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final boolean matchZero, final Set<Integer> expandOpcodes, final boolean log) {
        int last = -1;
        int ordinal = 0;
        for (AbstractInsnNode instruction : instructions) {
            int opcode = instruction.getOpcode();
            boolean match = expandOpcodes.contains(opcode);
            if (match) {
                if (last == Opcodes.LCMP || last == Opcodes.FCMPL || last == Opcodes.FCMPG || last == Opcodes.DCMPL || last == Opcodes.DCMPG) {
                    if (log) Logger.info("  BeforeConstant is ignoring {} following {}", opcode, last);
                    match = false;
                }
            } else if (matchZero) {
                Number number = ASMUtils.getNumber(instruction);
                match = (number instanceof Byte || number instanceof Short || number instanceof Integer) && number.intValue() == 0;
            }
            if (match) {
                if (log) Logger.info("  BeforeConstant found a matching constant at ordinal {}", ordinal);
                targets.add(instruction);
                ordinal++;
            }
            if (!(instruction instanceof LabelNode) && !(instruction instanceof FrameNode)) last = opcode;
        }
    }

    private Set<Integer> getExpandOpcodes(final String[] args) {
        Set<Integer> opcodes = new HashSet<>();
        for (String argument : args) {
            if (!argument.startsWith("expandZeroConditions=")) continue;
            for (String condition : argument.substring("expandZeroConditions=".length()).split(",")) {
                switch (condition.trim().toUpperCase(Locale.ROOT)) {
                    case "LESS_THAN_ZERO":
                    case "GREATER_THAN_OR_EQUAL_TO_ZERO":
                        opcodes.add(Opcodes.IFLT);
                        opcodes.add(Opcodes.IFGE);
                        break;
                    case "LESS_THAN_OR_EQUAL_TO_ZERO":
                    case "GREATER_THAN_ZERO":
                        opcodes.add(Opcodes.IFLE);
                        opcodes.add(Opcodes.IFGT);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown expand zero condition '" + condition + "'");
                }
            }
        }
        return opcodes;
    }

    private boolean hasArgument(final String[] args, final String argument) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(argument)) return true;
        }
        return false;
    }

    private void findNull(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions) {
        for (AbstractInsnNode instruction : instructions) {
            if (instruction.getOpcode() == Opcodes.ACONST_NULL) targets.add(instruction);
        }
    }

    private void findInt(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        int val = this.parse(value, "int", len -> len == 2, Integer::parseInt);

        for (AbstractInsnNode instruction : instructions) {
            Number number = ASMUtils.getNumber(instruction);
            if ((number instanceof Byte || number instanceof Short || number instanceof Integer) && number.intValue() == val) targets.add(instruction);
        }
    }

    private void findLong(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        long val = this.parse(value, "long", len -> len == 2, Long::parseLong);

        for (AbstractInsnNode instruction : instructions) {
            Number number = ASMUtils.getNumber(instruction);
            if (number instanceof Long && number.longValue() == val) targets.add(instruction);
        }
    }

    private void findFloat(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        float val = this.parse(value, "float", len -> len == 2, Float::parseFloat);

        for (AbstractInsnNode instruction : instructions) {
            Number number = ASMUtils.getNumber(instruction);
            if (number instanceof Float && Float.compare(number.floatValue(), val) == 0) targets.add(instruction);
        }
    }

    private void findDouble(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        double val = this.parse(value, "double", len -> len == 2, Double::parseDouble);

        for (AbstractInsnNode instruction : instructions) {
            Number number = ASMUtils.getNumber(instruction);
            if (number instanceof Double && Double.compare(number.doubleValue(), val) == 0) targets.add(instruction);
        }
    }

    private void findString(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        int separator = value.indexOf(' ');
        if (separator == -1) throw new IllegalArgumentException("String constant does not have a String as argument");
        String val = value.substring(separator + 1);

        for (AbstractInsnNode instruction : instructions) {
            if (instruction instanceof LdcInsnNode && val.equals(((LdcInsnNode) instruction).cst)) targets.add(instruction);
        }
    }

    private void findType(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        Type val = this.parse(value, "type", len -> len == 2, Type::getType);

        for (AbstractInsnNode instruction : instructions) {
            if (instruction instanceof LdcInsnNode && val.equals(((LdcInsnNode) instruction).cst)) targets.add(instruction);
        }
    }

    private <T> T parse(final String value, final String constantName, final Function<Integer, Boolean> sizeChecker, final Function<String, T> parser) {
        String[] parts = value.split(" ");
        if (!sizeChecker.apply(parts.length)) {
            throw new IllegalArgumentException(constantName + " constant does not have " + this.getAorAN(constantName) + " " + constantName + " as argument");
        }
        String unparsedVal = value.substring(parts[0].length() + 1);
        T val;
        try {
            val = parser.apply(unparsedVal);
        } catch (Throwable t) {
            throw new IllegalArgumentException(constantName + " constant value can not be parsed as " + this.getAorAN(constantName) + " " + constantName);
        }
        return val;
    }

    private String getAorAN(final String s) {
        char c = s.toUpperCase().charAt(0);
        if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U') return "an";
        return "a";
    }

}
