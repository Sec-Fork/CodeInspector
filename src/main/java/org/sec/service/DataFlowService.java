package org.sec.service;

import org.sec.core.DataFlowClassVisitor;
import org.sec.core.InheritanceMap;
import org.sec.model.ClassFile;
import org.sec.model.ClassReference;
import org.sec.model.MethodReference;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 参考GadgetInspector中的DataFlow
 * 分析每个方法返回值和入参的关系
 */
public class DataFlowService {
    private static final Logger logger = Logger.getLogger(DataFlowService.class);

    public static void start(InheritanceMap inheritanceMap,
                             List<MethodReference.Handle> sortedMethods,
                             Map<String, ClassFile> classFileByName,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             Map<MethodReference.Handle, Set<Integer>> dataflow) {
        logger.info("get data flow");
        for (MethodReference.Handle method : sortedMethods) {
            if (method.getName().equals("<clinit>")) {
                continue;
            }
            ClassFile file = classFileByName.get(method.getClassReference().getName());
            try {
                InputStream ins = file.getInputStream();
                ClassReader cr = new ClassReader(ins);
                ins.close();
                DataFlowClassVisitor cv = new DataFlowClassVisitor(classMap, inheritanceMap, dataflow, method);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                if (dataflow.get(method) != null && dataflow.get(method).size() != 0) {
                    continue;
                }
                dataflow.put(method, cv.getReturnTaint());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateInterfaceDataFlow(List<MethodReference> discoveredMethods,
                                               Map<ClassReference.Handle, ClassReference> classMap,
                                               Map<MethodReference.Handle, Set<Integer>> dataFlow,
                                               String finalPackageName) {
        Map<MethodReference.Handle, Set<Integer>> finalDataFlow = new HashMap<>();
        for (MethodReference.Handle method : dataFlow.keySet()) {
            if (dataFlow.get(method) != null && dataFlow.get(method).size() != 0) {
                finalDataFlow.put(method, dataFlow.get(method));
            }
        }
        for (MethodReference.Handle method : finalDataFlow.keySet()) {
            if (!method.getClassReference().getName().contains(finalPackageName)) {
                continue;
            }
            List<String> interfaces = classMap.get(method.getClassReference()).getInterfaces();
            if (interfaces != null && interfaces.size() != 0) {
                for (String i : interfaces) {
                    ClassReference.Handle inter = new ClassReference.Handle(i);
                    ClassReference superClass = classMap.get(inter);
                    if (superClass == null) {
                        continue;
                    }
                    for (MethodReference m : discoveredMethods) {
                        if (m.getClassReference().equals(inter)
                                && m.getName().equals(method.getName())) {
                            dataFlow.put(m.getHandle(), finalDataFlow.get(method));
                        }
                    }
                }
            }
        }
    }
}
