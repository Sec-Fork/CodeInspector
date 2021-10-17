package com.emyiqing.core;

import com.emyiqing.data.InheritanceMap;
import com.emyiqing.model.ClassReference;
import com.emyiqing.model.MethodReference;
import com.emyiqing.service.Decider;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

@SuppressWarnings("all")
public class DataFlowMethodAdapter extends CoreMethodAdapter<Integer> {

    private final Map<ClassReference.Handle, ClassReference> classMap;
    private final InheritanceMap inheritanceMap;
    private final Decider decider;
    private final Map<MethodReference.Handle, Set<Integer>> passthroughDataflow;

    private final int access;
    private final String desc;
    private final Set<Integer> returnTaint;

    public DataFlowMethodAdapter(Map<ClassReference.Handle, ClassReference> classMap,
                                 InheritanceMap inheritanceMap, Decider decider,
                                 Map<MethodReference.Handle, Set<Integer>> passthroughDataflow,
                                 MethodVisitor mv, String owner, int access, String name,
                                 String desc, String signature, String[] exceptions) {
        super(inheritanceMap, passthroughDataflow,
                Opcodes.ASM6, mv, owner, access, name, desc, signature, exceptions);
        this.classMap = classMap;
        this.inheritanceMap = inheritanceMap;
        this.passthroughDataflow = passthroughDataflow;
        this.access = access;
        this.desc = desc;
        this.decider = decider;
        returnTaint = new HashSet<>();
    }

    private static boolean couldBeSerialized(Decider decider, InheritanceMap inheritanceMap,
                                             ClassReference.Handle clazz) {
        if (Boolean.TRUE.equals(decider.apply(clazz))) {
            return true;
        }
        Set<ClassReference.Handle> subClasses = inheritanceMap.getSubClasses(clazz);
        if (subClasses != null) {
            for (ClassReference.Handle subClass : subClasses) {
                if (Boolean.TRUE.equals(decider.apply(subClass))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        int localIndex = 0;
        int argIndex = 0;
        if ((this.access & Opcodes.ACC_STATIC) == 0) {
            localVariables.set(localIndex, argIndex);
            localIndex += 1;
            argIndex += 1;
        }
        for (Type argType : Type.getArgumentTypes(desc)) {
            localVariables.set(localIndex, argIndex);
            localIndex += argType.getSize();
            argIndex += 1;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.ARETURN:
                returnTaint.addAll(operandStack.get(0));
                break;
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
                returnTaint.addAll(operandStack.get(1));
                break;
            case Opcodes.RETURN:
                break;
            default:
                break;
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        switch (opcode) {
            case Opcodes.GETSTATIC:
                break;
            case Opcodes.PUTSTATIC:
                break;
            case Opcodes.GETFIELD:
                Type type = Type.getType(desc);
                if (type.getSize() != 1) {
                    break;
                }
                Boolean isTransient = null;
                if (!couldBeSerialized(decider, inheritanceMap,
                        new ClassReference.Handle(type.getInternalName()))) {
                    isTransient = Boolean.TRUE;
                } else {
                    ClassReference clazz = classMap.get(new ClassReference.Handle(owner));
                    while (clazz != null) {
                        for (ClassReference.Member member : clazz.getMembers()) {
                            if (member.getName().equals(name)) {
                                isTransient = (member.getModifiers() & Opcodes.ACC_TRANSIENT) != 0;
                                break;
                            }
                        }
                        if (isTransient != null) {
                            break;
                        }
                        clazz = classMap.get(new ClassReference.Handle(clazz.getSuperClass()));
                    }
                }

                Set<Integer> taint;
                if (!Boolean.TRUE.equals(isTransient)) {
                    taint = operandStack.get(0);
                } else {
                    taint = new HashSet<>();
                }

                super.visitFieldInsn(opcode, owner, name, desc);
                operandStack.set(0, taint);
                return;
            case Opcodes.PUTFIELD:
                break;
            default:
                throw new IllegalStateException("unsupported opcode: " + opcode);
        }

        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        Type[] argTypes = Type.getArgumentTypes(desc);
        if (opcode != Opcodes.INVOKESTATIC) {
            Type[] extendedArgTypes = new Type[argTypes.length+1];
            System.arraycopy(argTypes, 0, extendedArgTypes, 1, argTypes.length);
            extendedArgTypes[0] = Type.getObjectType(owner);
            argTypes = extendedArgTypes;
        }
        int retSize = Type.getReturnType(desc).getSize();
        Set<Integer> resultTaint;
        switch (opcode) {
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKEINTERFACE:
                final List<Set<Integer>> argTaint = new ArrayList<>(argTypes.length);
                for (int i = 0; i < argTypes.length; i++) {
                    argTaint.add(null);
                }
                int stackIndex = 0;
                for (int i = 0; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    if (argType.getSize() > 0) {
                        argTaint.set(argTypes.length - 1 - i,
                                operandStack.get(stackIndex+argType.getSize()-1));
                    }
                    stackIndex += argType.getSize();
                }
                if (name.equals("<init>")) {
                    resultTaint = argTaint.get(0);
                } else {
                    resultTaint = new HashSet<>();
                }
                Set<Integer> passthrough = passthroughDataflow.get(
                        new MethodReference.Handle(new ClassReference.Handle(owner), name, desc));
                if (passthrough != null) {
                    for (Integer passthroughDataflowArg : passthrough) {
                        resultTaint.addAll(argTaint.get(passthroughDataflowArg));
                    }
                }
                break;
            default:
                throw new IllegalStateException("unsupported opcode: " + opcode);
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if (retSize > 0) {
            operandStack.get(retSize-1).addAll(resultTaint);
        }
    }

    public Set<Integer> getReturnTaint() {
        return returnTaint;
    }
}
