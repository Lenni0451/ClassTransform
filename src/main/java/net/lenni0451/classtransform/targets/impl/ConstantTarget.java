package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Function;

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
 * The constant type is case-insensitive.
 */
public class ConstantTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        List<AbstractInsnNode> instructions = this.getSlice(injectionTargets, method, slice);

        if (target.target().equalsIgnoreCase("null")) this.findNull(targets, instructions);
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
            if (number instanceof Float && number.floatValue() == val) targets.add(instruction);
        }
    }

    private void findDouble(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        double val = this.parse(value, "double", len -> len == 2, Double::parseDouble);

        for (AbstractInsnNode instruction : instructions) {
            Number number = ASMUtils.getNumber(instruction);
            if (number instanceof Double && number.doubleValue() == val) targets.add(instruction);
        }
    }

    private void findString(final List<AbstractInsnNode> targets, final List<AbstractInsnNode> instructions, final String value) {
        String val = this.parse(value, "String", len -> len >= 2, s -> s);

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
