package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A small value-flow graph used to match expressions against bytecode.
 */
public class ExpressionFlow {

    /**
     * Analyze the value flow of a method.
     *
     * @param classNode The class containing the method
     * @param method    The method to analyze
     * @return All value nodes
     */
    public static Collection<FlowValue> analyze(final ClassNode classNode, final MethodNode method) {
        FlowInterpreter interpreter = new FlowInterpreter(classNode, method);
        try {
            new Analyzer<>(interpreter).analyze(classNode.name, method);
        } catch (AnalyzerException e) {
            throw new IllegalStateException("Failed to analyze value flow of " + classNode.name + "." + method.name + method.desc, e);
        }
        List<FlowValue> flows = new ArrayList<>(interpreter.getFlows());
        for (FlowValue flow : flows) {
            if (flow.isComplex()) continue;
            flow.setOrder(method.instructions.indexOf(flow.getInsn()));
        }
        for (FlowValue flow : flows) {
            for (int i = 0; i < flow.inputCount(); i++) flow.getInput(i).addNext(flow);
        }
        return flows;
    }


    public static class FlowValue implements Value {
        private final Type type;
        private final AbstractInsnNode insn;
        private final FlowValue[] inputs;
        private final List<FlowValue> next = new ArrayList<>();
        private int order = -1;

        public FlowValue(final Type type, final AbstractInsnNode insn, final FlowValue... inputs) {
            this.type = type;
            this.insn = insn;
            this.inputs = inputs;
        }

        @Override
        public int getSize() {
            return this.type.getSize();
        }

        public Type getType() {
            return this.type;
        }

        public AbstractInsnNode getInsn() {
            return this.insn;
        }

        public FlowValue getInput(final int index) {
            return this.inputs[index];
        }

        public int inputCount() {
            return this.inputs.length;
        }

        /**
         * @return The values which use this value as an input
         */
        public List<FlowValue> getNext() {
            return this.next;
        }

        /**
         * @return The index of the instruction in the method or -1 for complex values
         */
        public int getOrder() {
            return this.order;
        }

        public void setOrder(final int order) {
            this.order = order;
        }

        public void addNext(final FlowValue value) {
            if (!this.next.contains(value)) this.next.add(value);
        }

        public boolean isComplex() {
            return this.insn == null;
        }

        public boolean typeMatches(final Type desiredType) {
            if (desiredType == null) return true;
            if (ExpressionTypes.isIntLike(desiredType) && ExpressionTypes.isIntLike(this.type)) return true;
            return this.type.equals(desiredType);
        }

        @Override
        public String toString() {
            return "FlowValue{" + (this.insn == null ? "complex" : this.insn.getOpcode()) + ", " + this.type + "}";
        }
    }

    public static class DummyFlowValue extends FlowValue {
        public DummyFlowValue(final Type type) {
            super(type, null);
        }

        @Override
        public FlowValue getInput(final int index) {
            throw new IllegalStateException("Complex flow value");
        }

        @Override
        public int inputCount() {
            return 0;
        }
    }


    private static class FlowInterpreter extends Interpreter<FlowValue> {
        private final LocalsCalculator.Calculator localsCalculator;
        private final Map<VarInsnNode, Type> localTypes;
        private final Map<AbstractInsnNode, FlowValue> flows = new IdentityHashMap<>();

        private FlowInterpreter(final ClassNode classNode, final MethodNode method) {
            super(Opcodes.ASM9);
            this.localsCalculator = new LocalsCalculator.Calculator(classNode, method);
            this.localTypes = this.localsCalculator.calculate();
        }

        private Collection<FlowValue> getFlows() {
            return new ArrayList<>(this.flows.values());
        }

        @Override
        public FlowValue newValue(final Type type) {
            if (type == null) return new DummyFlowValue(ExpressionTypes.NULL_TYPE);
            if (type == Type.VOID_TYPE) return null;
            return new DummyFlowValue(type);
        }

        @Override
        public FlowValue newOperation(final AbstractInsnNode insn) {
            return this.record(ExpressionTypes.getNewType(insn), insn);
        }

        @Override
        public FlowValue copyOperation(final AbstractInsnNode insn, final FlowValue value) {
            switch (insn.getOpcode()) {
                case Opcodes.DUP:
                case Opcodes.DUP_X1:
                case Opcodes.DUP_X2:
                case Opcodes.DUP2:
                case Opcodes.DUP2_X1:
                case Opcodes.DUP2_X2:
                case Opcodes.SWAP:
                    return value;
                case Opcodes.ISTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE:
                case Opcodes.DSTORE:
                case Opcodes.ASTORE:
                    this.record(Type.VOID_TYPE, insn, value);
                    return new DummyFlowValue(value.getType());
                default:
                    VarInsnNode var = (VarInsnNode) insn;
                    Type type = this.localTypes.getOrDefault(var, value.getType());
                    return this.record(type, insn);
            }
        }

        @Override
        public FlowValue unaryOperation(final AbstractInsnNode insn, final FlowValue value) {
            if (insn.getOpcode() == Opcodes.IINC) {
                this.record(Type.VOID_TYPE, insn);
                return new DummyFlowValue(Type.INT_TYPE);
            }
            Type type = ExpressionTypes.getUnaryType(insn);
            return this.record(type, insn, value);
        }

        @Override
        public FlowValue binaryOperation(final AbstractInsnNode insn, final FlowValue value1, final FlowValue value2) {
            if (insn.getOpcode() == Opcodes.AALOAD || insn.getOpcode() == Opcodes.BALOAD) {
                return this.record(ExpressionTypes.getInnerType(value1.getType()), insn, value1, value2);
            }
            return this.record(ExpressionTypes.getBinaryType(insn, value1.getType()), insn, value1, value2);
        }

        @Override
        public FlowValue ternaryOperation(final AbstractInsnNode insn, final FlowValue value1, final FlowValue value2, final FlowValue value3) {
            return this.record(Type.VOID_TYPE, insn, value1, value2, value3);
        }

        @Override
        public FlowValue naryOperation(final AbstractInsnNode insn, final List<? extends FlowValue> values) {
            if (insn instanceof MethodInsnNode && isBoxing((MethodInsnNode) insn) && !values.isEmpty()) {
                return values.get(0);
            }
            return this.record(ExpressionTypes.getNaryType(insn), insn, values.toArray(new FlowValue[0]));
        }

        @Override
        public void returnOperation(final AbstractInsnNode insn, final FlowValue value, final FlowValue expected) {
        }

        @Override
        public FlowValue merge(final FlowValue value1, final FlowValue value2) {
            if (value1.equals(value2)) return value1;
            Type type = ExpressionTypes.getCommonSupertype(value1.getType(), value2.getType());
            Set<FlowValue> sources = new LinkedHashSet<>();
            addSources(sources, value1);
            addSources(sources, value2);
            return new ComplexFlowValue(type, sources);
        }

        private static void addSources(final Set<FlowValue> sources, final FlowValue value) {
            if (value instanceof ComplexFlowValue) sources.addAll(((ComplexFlowValue) value).getSources());
            else sources.add(value);
        }

        private FlowValue record(final Type type, final AbstractInsnNode insn, final FlowValue... inputs) {
            FlowValue value = this.flows.get(insn);
            if (value == null) {
                value = new FlowValue(type, insn, inputs);
                this.flows.put(insn, value);
            }
            return value;
        }

        private static boolean isBoxing(final MethodInsnNode call) {
            Type boxed = Type.getObjectType(call.owner);
            Type unboxed = getUnboxed(boxed);
            if (unboxed == null) return false;
            Type[] args = Type.getArgumentTypes(call.desc);
            return call.name.equals(ExpressionTypes.getUnboxingMethod(unboxed))
                    || call.name.equals("valueOf") && args.length == 1 && args[0].equals(unboxed);
        }

        @Nullable
        private static Type getUnboxed(final Type boxed) {
            if (boxed.getSort() != Type.OBJECT) return null;
            switch (boxed.getInternalName()) {
                case "java/lang/Boolean": return Type.BOOLEAN_TYPE;
                case "java/lang/Character": return Type.CHAR_TYPE;
                case "java/lang/Byte": return Type.BYTE_TYPE;
                case "java/lang/Short": return Type.SHORT_TYPE;
                case "java/lang/Integer": return Type.INT_TYPE;
                case "java/lang/Float": return Type.FLOAT_TYPE;
                case "java/lang/Long": return Type.LONG_TYPE;
                case "java/lang/Double": return Type.DOUBLE_TYPE;
                default: return null;
            }
        }
    }


    public static class LocalsCalculator {
        public static class Calculator extends Interpreter<BasicValue> {
            private final ClassNode classNode;
            private final MethodNode method;
            private final Map<VarInsnNode, Object> results = new IdentityHashMap<>();

            public Calculator(final ClassNode classNode, final MethodNode method) {
                super(Opcodes.ASM9);
                this.classNode = classNode;
                this.method = method;
            }

            public Map<VarInsnNode, Type> calculate() {
                try {
                    new Analyzer<>(this).analyze(this.classNode.name, this.method);
                } catch (AnalyzerException e) {
                    throw new IllegalStateException("Failed to calculate locals of " + this.classNode.name + "." + this.method.name + this.method.desc, e);
                }
                Map<VarInsnNode, Type> result = new IdentityHashMap<>();
                for (Map.Entry<VarInsnNode, Object> entry : this.results.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Type) result.put(entry.getKey(), (Type) value);
                    else if (value instanceof Set) {
                        Type type = null;
                        for (Object typeValue : (Set<?>) value) {
                            type = type == null ? (Type) typeValue : ExpressionTypes.getCommonSupertype(type, (Type) typeValue);
                        }
                        if (type != null) result.put(entry.getKey(), type);
                    }
                }
                return result;
            }

            @Override
            public BasicValue newValue(final Type type) {
                if (type == Type.VOID_TYPE) return null;
                return new BasicValue(type == null ? ExpressionTypes.NULL_TYPE : type);
            }

            @Override
            public BasicValue newOperation(final AbstractInsnNode insn) {
                return new BasicValue(ExpressionTypes.getNewType(insn));
            }

            @Override
            public BasicValue copyOperation(final AbstractInsnNode insn, final BasicValue value) {
                if (insn.getOpcode() >= Opcodes.ILOAD && insn.getOpcode() <= Opcodes.ALOAD) {
                    this.record((VarInsnNode) insn, value.getType());
                }
                return value;
            }

            @Override
            public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value) {
                return new BasicValue(ExpressionTypes.getUnaryType(insn));
            }

            @Override
            public BasicValue binaryOperation(final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2) {
                return new BasicValue(ExpressionTypes.getBinaryType(insn, value1.getType()));
            }

            @Override
            public BasicValue ternaryOperation(final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2, final BasicValue value3) {
                return null;
            }

            @Override
            public BasicValue naryOperation(final AbstractInsnNode insn, final List<? extends BasicValue> values) {
                return new BasicValue(ExpressionTypes.getNaryType(insn));
            }

            @Override
            public void returnOperation(final AbstractInsnNode insn, final BasicValue value, final BasicValue expected) {
            }

            @Override
            public BasicValue merge(final BasicValue value1, final BasicValue value2) {
                if (value1.equals(value2)) return value1;
                return new BasicValue(ExpressionTypes.getCommonSupertype(value1.getType(), value2.getType()));
            }

            private void record(final VarInsnNode insn, final Type type) {
                Object cached = this.results.get(insn);
                if (cached instanceof Type) return;
                LocalVariableNode local = this.getLocalVariableAt(insn);
                if (local != null) {
                    this.results.put(insn, Type.getType(local.desc));
                } else if (cached instanceof Set) {
                    ((Set<Type>) cached).add(type);
                } else {
                    Set<Type> types = new HashSet<>();
                    types.add(type);
                    this.results.put(insn, types);
                }
            }

            @Nullable
            private LocalVariableNode getLocalVariableAt(final VarInsnNode insn) {
                if (this.method.localVariables == null || this.method.localVariables.isEmpty()) return null;
                int position = this.method.instructions.indexOf(insn);
                LocalVariableNode result = null;
                for (LocalVariableNode local : this.method.localVariables) {
                    if (local.index != insn.var || local.desc == null) continue;
                    if (this.method.instructions.indexOf(local.start) <= position && this.method.instructions.indexOf(local.end) > position) result = local;
                }
                return result;
            }
        }
    }

}
