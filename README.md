# CodeInspector

![](https://img.shields.io/badge/build-passing-brightgreen)
![](https://img.shields.io/badge/ASM-9.0-blue)
![](https://img.shields.io/badge/Java-8-red)

## 简介

简化并重写`GadgetInspector`实现一个针对于`SpringBoot`的**自动Java代码审计工具**

最终目标：通过输入一个`SpringBoot`的`Jar`，直接生成漏洞报告，得到从可控参数的输入点到漏洞的触发点完整信息

注意：

1. 请使用JDK8，不确定其他版本JDK是否稳定运行
2. 请勿中途停止运行，解压分析Jar包会有大量临时文件，正常的结束会删除它们，意外的停止不会删除
3. 在16G内存8核CPU情况下，完整运行一次大约需要3-5分钟

## 原理

基本原理是从`Java`的字节码角度入手，使用`ASM`技术解析，模拟JVM的`Operand Stack`和`Local Variables Array`的交互

其中重点在于逆拓扑排序所有方法调用关系得到每个方法返回值与入参之间的关系，进而实现数据流分析

JVM在每次方法调用均会创建一个对应的Frame，方法执行完毕或者异常终止，Frame被销毁

而每个Frame的结构如下，主要由本地变量数组（local variables）和操作栈（operand stack）组成

局部变量表所需的容量大小是在编译期确定下来的，表中的变量只在当前方法调用中有效

JVM把操作数栈作为它的**工作区**——大多数指令都要从这里弹出数据，执行运算，然后把结果压回操作数栈

参考代码中的`core/CoreMethodAdapter`，该类构造了`Operand Stack`和`Local Variables Array`

在用ASM技术解析class文件的时候，模拟他们在JVM中执行的过程，实现数据流分析

原版数据流分析不支持调用链包含接口的情况，我使用一些特殊的手段做到了跨越接口抽象方法的数据流分析

## 进度

### 解析可控参数

针对SpringMVC解析暂时只考虑这一种（其他的方式做起来不难）

```java
@Controller
public class TestController {
    @RequestMapping(path = "/test")
    @ResponseBody
    public String test(@RequestParam(name = "test") String test) {
        // 其中test认为是可控参数
    }
}
```

### 反射XSS

简单的反射XSS审计，判断根据为同时满足以下三点：

1. 从Controller传入的String型参数认为是可控的
2. 可控参数所能到达的任何地方都不存在实体编码的方法
3. 数据流分析发现Controller层的方法返回值与可控参数相关

检测结果如下

```text
Vuln Name: Reflection XSS
Risk: Low Level
Chains: 
	org/sec/cidemo/web/XSSController.reflection
	org/sec/cidemo/service/XSSService.reflection
	org/sec/cidemo/service/impl/XSSServiceImpl.reflection
	java/lang/String.equals
```

### 存储XSS（beta）

简单的存储XSS审计，判断根据为同时满足以下三点：

1. 从Controller传入的String型参数认为是可控的
2. 可控参数所能到达的任何地方都不存在实体编码的方法（正在实现）
3. 在完整的流程中存在数据库操作（这里只考虑了JdbcTemplate的情况）

```text
---------------------------
Vuln Name: Stored XSS
Risk: High Level
Chains: 
	org/sec/cidemo/web/XSSController.insertMessage
	org/sec/cidemo/service/XSSService.insert
	org/sec/cidemo/service/impl/XSSServiceImpl.insert
	org/sec/cidemo/dao/XSSDao.insert
	org/sec/cidemo/dao/impl/XSSDaoImpl.insert
```

### SSRF

基于JDK的SSRF，判断根据为同时满足以下三点：

1. 从Controller传入的String型参数认为是可控的
2. 可控参数所能到达的任何一处可以检测出JDK原生的`HttpURLConnection`请求过程
3. 这个请求过程必须是有关联的，第一步的输出应该是第二步的输入

检测结果如下

```text
---------------------------
Vuln Name: JDK SSRF
Risk: Middle Level
Chains: 
	org/sec/cidemo/web/SSRFController.ssrf1
	org/sec/cidemo/service/SSRFService.ssrf1
	org/sec/cidemo/service/impl/SSRFServiceImpl.ssrf1
```

## 使用

参考靶机SpringBoot项目：https://github.com/EmYiQing/CIDemo

目前仅测试

打包：`mvn clean package`

执行：`java -jar CodeInspector.jar --boot SpringBoot.jar --pack org.sec.cidemo`

- boot：指定SpringBoot的Jar包路径
- pack：指定项目的包名，将会分析启动的SpringMVC路径映射，生成自动审计的入口

## 关于

主要原理参考GadgetInspector： https://github.com/JackOfMostTrades/gadgetinspector

GadgetInspector大概原理分析：https://xz.aliyun.com/t/10363

该项目实现SSRF的简单分析：https://xz.aliyun.com/t/10433


