package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.CTargetImpl;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Select a void method call with one literal string argument.<br>
 * The string is specified with {@code ldc=...} in {@link CTarget#args()}.
 */
@ParametersAreNonnullByDefault
public class InvokeStringTarget extends InvokeTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        String literal = null;
        for (String argument : target.args()) {
            if (argument.startsWith("ldc=")) literal = argument.substring(4);
        }
        if (literal == null) throw new IllegalArgumentException("INVOKE_STRING requires an ldc argument");
        List<AbstractInsnNode> calls = super.getTargets(injectionTargets, method, CTargetImpl.invoke(target.target(), -1), slice);
        if (calls == null) return null;
        List<AbstractInsnNode> result = new ArrayList<>();
        int ordinal = 0;
        for (AbstractInsnNode instruction : calls) {
            if (!((MethodInsnNode) instruction).desc.equals("(Ljava/lang/String;)V")) continue;
            AbstractInsnNode previous = instruction.getPrevious();
            while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
            if (!(previous instanceof LdcInsnNode) || !literal.equals(((LdcInsnNode) previous).cst)) continue;
            if (target.ordinal() == -1 || target.ordinal() == ordinal) result.add(instruction);
            ordinal++;
        }
        return result;
    }

}
