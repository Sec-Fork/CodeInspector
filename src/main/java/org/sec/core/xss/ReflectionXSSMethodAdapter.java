package org.sec.core.xss;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.sec.core.CoreMethodAdapter;
import org.sec.core.InheritanceMap;
import org.sec.model.MethodReference;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReflectionXSSMethodAdapter extends CoreMethodAdapter<Boolean> {
    private final int access;
    private final String desc;
    private final int methodArgIndex;
    private final List<Boolean> escape;

    public ReflectionXSSMethodAdapter(int methodArgIndex, List<Boolean> escape, InheritanceMap inheritanceMap,
                                      Map<MethodReference.Handle, Set<Integer>> passthroughDataflow,
                                      int api, MethodVisitor mv, String owner, int access, String name,
                                      String desc, String signature, String[] exceptions) {
        super(inheritanceMap, passthroughDataflow, api, mv, owner, access, name, desc, signature, exceptions);
        this.access = access;
        this.desc = desc;
        this.methodArgIndex = methodArgIndex;
        this.escape = escape;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        int localIndex = 0;
        int argIndex = 0;
        if ((this.access & Opcodes.ACC_STATIC) == 0) {
            localIndex += 1;
            argIndex += 1;
        }
        for (Type argType : Type.getArgumentTypes(desc)) {
            if (argIndex == this.methodArgIndex) {
                localVariables.set(localIndex, true);
            }
            localIndex += argType.getSize();
            argIndex += 1;
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        boolean escapeCondition = owner.equals("org/apache/commons/lang/StringEscapeUtils") &&
                name.equals("escapeHtml") &&
                desc.equals("(Ljava/lang/String;)Ljava/lang/String;");

        boolean isTaint = false;
        Type[] argTypes = Type.getArgumentTypes(desc);

        if (escapeCondition) {
            int stackIndex = 0;
            for (int i = 0; i < argTypes.length; i++) {
                int argIndex = argTypes.length - 1 - i;
                Type type = argTypes[argIndex];
                Set<Boolean> taint = operandStack.get(stackIndex);
                if (taint.size() > 0 && taint.contains(true)) {
                    isTaint = true;
                    break;
                }
                stackIndex += type.getSize();
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (isTaint) {
                if (operandStack.size() > 0) {
                    escape.add(true);
                }
            }
            return;
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }
}
