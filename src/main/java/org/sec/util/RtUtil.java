package org.sec.util;

import org.sec.model.ClassFile;
import com.google.common.reflect.ClassPath;
import org.apache.log4j.Logger;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.*;

/**
 * 这个类的目的是获取所有Class文件
 */
@SuppressWarnings("all")
public class RtUtil {
    private static final Logger logger = Logger.getLogger(RtUtil.class);

    /**
     * 从Jar中获取所有Class文件
     * @param jarPathList jar路径列表
     * @return 封装后的classFile列表
     */
    public static List<ClassFile> getAllClassesFromJars(List<String> jarPathList) {
        logger.info("get all classes");
        Set<ClassFile> classFileSet = new HashSet<>();
        for (String jarPath : jarPathList) {
            classFileSet.addAll(JarUtil.resolveNormalJarFile(jarPath));
        }
        classFileSet.addAll(getRuntimeClasses());
        return new ArrayList<>(classFileSet);
    }

    /**
     * 从SpringBoot的Jar中获取所有的Class文件
     * @param jarPathList SpringBoot的Jar包列表
     * @param useAllLib 是否将SpringBoot的所有lib都解压获取Class文件
     * @return 封装后的classFile列表
     */
    public static List<ClassFile> getAllClassesFromBoot(List<String> jarPathList,
                                                        boolean useAllLib) {
        Set<ClassFile> classFileSet = new HashSet<>();
        for (String jarPath : jarPathList) {
            classFileSet.addAll(JarUtil.resolveSpringBootJarFile(jarPath, useAllLib));
        }
        classFileSet.addAll(getRuntimeClasses());
        return new ArrayList<>(classFileSet);
    }

    /**
     * 获取rt.jar中所有Class文件
     * @return 封装后的classFile列表
     */
    private static List<ClassFile> getRuntimeClasses() {
        try {
            // 从java.lang.String中获取
            URL stringClassUrl = Object.class.getResource("String.class");
            URLConnection connection = null;
            if (stringClassUrl != null) {
                connection = stringClassUrl.openConnection();
            }
            Set<ClassFile> result = new HashSet<>();
            if (connection instanceof JarURLConnection) {
                URL runtimeUrl = ((JarURLConnection) connection).getJarFileURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{runtimeUrl});
                // 基于Guava的ClassPath.from方法获取所有的Class文件输入流
                for (ClassPath.ClassInfo classInfo : ClassPath.from(classLoader).getAllClasses()) {
                    result.add(new ClassFile(classInfo.getResourceName(),
                            classLoader.getResourceAsStream(classInfo.getResourceName())));
                }
            }
            return new ArrayList<>(result);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }
}
