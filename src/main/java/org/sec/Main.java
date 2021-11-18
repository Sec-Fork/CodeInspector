package org.sec;

import com.beust.jcommander.JCommander;
import org.sec.config.Command;
import org.sec.config.Logo;
import org.sec.model.*;
import org.sec.util.DataUtil;
import org.sec.util.OutputUtil;
import org.sec.util.RtUtil;
import org.apache.log4j.Logger;
import org.sec.core.CallGraph;
import org.sec.core.InheritanceMap;
import org.sec.service.*;

import java.util.*;

//                          _ooOoo_
//                         o8888888o
//                         88" . "88
//                         (| -_- |)
//                          O\ = /O
//                      ____/`---'\____
//                    .   ' \\| |// `.
//                     / \\||| : |||// \
//                   / _||||| -:- |||||- \
//                     | | \\\ - /// | |
//                   | \_| ''\---/'' | |
//                    \ .-\__ `-` ___/-. /
//                 ___`. .' /--.--\ `. . __
//              ."" '< `.___\_<|>_/___.' >'"".
//             | | : `- \`.;`\ _ /`;.`/ - ` : | |
//               \ \ `-. \_ __\ /__ _/ .-` / /
//       ======`-.____`-.___\_____/___.-`____.-'======
//                          `=---='
//
//       .............................................
//                佛祖保佑           永无BUG
//
//                写字楼里写字间，写字间里程序员；
//                程序人员写程序，又拿程序换酒钱。
//                酒醒只在网上坐，酒醉还来网下眠；
//                酒醉酒醒日复日，网上网下年复年。
//                但愿老死电脑间，不愿鞠躬老板前；
//                奔驰宝马贵者趣，公交自行程序员。
//                别人笑我忒疯癫，我笑自己命太贱；
//                不见满街漂亮妹，哪个归得程序员？
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);
    // 所有类
    private static final List<ClassReference> discoveredClasses = new ArrayList<>();
    // 所有方法
    private static final List<MethodReference> discoveredMethods = new ArrayList<>();
    // 所有方法内的方法调用
    private static final Map<MethodReference.Handle, Set<MethodReference.Handle>> methodCalls = new HashMap<>();
    // 方法返回值与哪些参数有关
    private static final Map<MethodReference.Handle, Set<Integer>> dataFlow = new HashMap<>();
    // 所有的方法调用关系
    private static final Set<CallGraph> discoveredCalls = new HashSet<>();
    // 类名->类对象
    private static final Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
    // 方法名->方法对象
    private static final Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
    // 类名->类资源
    private static final Map<String, ClassFile> classFileByName = new HashMap<>();
    // 方法名->方法调用关系
    private static final Map<MethodReference.Handle, Set<CallGraph>> graphCallMap = new HashMap<>();

    public static void main(String[] args) {
        // 打印Logo
        Logo.PrintLogo();
        logger.info("start code inspector");
        // 使用JCommander处理命令参数
        Command command = new Command();
        JCommander jc = JCommander.newBuilder().addObject(command).build();
        jc.parse(args);
        if (command.help) {
            jc.usage();
        }
        // 暂时只处理输入SpringBoot的Jar包情况
        // 其实Tomcat的War包情况类似暂不处理
        if (command.boots != null && command.boots.size() != 0) {
            start(command);
        }
    }

    private static void start(Command command) {
        List<String> boots = command.boots;
        String packageName = command.packageName;
        boolean draw = command.drawCallGraph;

        // 读取JDK和输入Jar所有class资源
        List<ClassFile> classFileList = RtUtil.getAllClassesFromBoot(boots, true);
        // 获取所有方法和类
        DiscoveryService.start(classFileList, discoveredClasses, discoveredMethods);
        // 根据已有方法和类得到继承关系
        InheritanceMap inheritanceMap = InheritanceService.start(discoveredClasses, discoveredMethods,
                classMap, methodMap);
        // 包名
        String finalPackageName = packageName.replace(".", "/");
        // 获取全部controller
        List<SpringController> controllers = new ArrayList<>();
        // 根据SpringMVC的规则得到相关信息
        SpringService.start(classFileList, finalPackageName, controllers, classMap, methodMap);
        // 得到方法中的方法调用
        MethodCallService.start(classFileList, methodCalls, classFileByName);
        // 对方法进行拓扑逆排序
        List<MethodReference.Handle> sortedMethods = SortService.start(methodCalls);
        // 分析方法返回值与哪些参数有关
        DataFlowService.start(inheritanceMap, sortedMethods, classFileByName, classMap, dataFlow);
        DataUtil.SaveDataFlows(dataFlow, methodMap, finalPackageName);
        // 根据已有条件得到方法调用关系
        CallGraphService.start(inheritanceMap, discoveredCalls, sortedMethods, classFileByName,
                classMap, dataFlow, graphCallMap, methodMap);
        // 保存到本地观察
        DataUtil.SaveCallGraphs(discoveredCalls, finalPackageName);
        // SSRF检测
        SSRFService.start(classFileByName, controllers, inheritanceMap, dataFlow, graphCallMap);
        List<ResultInfo> results = SSRFService.getResults();
        OutputUtil.doOutput(results);
        if (draw) {
            DrawService.start(discoveredCalls, finalPackageName, classMap);
        }
    }
}
