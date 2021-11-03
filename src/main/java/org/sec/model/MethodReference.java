package org.sec.model;

import java.util.Objects;

/**
 * 方法对象封装
 */
public class MethodReference {
    // 类名关联
    private final ClassReference.Handle classReference;
    // 方法名
    private final String name;
    // 方法描述符
    private final String desc;
    // 是否为静态
    private final boolean isStatic;

    public MethodReference(ClassReference.Handle classReference,
                           String name, String desc, boolean isStatic) {
        this.classReference = classReference;
        this.name = name;
        this.desc = desc;
        this.isStatic = isStatic;
    }

    public ClassReference.Handle getClassReference() {
        return classReference;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Handle getHandle() {
        return new Handle(classReference, name, desc);
    }

    /**
     * 方法名封装
     */
    public static class Handle {
        // 类对象关联
        private final ClassReference.Handle classReference;
        // 方法名
        private final String name;
        // 方法描述
        private final String desc;

        public Handle(ClassReference.Handle classReference, String name, String desc) {
            this.classReference = classReference;
            this.name = name;
            this.desc = desc;
        }

        public ClassReference.Handle getClassReference() {
            return classReference;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        /**
         * 三个条件一致说明相等
         * @param o 对象
         * @return 是否相等
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Handle handle = (Handle) o;
            if (!Objects.equals(classReference, handle.classReference))
                return false;
            if (!Objects.equals(name, handle.name))
                return false;
            return Objects.equals(desc, handle.desc);
        }

        /**
         * 配合equals
         * @return hashcode
         */
        @Override
        public int hashCode() {
            int result = classReference != null ? classReference.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (desc != null ? desc.hashCode() : 0);
            return result;
        }
    }
}
