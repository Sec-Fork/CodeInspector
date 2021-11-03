package org.sec.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录相关操作的Util
 */
public class DirUtil {
    private static final List<String> filenames = new ArrayList<>();

    /**
     * 对外方法：递归获取指定路径下所有文件
     * @param path 目标路径
     * @return 所有文件名
     */
    public static List<String> GetFiles(String path) {
        filenames.clear();
        return getFiles(path);
    }

    /**
     * 递归遍历目录
     * @param path 目标路径
     * @return 所有文件名
     */
    private static List<String> getFiles(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return filenames;
            }
            for (File value : files) {
                if (value.isDirectory()) {
                    getFiles(value.getPath());
                } else {
                    filenames.add(value.getAbsolutePath());
                }
            }
        } else {
            filenames.add(file.getAbsolutePath());
        }
        return filenames;
    }

    /**
     * 删除某目录
     * @param dir File类型目录
     * @return 是否删除成功
     */
    public static boolean removeDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = removeDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
}
