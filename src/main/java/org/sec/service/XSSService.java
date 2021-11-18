package org.sec.service;

import org.apache.log4j.Logger;
import org.sec.core.CallGraph;
import org.sec.core.InheritanceMap;
import org.sec.model.ClassFile;
import org.sec.model.MethodReference;
import org.sec.model.ResultInfo;
import org.sec.model.SpringController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    }
}
