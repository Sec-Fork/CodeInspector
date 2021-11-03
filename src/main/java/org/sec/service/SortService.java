package org.sec.service;

import org.sec.core.Sort;
import org.sec.model.MethodReference;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * 在DataFlow分析中需要拓扑逆排序
 * 参考我在先知社区的文章：https://xz.aliyun.com/t/10363#toc-52
 */
public class SortService {
    private static final Logger logger = Logger.getLogger(SortService.class);

    public static List<MethodReference.Handle> start(
            Map<MethodReference.Handle, Set<MethodReference.Handle>> methodCalls) {
        logger.info("topological sort methods");
        Map<MethodReference.Handle, Set<MethodReference.Handle>> outgoingReferences = new HashMap<>();
        for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle method = entry.getKey();
            outgoingReferences.put(method, new HashSet<>(entry.getValue()));
        }
        Set<MethodReference.Handle> dfsStack = new HashSet<>();
        Set<MethodReference.Handle> visitedNodes = new HashSet<>();
        List<MethodReference.Handle> sortedMethods = new ArrayList<>(outgoingReferences.size());
        for (MethodReference.Handle root : outgoingReferences.keySet()) {
            Sort.dfsSort(outgoingReferences, sortedMethods, visitedNodes, dfsStack, root);
        }
        return sortedMethods;
    }
}
