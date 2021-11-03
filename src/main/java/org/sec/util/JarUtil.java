package org.sec.util;

import org.sec.model.ClassFile;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * 和RtUtil配合实现Jar解压读取Class
 */
@SuppressWarnings("all")
public class JarUtil {
    private static final Logger logger = Logger.getLogger(JarUtil.class);
    private static final Set<ClassFile> classFileSet = new HashSet<>();

    /**
     * 处理SpringBoot的Jar包
     * @param jarPath Jar包的路径
     * @param useAllLib 是否处理所有的lib
     * @return 封装后的classFile列表
     */
    public static List<ClassFile> resolveSpringBootJarFile(String jarPath,boolean useAllLib) {
        try {
            // 创建一个随机命名的临时目录
            final Path tmpDir = Files.createTempDirectory(UUID.randomUUID().toString());
            // 添加一个Hook让JVM退出时删除目录
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // 注意要先关闭所有的Class流
                // 否则占用无法删除目录
                closeAll();
                // 删除所有目录
                DirUtil.removeDir(tmpDir.toFile());
            }));
            // 解压
            resolve(jarPath, tmpDir);
            // 解压lib
            resolveBoot(jarPath, tmpDir);
            // 如果需要处理所有lib
            if(useAllLib){
                // 对BOOT-INF/lib中所有库都进行Jar解压
                Files.list(tmpDir.resolve("BOOT-INF/lib")).forEach(p -> {
                    resolveNormalJarFile(p.toFile().getAbsolutePath());
                });
            }
            return new ArrayList<>(classFileSet);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }

    /**
     * 处理普通Jar包
     * @param jarPath Jar包路径
     * @return 封装后的classFile列表
     */
    public static List<ClassFile> resolveNormalJarFile(String jarPath) {
        try {
            // 临时目录
            final Path tmpDir = Files.createTempDirectory(UUID.randomUUID().toString());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                closeAll();
                DirUtil.removeDir(tmpDir.toFile());
            }));
            // 解压
            resolve(jarPath, tmpDir);
            return new ArrayList<>(classFileSet);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }

    /**
     * 解压Jar包
     * @param jarPath Jar路径
     * @param tmpDir 临时路径
     */
    private static void resolve(String jarPath, Path tmpDir) {
        try {
            // 解压Jar包
            InputStream is = new FileInputStream(jarPath);
            JarInputStream jarInputStream = new JarInputStream(is);
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                Path fullPath = tmpDir.resolve(jarEntry.getName());
                if (!jarEntry.isDirectory()) {
                    if (!jarEntry.getName().endsWith(".class")) {
                        continue;
                    }
                    Path dirName = fullPath.getParent();
                    if (!Files.exists(dirName)) {
                        // 创建目录
                        Files.createDirectories(dirName);
                    }
                    // 通过复制流将Jar包所有Class文件复制到临时目录
                    OutputStream outputStream = Files.newOutputStream(fullPath);
                    IOUtil.copy(jarInputStream, outputStream);
                    // 封装ClassFile
                    InputStream fis = new FileInputStream(fullPath.toFile());
                    ClassFile classFile = new ClassFile(jarEntry.getName(), fis);
                    classFileSet.add(classFile);
                }
            }
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }

    /**
     * 解压SpringBoot的Jar包
     * @param jarPath Jar路径
     * @param tmpDir 临时目录
     */
    private static void resolveBoot(String jarPath, Path tmpDir) {
        try {
            InputStream is = new FileInputStream(jarPath);
            JarInputStream jarInputStream = new JarInputStream(is);
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                Path fullPath = tmpDir.resolve(jarEntry.getName());
                // 不同于普通Jar包
                // SpringBootFatJar中包含其他Jar
                if (!jarEntry.isDirectory()) {
                    if (!jarEntry.getName().endsWith(".jar")) {
                        continue;
                    }
                    Path dirName = fullPath.getParent();
                    if (!Files.exists(dirName)) {
                        Files.createDirectories(dirName);
                    }
                    // 这里是把其他Jar复制到临时目录
                    OutputStream outputStream = Files.newOutputStream(fullPath);
                    IOUtil.copy(jarInputStream, outputStream);
                }
            }
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }

    /**
     * 关闭所有流
     */
    private static void closeAll() {
        // 删除目录前关闭流防止占用
        List<ClassFile> classFileList = new ArrayList<>(classFileSet);
        for (ClassFile classFile : classFileList) {
            classFile.close();
        }
    }
}