package org.sec.service;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.sec.core.CallGraph;
import org.sec.core.InheritanceMap;
import org.sec.core.xss.ReflectionXSSClassVisitor;
import org.sec.core.xss.StoredXSSClassVisitor;
import org.sec.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * XSS检测
 */
@SuppressWarnings("all")
public class XSSService {
    private static final Logger logger = Logger.getLogger(XSSService.class);

    private static Map<MethodReference.Handle, Set<CallGraph>> allCalls;
    private static Map<String, ClassFile> classFileMap;
    private static InheritanceMap localInheritanceMap;
    private static Map<MethodReference.Handle, Set<Integer>> localDataFlow;
    private static final List<String> tempChain = new ArrayList<>();
    private static final List<ResultInfo> results = new ArrayList<>();

    private static boolean mayXSS;
    private static boolean saveStoredXSS;

    public static void startReflection(Map<String, ClassFile> classFileByName,
                                       List<SpringController> controllers,
                                       InheritanceMap inheritanceMap,
                                       Map<MethodReference.Handle, Set<Integer>> dataFlow,
                                       Map<MethodReference.Handle, Set<CallGraph>> discoveredCalls) {
        results.clear();
        tempChain.clear();
        mayXSS = false;

        allCalls = discoveredCalls;
        classFileMap = classFileByName;
        localInheritanceMap = inheritanceMap;
        localDataFlow = dataFlow;

        logger.info("analysis reflection xss...");
        for (SpringController controller : controllers) {
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference methodReference = mapping.getMethodReference();
                if (methodReference == null) {
                    continue;
                }
                Type[] argTypes = Type.getArgumentTypes(methodReference.getDesc());
                Type[] extendedArgTypes = new Type[argTypes.length + 1];
                System.arraycopy(argTypes, 0, extendedArgTypes, 1, argTypes.length);
                argTypes = extendedArgTypes;
                boolean[] vulnerableIndex = new boolean[argTypes.length];
                for (int i = 1; i < argTypes.length; i++) {
                    if (argTypes[i].getClassName().equals("java.lang.String")) {
                        vulnerableIndex[i] = true;
                    }
                }
                Set<CallGraph> calls = allCalls.get(methodReference.getHandle());
                if (calls == null || calls.size() == 0) {
                    continue;
                }
                tempChain.add(methodReference.getClassReference().getName() + "." + methodReference.getName());
                for (CallGraph callGraph : calls) {
                    int callerIndex = callGraph.getCallerArgIndex();
                    if (callerIndex == -1) {
                        continue;
                    }
                    if (vulnerableIndex[callerIndex]) {
                        Set<Integer> flows = dataFlow.get(methodReference.getHandle());
                        if (flows != null && flows.size() != 0) {
                            if (flows.contains(callerIndex)) {
                                mayXSS = true;
                            }
                        }
                        tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                                callGraph.getTargetMethod().getName());
                        // 防止循环
                        List<MethodReference.Handle> visited = new ArrayList<>();
                        doReflectionTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
                        if (mayXSS) {
                            ResultInfo resultInfo = new ResultInfo();
                            resultInfo.setRisk(ResultInfo.LOW_RISK);
                            resultInfo.setVulnName("Reflection XSS");
                            resultInfo.getChains().addAll(tempChain);
                            results.add(resultInfo);
                            String message = callGraph.getTargetMethod().getClassReference().getName() + "." +
                                    callGraph.getTargetMethod().getName();
                            logger.info("detect reflection xss: " + message);
                        }
                        mayXSS = false;
                    }
                }
                tempChain.clear();
            }
        }
    }

    public static void startStored(Map<String, ClassFile> classFileByName,
                                   List<SpringController> controllers,
                                   InheritanceMap inheritanceMap,
                                   Map<MethodReference.Handle, Set<Integer>> dataFlow,
                                   Map<MethodReference.Handle, Set<CallGraph>> discoveredCalls) {
        results.clear();
        tempChain.clear();
        saveStoredXSS = false;

        allCalls = discoveredCalls;
        classFileMap = classFileByName;
        localInheritanceMap = inheritanceMap;
        localDataFlow = dataFlow;

        logger.info("analysis stored xss...");
        for (SpringController controller : controllers) {
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference methodReference = mapping.getMethodReference();
                if (methodReference == null) {
                    continue;
                }
                Type[] argTypes = Type.getArgumentTypes(methodReference.getDesc());
                Type[] extendedArgTypes = new Type[argTypes.length + 1];
                System.arraycopy(argTypes, 0, extendedArgTypes, 1, argTypes.length);
                argTypes = extendedArgTypes;
                boolean[] vulnerableIndex = new boolean[argTypes.length];
                for (int i = 1; i < argTypes.length; i++) {
                    if (argTypes[i].getClassName().equals("java.lang.String")) {
                        vulnerableIndex[i] = true;
                    }
                }
                Set<CallGraph> calls = allCalls.get(methodReference.getHandle());
                if (calls == null || calls.size() == 0) {
                    continue;
                }
                tempChain.add(methodReference.getClassReference().getName() + "." + methodReference.getName());
                for (CallGraph callGraph : calls) {
                    int callerIndex = callGraph.getCallerArgIndex();
                    if (callerIndex == -1) {
                        continue;
                    }
                    if (vulnerableIndex[callerIndex]) {
                        Set<Integer> flows = dataFlow.get(methodReference.getHandle());
                        tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                                callGraph.getTargetMethod().getName());
                        // 防止循环
                        List<MethodReference.Handle> visited = new ArrayList<>();
                        doStoredTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
                        if (saveStoredXSS) {
                            ResultInfo resultInfo = new ResultInfo();
                            resultInfo.setRisk(ResultInfo.HIGH_RISK);
                            resultInfo.setVulnName("Stored XSS");
                            resultInfo.getChains().addAll(tempChain);
                            results.add(resultInfo);
                            String message = callGraph.getTargetMethod().getClassReference().getName() + "." +
                                    callGraph.getTargetMethod().getName();
                            logger.info("detect stored xss: " + message);
                        }
                        saveStoredXSS = false;
                    }
                }
                tempChain.clear();
            }
        }
    }

    public static List<ResultInfo> getResults() {
        return results;
    }

    private static void doReflectionTask(MethodReference.Handle targetMethod, int targetIndex,
                                         List<MethodReference.Handle> visited) {
        if (visited.contains(targetMethod)) {
            return;
        } else {
            visited.add(targetMethod);
        }
        ClassFile file = classFileMap.get(targetMethod.getClassReference().getName());
        try {
            InputStream ins = file.getInputStream();
            ClassReader cr = new ClassReader(ins);
            ins.close();
            ReflectionXSSClassVisitor cv = new ReflectionXSSClassVisitor(
                    targetMethod, targetIndex, localInheritanceMap, localDataFlow);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            if (cv.getEscape().contains(true)) {
                logger.info("no reflection xss because escape method");
                mayXSS = false;
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Set<CallGraph> calls = allCalls.get(targetMethod);
        if (calls == null || calls.size() == 0) {
            return;
        }
        for (CallGraph callGraph : calls) {
            if (callGraph.getCallerArgIndex() == targetIndex && targetIndex != -1) {
                if (visited.contains(callGraph.getTargetMethod())) {
                    return;
                }
                tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                        callGraph.getTargetMethod().getName());
                doReflectionTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
            }
        }
    }

    private static void doStoredTask(MethodReference.Handle targetMethod, int targetIndex,
                                     List<MethodReference.Handle> visited) {
        if (visited.contains(targetMethod)) {
            return;
        } else {
            visited.add(targetMethod);
        }
        ClassFile file = classFileMap.get(targetMethod.getClassReference().getName());
        try {
            InputStream ins = file.getInputStream();
            ClassReader cr = new ClassReader(ins);
            ins.close();
            StoredXSSClassVisitor cv = new StoredXSSClassVisitor(
                    targetMethod, targetIndex, localInheritanceMap, localDataFlow);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            if (cv.getSave().contains(true)) {
                    saveStoredXSS = true;
                    return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Set<CallGraph> calls = allCalls.get(targetMethod);
        if (calls == null || calls.size() == 0) {
            return;
        }
        for (CallGraph callGraph : calls) {
            if (callGraph.getCallerArgIndex() == targetIndex && targetIndex != -1) {
                if (visited.contains(callGraph.getTargetMethod())) {
                    return;
                }
                tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                        callGraph.getTargetMethod().getName());
                doStoredTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
            }
        }
    }
}
