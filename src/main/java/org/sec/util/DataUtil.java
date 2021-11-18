package org.sec.util;

import org.sec.core.CallGraph;
import org.sec.model.MethodReference;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * 保存运行数据相关的Util
 */
public class DataUtil {
    private static final String DataFlowFile = "dataflow.dat";
    private static final String TargetDataFlowFile = "target-dataflow.dat";
    private static final String CallGraphFile = "callgraph.dat";
    private static final String TargetCallGraphFile = "target-callgraph.dat";

    /**
     * 保存DataFlow
     * @param dataFlow dataFlow
     * @param methodMap methodMap
     * @param packageName packageName
     */
    @SuppressWarnings("all")
    public static void SaveDataFlows(Map<MethodReference.Handle, Set<Integer>> dataFlow,
                                     Map<MethodReference.Handle, MethodReference> methodMap,
                                     String packageName) {
        try {
            // All DataFlow
            Path filePath = Paths.get(DataFlowFile);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            StringBuilder dataFlowStr = new StringBuilder();
            for (MethodReference.Handle handle : dataFlow.keySet()) {
                String className = methodMap.get(handle).getClassReference().getName();
                String methodName = handle.getName();
                Set<Integer> results = dataFlow.get(handle);
                if (results != null && results.size() != 0) {
                    dataFlowStr.append("results:");
                    for (Integer i : results) {
                        dataFlowStr.append(i).append(" ");
                    }
                } else {
                    continue;
                }
                dataFlowStr.append(className).append("\t");
                dataFlowStr.append(methodName).append("\t");
                dataFlowStr.append("\n");
            }
            FileUtil.writeFile(DataFlowFile, dataFlowStr.toString());

            // Target DataFlow
            Path targetFilePath = Paths.get(TargetDataFlowFile);
            if (Files.exists(targetFilePath)) {
                Files.delete(targetFilePath);
            }
            StringBuilder targetDataFlowStr = new StringBuilder();
            for (MethodReference.Handle handle : dataFlow.keySet()) {
                String className = methodMap.get(handle).getClassReference().getName();
                if (!className.contains(packageName)) {
                    continue;
                }
                String methodName = handle.getName();
                Set<Integer> results = dataFlow.get(handle);
                if (results != null && results.size() != 0) {
                    targetDataFlowStr.append("results:");
                    for (Integer i : results) {
                        targetDataFlowStr.append(i).append(" ");
                    }
                } else {
                    continue;
                }
                targetDataFlowStr.append(className).append("\t");
                targetDataFlowStr.append(methodName).append("\t");
                targetDataFlowStr.append("\n");
            }
            FileUtil.writeFile(TargetDataFlowFile, targetDataFlowStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存CallGraph
     * @param discoveredCalls discoveredCalls
     * @param packageName packageName
     */
    public static void SaveCallGraphs(Set<CallGraph> discoveredCalls, String packageName) {
        try {
            // All CallGraph
            Path filePath = Paths.get(CallGraphFile);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            StringBuilder callGraphStr = new StringBuilder();
            for (CallGraph callGraph : discoveredCalls) {
                String callerClass = callGraph.getCallerMethod().getClassReference().getName();
                String callerMethod = callGraph.getCallerMethod().getName();
                Integer callerArgIndex = callGraph.getCallerArgIndex();
                String targetClass = callGraph.getTargetMethod().getClassReference().getName();
                String targetMethod = callGraph.getTargetMethod().getName();
                Integer targetArgIndex = callGraph.getTargetArgIndex();
                callGraphStr.append("caller:").append(callerClass).append(".")
                        .append(callerMethod).append("(").append(callerArgIndex).append(")")
                        .append("\n").append("target:").append(targetClass).append(".")
                        .append(targetMethod).append("(").append(targetArgIndex).append(")").append("\n")
                        .append("--------------------------------------------------------------------\n");
            }
            FileUtil.writeFile(CallGraphFile, callGraphStr.toString());
            // Target CallGraph
            Path targetFilePath = Paths.get(TargetCallGraphFile);
            if (Files.exists(targetFilePath)) {
                Files.delete(targetFilePath);
            }
            StringBuilder targetCallGraphStr = new StringBuilder();
            for (CallGraph callGraph : discoveredCalls) {
                String callerClass = callGraph.getCallerMethod().getClassReference().getName();
                if (!callerClass.contains(packageName)) {
                    continue;
                }
                String callerMethod = callGraph.getCallerMethod().getName();
                Integer callerArgIndex = callGraph.getCallerArgIndex();
                String targetClass = callGraph.getTargetMethod().getClassReference().getName();
                String targetMethod = callGraph.getTargetMethod().getName();
                Integer targetArgIndex = callGraph.getTargetArgIndex();
                targetCallGraphStr.append("caller:").append(callerClass).append(".")
                        .append(callerMethod).append("(").append(callerArgIndex).append(")")
                        .append("\n").append("target:").append(targetClass).append(".")
                        .append(targetMethod).append("(").append(targetArgIndex).append(")").append("\n")
                        .append("--------------------------------------------------------------------\n");
            }
            FileUtil.writeFile(TargetCallGraphFile, targetCallGraphStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
