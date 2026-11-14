package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Detect array literals by finding the stores which initialize a created array.
 */
@ParametersAreNonnullByDefault
public class NewArrayPostProcessor {

    /**
     * Get the creation information of an array.
     *
     * @param node The array creation value
     * @return The creation information or null if the array is not initialized with constant indices
     */
    @Nullable
    public static ArrayCreationInfo getInfo(final ExpressionFlow.FlowValue node) {
        if (node.isComplex()) return null;
        int opcode = node.getInsn().getOpcode();
        if (opcode != Opcodes.NEWARRAY && opcode != Opcodes.ANEWARRAY) return null;
        if (node.inputCount() < 1) return null;
        Integer size = getIntConstant(node.getInput(0));
        if (size == null) return null;

        List<ExpressionFlow.FlowValue> next = new ArrayList<>();
        for (ExpressionFlow.FlowValue value : node.getNext()) {
            if (!value.isComplex()) next.add(value);
        }
        next.sort(Comparator.comparingInt(ExpressionFlow.FlowValue::getOrder));
        if (next.size() < size) return null;

        List<ExpressionFlow.FlowValue> stores = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ExpressionFlow.FlowValue store = next.get(i);
            if (!isStore(node, store, i)) return null;
            stores.add(store);
        }
        if (stores.isEmpty()) return null;
        return new ArrayCreationInfo(stores, stores.get(stores.size() - 1));
    }

    @Nullable
    private static Integer getIntConstant(final ExpressionFlow.FlowValue node) {
        if (node.isComplex()) return null;
        Object constant = ExpressionAst.getConstant(node.getInsn());
        return constant instanceof Integer ? (Integer) constant : null;
    }

    private static boolean isStore(final ExpressionFlow.FlowValue array, final ExpressionFlow.FlowValue store, final int index) {
        if (store.isComplex()) return false;
        int opcode = store.getInsn().getOpcode();
        if (opcode < Opcodes.IASTORE || opcode > Opcodes.SASTORE) return false;
        if (store.inputCount() != 3 || store.getInput(0) != array) return false;
        Integer storeIndex = getIntConstant(store.getInput(1));
        return storeIndex != null && storeIndex == index;
    }

}
