# CodeInspector

![](https://img.shields.io/badge/build-passing-brightgreen)
![](https://img.shields.io/badge/ASM-9.0-blue)
![](https://img.shields.io/badge/Java-8-red)

## 简介

简化并重写`GadgetInspector`尝试实现一个**自动Java代码审计工具**

基本原理是从`Java`的字节码角度入手，使用`ASM`技术解析，模拟JVM的`Operand Stack`和`Local Variables Array`

其中重点在于逆拓扑排序所有方法调用关系得到每个方法返回值与入参之间的关系，进而实现数据流分析

最终目标：通过输入一个`SpringBoot`的`Jar`，直接生成漏洞报告，得到从可控参数的输入点到漏洞的触发点完整信息

参考先知社区文章：

https://xz.aliyun.com/t/10433

https://xz.aliyun.com/t/10363

## 原理

JVM在每次方法调用均会创建一个对应的Frame，方法执行完毕或者异常终止，Frame被销毁

而每个Frame的结构如下，主要由本地变量数组（local variables）和操作栈（operand stack）组成

局部变量表所需的容量大小是在编译期确定下来的，表中的变量只在当前方法调用中有效

JVM把操作数栈作为它的**工作区**——大多数指令都要从这里弹出数据，执行运算，然后把结果压回操作数栈

参考代码中的`core/CoreMethodAdapter`，该类构造了`Operand Stack`和`Local Variables Array`并结合ASM技术实现数据流分析

## 进度

目前仅尝试实现了一种简单的SSRF，但可以做到参数可控性判断和数据流追踪分析，参考已有代码可以实现其他的漏洞检测

## 使用

参考靶机SpringBoot项目：https://github.com/EmYiQing/CIDemo

目前仅测试

打包：`mvn clean package`

执行：`java -jar CodeInspector.jar --boot SpringBoot.jar --pack org.sec.cidemo`

- boot：指定SpringBoot的Jar包路径
- pack：指定项目的包名，将会分析启动的SpringMVC路径映射，生成自动审计的入口
