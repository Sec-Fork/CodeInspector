package org.sec.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 文件相关操作的Util
 */
public class FileUtil {
    private static final Logger logger = Logger.getLogger(FileUtil.class);

    /**
     * 通过输入流读文件
     * @param is 输入流
     * @return 字符串格式数据
     */
    public static String readFile(InputStream is) {
        return new String(doReadFile(is), StandardCharsets.UTF_8);
    }

    /**
     * 通过输入流读文件
     * @param is 输入流
     * @return 字节格式数据
     */
    public static byte[] readFileBytes(InputStream is) {
        return doReadFile(is);
    }

    /**
     * 通过文件名读文件
     * @param filename 字符串文件名
     * @return 字符串数据
     */
    public static String readFile(String filename) {
        try {
            InputStream r = new FileInputStream(filename);
            return new String(doReadFile(r), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return "";
    }

    /**
     * 通过输入流读文件
     * @param is 输入流
     * @return 字节数据
     */
    private static byte[] doReadFile(InputStream is) {
        try {
            ByteArrayOutputStream byteData = new ByteArrayOutputStream();
            byte[] temp = new byte[1024 * 4];
            byte[] context;
            int i;
            while ((i = is.read(temp)) > 0) {
                byteData.write(temp, 0, i);
            }
            context = byteData.toByteArray();
            is.close();
            byteData.close();
            return context;
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new byte[0];
    }

    /**
     * 写文件
     * @param filename 字符串文件名
     * @param output 字符串数据
     */
    public static void writeFile(String filename, String output) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                if (file.createNewFile()) {
                    logger.debug("create new output file");
                }
            }
            BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(file),
                            StandardCharsets.UTF_8));
            bw.write(output);
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得文件后缀
     * @param filename 完整文件名
     * @return 后缀名
     */
    public static String getFileExt(String filename) {
        String[] splits = filename.split("\\.");
        return splits[splits.length - 1];
    }
}
