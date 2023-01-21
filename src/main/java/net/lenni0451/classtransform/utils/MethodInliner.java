package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CInline;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Util to inline methods into other methods.<br>
 * This is used for the {@link CInline} annotation.
 */
public class MethodInliner {

    /**
     * Inline a method into all methods of a class.<br>
     * Return opcodes are replaced with a jump instruction behind the inlined method instructions.<br>
     * Direct modifications of arguments are not supported.
     *
     * @param classNode          The class with the methods to inline into
     * @param inlinedMethod      The method to inline
     * @param inlinedMethodOwner The owner of the method to inline
     */
    public static void wrappedInline(final ClassNode classNode, final MethodNode inlinedMethod, final String inlinedMethodOwner) {
        for (MethodNode method : classNode.methods) {
            AbstractInsnNode[] inlinedInstructions = instructionCalling(method, Modifier.isStatic(inlinedMethod.access) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, inlinedMethodOwner, inlinedMethod.name, inlinedMethod.desc);
            for (AbstractInsnNode inlinedInstruction : inlinedInstructions) wrappedInline(classNode.name, method, inlinedInstruction, ASMUtils.cloneMethod(inlinedMethod));
        }
        classNode.methods.remove(inlinedMethod);
    }

    private static AbstractInsnNode[] instructionCalling(final MethodNode method, final int callOpcode, final String owner, final String name, final String desc) {
        List<AbstractInsnNode> insns = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode && instruction.getOpcode() == callOpcode) {
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                if (methodInsn.owner.equals(owner) && methodInsn.name.equals(name) && methodInsn.desc.equals(desc)) insns.add(instruction);
            }
        }
        return insns.toArray(new AbstractInsnNode[0]);
    }

    private static void wrappedInline(final String methodOwner, final MethodNode method, final AbstractInsnNode inlinedInstruction, final MethodNode inlinedMethod) {
        int freeVarSpace = ASMUtils.getFreeVarIndex(method);
        Map<Integer, Integer> varMappings = new HashMap<>();
        List<StackVariable> stackVariables = new ArrayList<>();
        Type[] inlinedMethodArgs = Type.getArgumentTypes(inlinedMethod.desc);
        Type inlinedReturnType = Type.getReturnType(inlinedMethod.desc);
        LabelNode returnLabel = new LabelNode();
        InsnList instructions = new InsnList();
        {
            int thisOffset = Modifier.isStatic(inlinedMethod.access) ? 0 : 1;
            for (int i = inlinedMethodArgs.length - 1; i >= 0; i--) { //Store method arguments in local variables
                Type argType = inlinedMethodArgs[i];
                instructions.add(new VarInsnNode(argType.getOpcode(Opcodes.ISTORE), freeVarSpace));
                varMappings.put(i + thisOffset, freeVarSpace);
                freeVarSpace += argType.getSize();
            }
            if (!Modifier.isStatic(inlinedMethod.access)) {
                instructions.add(new VarInsnNode(Opcodes.ASTORE, freeVarSpace));
                varMappings.put(0, freeVarSpace);
                freeVarSpace++;
            }
        }
        try {
            //Use the ASM analyzer to store the rest of the stack in local variables
            //There will be stack issues with try-catch blocks if this is not done
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            Frame<BasicValue>[] frames = analyzer.analyze(methodOwner, method);
            Frame<BasicValue> inlinedInstructionFrame = frames[method.instructions.indexOf(inlinedInstruction)];
            if (inlinedInstructionFrame != null) {
                int stackSize = inlinedInstructionFrame.getStackSize() - varMappings.size(); //Remove the method arguments from the stack size since they are already stored
                for (int i = 0; i < stackSize; i++) {
                    //0 is the top of the stack
                    Type stackType = inlinedInstructionFrame.getStack(stackSize - i - 1).getType();
                    instructions.add(new VarInsnNode(stackType.getOpcode(Opcodes.ISTORE), freeVarSpace));
                    stackVariables.add(new StackVariable(stackType, freeVarSpace));
                    freeVarSpace += stackType.getSize();
                }
            }
        } catch (Throwable ignored) {
        }
        for (AbstractInsnNode instruction : inlinedMethod.instructions) {
            if (instruction instanceof FrameNode) continue;
            if (instruction.getOpcode() >= Opcodes.IRETURN && instruction.getOpcode() <= Opcodes.RETURN) {
                //Replace return instructions with a jump to the return label
                instructions.add(new JumpInsnNode(Opcodes.GOTO, returnLabel));
                continue;
            }

            //Change the var index of all local variable access instructions (VarInsnNode, IincInsnNode)
            if (instruction instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) instruction;
                varMappings.putIfAbsent(varInsn.var, varInsn.var + freeVarSpace);
                varInsn.var = varMappings.get(varInsn.var);
            } else if (instruction instanceof IincInsnNode) {
                IincInsnNode iincInsn = (IincInsnNode) instruction;
                varMappings.putIfAbsent(iincInsn.var, iincInsn.var + freeVarSpace);
                iincInsn.var = varMappings.get(iincInsn.var);
            }
            instructions.add(instruction);
        }
        instructions.add(returnLabel);
        if (!stackVariables.isEmpty()) { //If the stack was not empty before inlining push all the stored stack variables back on the stack
            int returnVar = freeVarSpace + varMappings.values().stream().mapToInt(i -> i).max().orElse(0);
            if (!inlinedReturnType.equals(Type.VOID_TYPE)) instructions.add(new VarInsnNode(inlinedReturnType.getOpcode(Opcodes.ISTORE), returnVar));
            for (int i = stackVariables.size() - 1; i >= 0; i--) {
                StackVariable stackVariable = stackVariables.get(i);
                instructions.add(new VarInsnNode(stackVariable.getType().getOpcode(Opcodes.ILOAD), stackVariable.getVarIndex()));
            }
            if (!inlinedReturnType.equals(Type.VOID_TYPE)) instructions.add(new VarInsnNode(inlinedReturnType.getOpcode(Opcodes.ILOAD), returnVar));
        }

        method.instructions.insertBefore(inlinedInstruction, instructions);
        method.instructions.remove(inlinedInstruction);

        //Merge other method attributes
        method.tryCatchBlocks = mergeTryCatchBlockNodes(method.tryCatchBlocks, inlinedMethod.tryCatchBlocks);
        method.localVariables = mergeLocalVariableTable(method.localVariables, inlinedMethod.localVariables, freeVarSpace, varMappings);
        method.exceptions = mergeExceptions(method.exceptions, inlinedMethod.exceptions);
    }

    private static List<TryCatchBlockNode> mergeTryCatchBlockNodes(final List<TryCatchBlockNode> tryCatchBlockNodes, final List<TryCatchBlockNode> inlinedTryCatchBlockNodes) {
        List<TryCatchBlockNode> mergedTryCatchBlockNodes = new ArrayList<>();
        if (tryCatchBlockNodes != null) mergedTryCatchBlockNodes.addAll(tryCatchBlockNodes);
        if (inlinedTryCatchBlockNodes != null) mergedTryCatchBlockNodes.addAll(inlinedTryCatchBlockNodes);
        return mergedTryCatchBlockNodes;
    }

    private static List<LocalVariableNode> mergeLocalVariableTable(final List<LocalVariableNode> localVariables, final List<LocalVariableNode> inlinedLocalVariables, final int freeVarSpace, final Map<Integer, Integer> varMappings) {
        List<LocalVariableNode> mergedLocalVariables = new ArrayList<>();
        if (localVariables != null) mergedLocalVariables.addAll(localVariables);
        if (inlinedLocalVariables != null) {
            for (LocalVariableNode inlinedLocalVariable : inlinedLocalVariables) {
                if (varMappings.containsKey(inlinedLocalVariable.index)) inlinedLocalVariable.index = varMappings.get(inlinedLocalVariable.index);
                else inlinedLocalVariable.index += freeVarSpace;
                mergedLocalVariables.add(inlinedLocalVariable);
            }
        }
        return mergedLocalVariables;
    }

    private static List<String> mergeExceptions(final List<String> exceptions, final List<String> inlinedExceptions) {
        Set<String> mergedExceptions = new HashSet<>();
        if (exceptions != null) mergedExceptions.addAll(exceptions);
        if (inlinedExceptions != null) mergedExceptions.addAll(inlinedExceptions);
        return new ArrayList<>(mergedExceptions);
    }


    private static class StackVariable {
        private final Type type;
        private final int varIndex;

        StackVariable(final Type type, final int varIndex) {
            this.type = type;
            this.varIndex = varIndex;
        }

        public Type getType() {
            return this.type;
        }

        public int getVarIndex() {
            return this.varIndex;
        }
    }

}
