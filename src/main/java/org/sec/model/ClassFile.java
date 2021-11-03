package org.sec.model;

import org.sec.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 封装了类资源
 */
public class ClassFile {
    // 类名
    private final String className;
    // 对于的输入流
    private InputStream inputStream;

    public ClassFile(String className, InputStream inputStream) {
        this.className = className;
        this.inputStream = inputStream;
    }

    /**
     * 两个对象判断相等的依据是类名
     * @param o 对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassFile classFile = (ClassFile) o;
        return Objects.equals(className, classFile.className);
    }

    /**
     * 和equals配套
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return className != null ? className.hashCode() : 0;
    }

    public String getClassName() {
        return className;
    }

    /**
     * 获取输入流
     * 注意：必须要以复制的方式处理否则只能够读一次
     * @return 输入流
     */
    public InputStream getInputStream() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtil.copy(inputStream, outputStream);
        // 确保对象的输入流还是可以读的
        this.inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 简单的关闭流方法
     */
    public void close() {
        try {
            this.inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}