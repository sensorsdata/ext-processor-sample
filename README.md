# ExtProcessor 数据预处理模块

## 1. 概述

Sensors Analytics 从 1.6 开始为用户开放自定义“数据预处理模块”，即为 SDK 等方式接入的数据（不包括批量导入工具方式）提供一个简单的 ETL 流程，使数据接入更加灵活。

可以使用“数据预处理模块”处理的数据来源包括：

* SDK（各语言 SDK 直接发送的数据）;
* LogAgent;
* FormatImporter;

使用 BatchImporter 和 HdfsImporter 批量导入数据的情况除外。

例如 SDK 发来一条数据，传入“数据预处理模块”时格式如下：

```json
{
    "distinct_id":"2b0a6f51a3cd6775",
    "time":1434556935000,
    "type":"track",
    "event":"ViewProduct",
    "project": "default",
    "ip":"123.123.123.123",
    "properties":{
        "product_name":"苹果"
    }
}
```

这时希望增加一个字段 `product_classify`，表示产品的分类，可通过“数据预处理模块”将数据处理成：

```json
{
    "distinct_id":"2b0a6f51a3cd6775",
    "time":1434556935000,
    "type":"track",
    "event":"ViewProduct",
    "project": "default",
    "properties":{
        "product_name":"苹果",
        "product_classify":"水果"
    }
}
```

## 2. 开发方法

一个“数据预处理模块”需要自定义一个 Java 类实现 `com.sensorsdata.analytics.extractor.processor.ExtProcessor` 接口，该接口定义如下：

[ExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/com/sensorsdata/analytics/extractor/processor/ExtProcessor.java) :

```java
package com.sensorsdata.analytics.extractor.processor;

public interface ExtProcessor {
  String process(String record) throws Exception;
}
```

* 参数: 一条符合 [Sensors Analytics 的数据格式定义](https://www.sensorsdata.cn/manual/data_schema.html)的 JSON 文本，例如概述中的第一个 JSON。与 [数据格式](https://www.sensorsdata.cn/manual/data_schema.html) 唯一区别在于数据中包含字段 `ip` ，值为接收数据时取到的客户端 IP;
* 返回值: 经过处理后的 JSON 或 JSON 数组，例如概述中的第二个 JSON。其格式需要符合 [Sensors Analytics 的数据格式定义](https://www.sensorsdata.cn/manual/data_schema.html); 如果返回值包含多条数据，可返回一个 JSON 数组，数组中的每个元素为一条符合 [数据格式](https://www.sensorsdata.cn/manual/data_schema.html) 的数据; 若返回值为 `null`，表示抛弃这条数据;
* 异常: 抛出异常将导致这条数据被抛弃并输出错误日志;

本 repo 提供了一个完整的“数据预处理模块”样例代码，用于实现“概述”中所描述的样例场景，定义接口文件：

[ExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/com/sensorsdata/analytics/extractor/processor/ExtProcessor.java)

实现自定义处理逻辑的类文件：

[SampleExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/cn/sensorsdata/sample/SampleExtProcessor.java)

* 在开发其他项目时，在合适的目录下添加 [ExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/com/sensorsdata/analytics/extractor/processor/ExtProcessor.java) 文件并实现接口即可。

## 2.1 开发常见问题

* 如果使用了 log4j (或 slf4j) 日志库，日志将默认输出到 `/data/sa_cluster/logs/extractor` (其中 `/data` 为数据盘挂载点) 下的 `extractor.log` 中;
* 如果想要抛弃一条数据，`process` 函数直接返回 `null` 即可;
* 如希望一次处理返回多条数据(例如一条传入数据输出多条数据，或传入多条数据批处理再全部输出)，请返回一个 JSON 数组，数组中的每个元素都为符合 [Sensors Analytics 的数据格式定义](https://www.sensorsdata.cn/manual/data_schema.html) 的数据:
  ```json
  [
      {
          "distinct_id":"2b0a6f51a3cd6775",
          "time":1434556935000,
          "type":"track",
          "event":"ViewProduct",
          "project": "sample_project",
          "properties":{
              ...
          }
      },
      {
          "distinct_id":"2b0a6f51a3cd6775",
          "type":"profile_set",
          "time":1434556935000,
          "project": "sample_project",
          "properties":{
              "is_vip":true
          }
      }
  ]
  ```
* 请注意**空指针的问题**，比如某个需要处理的 `property` 不是每条数据都存在，如果不存在时取值并使用可能造成空指针异常，如果不在处理模块内部处理该异常直接抛出，将导致这条数据被抛弃;
* 请注意用户属性数据即 `type` 以 `profile_` 开头的数据，是没有 `event` 字段的，若用到 `event` 字段，请先判断字段是否存在;
* 请用尽量多的判断以确定一条数据是否是你希望修改的数据再做操作;
* 一条数据若不需要修改直接返回原文本即可;
* 一般情况下，Sensors Analytics 每台机器实时导入速度最高可以达到约每秒 5k ~ 20k 条（受数据字段数、机器性能等影响而不同），若使用“数据预处理模块”可能带来额外的性能开销，建议使用前对“数据预处理模块”性能进行评估，更多对性能的影响请参考 [性能](https://github.com/sensorsdata/ext-processor-sample#7-%e6%80%a7%e8%83%bd);
* 极端情况下（如模块重启）同一条数据可能被“数据预处理模块”多次处理。若使用“数据预处理模块”的目的如本 repo 仅添加字段，那么多次处理没有影响，但若是在“数据预处理模块”中做统计等操作（不建议这样做，统计需求建议通过订阅 kafka 数据实现），则需考虑重复执行的影响;
* 导入模块通过反射实例化用户指定的预处理类，并通过接口进行访问。不建议在“数据预处理模块”中包含复杂逻辑，此部分的问题需相关开发人员自行调试;
* 预处理类在一个 JVM 内可能被实例化多次，对于一个实例不会被多线程同时访问，但多个实例有可能被同时访问;

## 3. 编译打包

用于部署的“数据预处理模块”需要打成一个 JAR 包。

本 repo 附带的样例使用了 Jackson 库解析 JSON，并使用 Maven 做包管理，编译并打包本 repo 代码可通过：

```bash
git clone git@github.com:sensorsdata/ext-processor-sample.git
cd ext-processor-sample
mvn clean package
```

执行编译后可在 `target` 目录下找到 `ext-processor-sample-0.1.jar`。

## 4. 测试 JAR

ext-processor-utils 是用于测试、部署“数据预处理模块”的工具，只能运行于部署 Sensors Analytics 的机器上。

将编译出的 JAR 文件上传到部署 Sensors Analytics 的机器上，例如 `ext-processor-sample-0.1.jar`。

切换到 `sa_cluster` 或 `sa_standalone` 账户，例如切换到 `sa_cluster` 通过：

```bash
sudo su - sa_cluster
```

直接运行 ext-processor-utils 将输出参数列表如：

```
~/sa/extractor/bin/ext-processor-utils

usage: [ext-processor-utils] [-c <arg>] [-h] [-j <arg>] -m <arg>
 -c,--class <arg>    实现 ExtProcessor 的类名, 例如 cn.kbyte.CustomProcessor
 -h,--help           help
 -j,--jar <arg>      包含 ExtProcessor 的 jar, 例如 custom-processor-0.1.jar
 -m,--method <arg>   操作类型, 可选 test/run/install/uninstall/info
                     test:      测试 jar 是否可加载;
                     install:   安装 ExtProcessor;
                     uninstall: 卸载 ExtProcessor;
                     info:      查看当前配置状态;
                     run:       运行指定 class 类的 process 方法, 以标准输入的逐行数据作为参数输入,
                                将返回结果输出到标准输出;
                     run_with_real_time_data: 使用本机实时的数据作为输入,
                                              将返回结果输出到标准输出;
    --when_exception_use_original <arg>   当 ExtProcessor 抛异常时导入原始数据而不是直接抛弃,
                                          yes 表示预处理遇到异常时使用原始数据导入,
                                          no 表示遇到异常时抛弃该条数据;
```

使用 `test` 方法测试 JAR 并加载 Class：

```bash
~/sa/extractor/bin/ext-processor-utils \
    --jar ext-processor-sample-0.1.jar \
    --class cn.sensorsdata.sample.SampleExtProcessor \
    --method test
```

* `jar`: JAR 包路径;
* `class`: 实现 `com.sensorsdata.analytics.extractor.processor.ExtProcessor` 的 Java 类;

输出如下：

```
16/10/15 18:27:51 main INFO utils.ExtLibUtils: 加载 jar: /home/sa_cluster/ext-processor-sample-0.1.jar, class: cn.sensorsdata.sample.SampleExtProcessor 成功
```

### 4.1 测试运行

使用 `run` 方法加载 JAR 并实例化 Class，以标准输入的逐行数据作为预处理函数输入，并将处理结果输出到标准输出:

```bash
~/sa/extractor/bin/ext-processor-utils \
    --jar ext-processor-sample-0.1.jar \
    --class cn.sensorsdata.sample.SampleExtProcessor \
    --method run
```

### 4.2 以线上实时数据测试运行

使用 `run_with_real_time_data` 方法加载 JAR 并实例化 Class，以本机实际接收的数据作为预处理函数输入，并将输入和输出打印到标准输出:

```bash
~/sa/extractor/bin/ext-processor-utils \
    --jar ext-processor-sample-0.1.jar \
    --class cn.sensorsdata.sample.SampleExtProcessor \
    --method run_with_real_time_data \
    --when_exception_use_original no
```

## 5. 安装

使用 ext-processor-utils 的 `install` 方法安装，例如安装样例执行如下命令：

```bash
~/sa/extractor/bin/ext-processor-utils \
    --jar ext-processor-sample-0.1.jar \
    --class cn.sensorsdata.sample.SampleExtProcessor \
    --method install \
    --when_exception_use_original yes
```

* 由于涉及内部模块启停，安装时请耐心等待;
* 集群版安装预处理模块会自动分发，不需要每台机器操作;
* 若已经安装过“数据预处理模块”，再次执行“安装”操作将替换使用新的 JAR 包;
* when_exception_use_original 设置为 yes 可以避免未考虑到的空指针异常使数据无法导入，但副作用是这条数据没有经过预处理，可能不符合预期而无法用于查询甚至产生脏数据;

## 6. 验证

安装好“数据预处理模块”后，为了验证处理结果是否符合预期，可以开启 SDK 的 [`Debug 模式`](https://www.sensorsdata.cn/manual/debug_mode.html) 校验数据。

1. 使用管理员帐号登录 Sensors Analytics 界面，点击左下角 `埋点`，在新页面中点击右上角 `数据接入辅助工具`，在新页面中点击最上面导航栏中的 `DEBUG数据查看`;
2. 配置 SDK 使用 [`Debug 模式`](https://www.sensorsdata.cn/manual/debug_mode.html);
3. 发送一条测试用的数据，观察是否进行了预期处理即可;

## 7. 性能

执行命令输出导入模块的统计信息：

```bash
sa_admin status -m extractor
```

其中 extProcessorBottleneck 为当前数据预处理模块的性能瓶颈，其计算方法如下：

```java
/** 初始化统计 **/
processUseTime = 0;   // 用于统计执行预处理所用时间
processCount = 0;     // 用于统计调用预处理次数

/** 每次预处理 **/
start = System.nanoTime();                   // 记录执行预处理前的时间戳
record = serializeToJson(rawRecord);         // 将数据序列化成 JSON 格式
extProcessor.process(record);                // 调用预处理函数
processUseTime += System.nanoTime() - start; // 累加本次执行预处理消耗的时间到总和
++processCount;                              // 累加执行次数

/** 每 1 分钟计算上 1 分钟统计值，并清零计数 **/
// 得出每秒最多执行 process() 次数，即此处的性能瓶颈
extProcessorBottleneck = processCount * 1000000000L / processUseTime;
processUseTime = 0;
processCount = 0;
```

1. 此处计算出来的值 extProcessorBottleneck 相当于仅执行预处理每秒最多多少次，由于导入还有很多其他数据处理步骤，故总导入速度将小于该值;
2. 导入模块分配的内存有限，若预处理需要消耗较多内存，请提前联系我们调大内存参数，若过多将影响其他模块运行;

## 8. 卸载

若不再需要“数据预处理模块”，可以通过 ext-processor-utils 的 `uninstall` 方法卸载，执行如下命令：

```bash
~/sa/extractor/bin/ext-processor-utils --method uninstall
```

* 若希望更新 JAR 包，请直接使用工具“安装”新的 JAR 包即可，不需要先进行卸载;
