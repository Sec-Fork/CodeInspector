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

public class StoredXSSMethodAdapter extends CoreMethodAdapter<Boolean> {
    private final int access;
    private final String desc;
    private final int methodArgIndex;
    private final List<Boolean> save;

    public StoredXSSMethodAdapter(int methodArgIndex, List<Boolean> save,
                                  InheritanceMap inheritanceMap,
                                  Map<MethodReference.Handle, Set<Integer>> passthroughDataflow,
                                  int api, MethodVisitor mv, String owner, int access, String name,
                                  String desc, String signature, String[] exceptions) {
        super(inheritanceMap, passthroughDataflow, api, mv, owner, access, name, desc, signature, exceptions);
        this.access = access;
        this.desc = desc;
        this.methodArgIndex = methodArgIndex;
        this.save = save;
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
        boolean buildSqlCondition = owner.equals("java/lang/StringBuilder") &&
                name.equals("append") &&
                desc.equals("(Ljava/lang/String;)Ljava/lang/StringBuilder;");

        boolean toStringCondition = owner.equals("java/lang/StringBuilder") &&
                name.equals("toString") &&
                desc.equals("()Ljava/lang/String;");

        boolean jdbcUpdateCondition = owner.equals("org/springframework/jdbc/core/JdbcTemplate") &&
                name.equals("update") && desc.equals("(Ljava/lang/String;)I");

        if (buildSqlCondition) {
            if (operandStack.get(0).contains(true)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                operandStack.set(0, true);
                return;
            }
        }

        if (toStringCondition) {
            if (operandStack.get(0).contains(true)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                operandStack.set(0, true);
                return;
            }
        }

        if (jdbcUpdateCondition) {
            if (operandStack.get(0).contains(true)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                save.add(true);
                return;
            }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }
}
