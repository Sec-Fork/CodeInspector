package org.sec.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 类对象封装
 */
public class ClassReference {
    // 类名
    private final String name;
    // 父类名
    private final String superClass;
    // 实现的接口名
    private final List<String> interfaces;
    // 是否是接口
    private final boolean isInterface;
    // 拥有哪些属性
    private final List<Member> members;
    // 有哪些注解
    private final Set<String> annotations;

    /**
     * 属性封装
     */
    public static class Member {
        // 属性
        private final String name;
        // 属性描述符(public static等)
        private final int modifiers;
        // 与本类的关联
        private final Handle type;

        public Member(String name, int modifiers, Handle type) {
            this.name = name;
            this.modifiers = modifiers;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public int getModifiers() {
            return modifiers;
        }

        public Handle getType() {
            return type;
        }
    }

    public ClassReference(String name, String superClass, List<String> interfaces,
                          boolean isInterface, List<Member> members, Set<String> annotations) {
        this.name = name;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.isInterface = isInterface;
        this.members = members;
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public String getSuperClass() {
        return superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public List<Member> getMembers() {
        return members;
    }

    public Handle getHandle() {
        return new Handle(name);
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    /**
     * 类名的封装
     */
    public static class Handle {
        // 类名
        private final String name;

        public Handle(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * 两个类名相等的条件是handle.name一致
         * @param o 对象
         * @return 是否相等
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Handle handle = (Handle) o;
            return Objects.equals(name, handle.name);
        }

        /**
         * 配合equals
         * @return hashcode
         */
        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
