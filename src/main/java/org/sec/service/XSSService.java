package org.sec.service;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.sec.core.CallGraph;
import org.sec.core.InheritanceMap;
import org.sec.core.ssrf.SimpleSSRFClassVisitor;
import org.sec.core.xss.ReflectionXSSClassVisitor;
import org.sec.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * XSS检测
 */
public class XSSService {
    private static final Logger logger = Logger.getLogger(XSSService.class);

    private static Map<MethodReference.Handle, Set<CallGraph>> allCalls;
    private static Map<String, ClassFile> classFileMap;
    private static InheritanceMap localInheritanceMap;
    private static Map<MethodReference.Handle, Set<Integer>> localDataFlow;
    private static final List<String> tempChain = new ArrayList<>();
    private static final List<ResultInfo> results = new ArrayList<>();

    public static void startReflection(Map<String, ClassFile> classFileByName,
                             List<SpringController> controllers,
                             InheritanceMap inheritanceMap,
                             Map<MethodReference.Handle, Set<Integer>> dataFlow,
                             Map<MethodReference.Handle, Set<CallGraph>> discoveredCalls) {
        allCalls = discoveredCalls;
        classFileMap = classFileByName;
        localInheritanceMap = inheritanceMap;
        localDataFlow = dataFlow;

        logger.info("analysis xss...");
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
                // mapping method中的call
                for (CallGraph callGraph : calls) {
                    int callerIndex = callGraph.getCallerArgIndex();
                    if (callerIndex == -1) {
                        continue;
                    }
                    if (vulnerableIndex[callerIndex]) {
                        tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                                callGraph.getTargetMethod().getName());
                        // 防止循环
                        List<MethodReference.Handle> visited = new ArrayList<>();
                        doTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
                    }
                }
                tempChain.clear();
            }
        }
    }

    public static List<ResultInfo> getResults() {
        return results;
    }

    private static void doTask(MethodReference.Handle targetMethod, int targetIndex,
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
            if(cv.getEscape().contains(true)){
                logger.info("no reflection xss because escape method");
            }else{
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setRisk(ResultInfo.LOW_RISK);
                resultInfo.setVulnName("Reflection XSS");
                resultInfo.getChains().addAll(tempChain);
                results.add(resultInfo);
                String message = targetMethod.getClassReference().getName() + "." + targetMethod.getName();
                logger.info("detect reflection xss: " + message);
            }
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
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
                doTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
            }
        }
    }
}
