# ExtProcessor 外部数据处理模块

## 1. 概述

Sensors Analytics 从 1.6 开始为用户开放“外部数据处理模块”，即对 SDK 等方式接入的数据（不包括批量导入工具方式）提供一个简单的 ETL 流程，使数据接入更加灵活。

例如 SDK 发来一条数据内容如下：

```json
{
    "distinct_id":"2b0a6f51a3cd6775",
    "time":1434556935000,
    "type":"track",
    "event":"ViewProduct",
    "properties":{
        "product_name":"苹果"
    }
}
```

这时希望增加一个字段 `product_classify`，表示产品的分类，将数据处理成：

```json
{
    "distinct_id":"2b0a6f51a3cd6775",
    "time":1434556935000,
    "type":"track",
    "event":"ViewProduct",
    "properties":{
        "product_name":"苹果",
        "product_classify":"水果"
    }
}
```

这时就可通过“外部数据处理模块”将数据预处理再导入 Sensors Analytics。

## 2. 开发方法

一个“外部数据处理模块”需要自定义一个 Java 类实现 `com.sensorsdata.analytics.extractor.processor.ExtProcessor` 接口，该接口定义如下：

[ExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/com/sensorsdata/analytics/extractor/processor/ExtProcessor.java) :

```java
package com.sensorsdata.analytics.extractor.processor;

public interface ExtProcessor {
  String process(String record) throws Exception;
}
```

* `参数 String record`: **一条符合 Sensors Analytics 的数据格式定义的 JSON 文本**，例如概述中的第一个 JSON;
* `返回值`: 经过处理后的 JSON 或 JSON 数组，例如概述中的第二个 JSON。其格式也需要符合 Sensors Analytics 的数据格式定义;

本 repo 提供了一个完整的“外部数据处理模块”样例代码，用于实现“概述”中所描述的样例场景，定义接口文件：

[ExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/com/sensorsdata/analytics/extractor/processor/ExtProcessor.java)

实现自定义处理逻辑的类文件：

[SampleExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/cn/sensorsdata/sample/SampleExtProcessor.java)

* 在开发其他项目时，在合适的目录下添加 [ExtProcessor.java](https://github.com/sensorsdata/ext-processor-sample/blob/master/src/main/java/com/sensorsdata/analytics/extractor/processor/ExtProcessor.java) 文件并实现接口即可。

## 3. 编译打包

用于部署的“外部数据处理模块”需要打成一个 Jar 包。

本 repo 附带的样例使用了 Jackson 库解析 JSON，并使用 Maven 做包管理，编译并打包本 repo 代码可通过：

```bash
git clone git@github.com:sensorsdata/ext-processor-sample.git
cd ext-processor-sample
mvn clean package
```

执行编译后可在 `target` 目录下找到 `ext-processor-sample-0.1.jar`。

## 4. 测试 Jar

ext-processor-utils 是用于测试、部署“外部数据处理模块”的工具，只能运行于部署 Sensors Analytics 的机器上。

将编译出的 Jar 文件上传到部署 Sensors Analytics 的机器上，例如 `ext-processor-sample-0.1.jar`。

切换到 sa_cluster 或 sa_standalone 账户，例如切换到 sa_cluster 通过：

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
                     test: 测试 jar 是否可加载;
                     run:  运行指定 class 类的 process 方法, 以标准输入的逐行数据作为参数输入,
                     将返回结果输出到标准输出;
                     install:   安装 ExtProcessor;
                     uninstall: 卸载 ExtProcessor;
                     info:      查看当前配置状态;
```

使用 test 方法测试 Jar 并加载 Class：

```bash
~/sa/extractor/bin/ext-processor-utils \
    --jar ext-processor-sample-0.1.jar \
    --class cn.sensorsdata.sample.SampleExtProcessor \
    --method test
```

* `jar`: Jar 包路径;
* `class`: 实现 `com.sensorsdata.analytics.extractor.processor.ExtProcessor` 的 Java 类;

输出如下：

```
16/10/15 18:27:51 main INFO utils.ExtLibUtils: 加载 jar: /home/sa_cluster/ext-processor-sample-0.1.jar, class: cn.sensorsdata.sample.SampleExtProcessor 成功
```

## 5. 安装

使用 ext-processor-utils 的 install 方法安装，例如安装样例执行如下命令：

```bash
~/sa/extractor/bin/ext-processor-utils \
    --jar ext-processor-sample-0.1.jar \
    --class cn.sensorsdata.sample.SampleExtProcessor \
    --method install
```

* 由于涉及内部模块启停，安装时请耐心等待;

## 6. 卸载

若不再需要“外部数据处理模块”，可以通过 ext-processor-utils 的 uninstall 方法卸载，执行如下命令：

```bash
~/sa/extractor/bin/ext-processor-utils --method uninstall
```

## 7. 其他

* 如果想要抛弃一条数据，`process` 函数直接返回 `null` 即可;
* 如希望返回多条数据，请返回一个 Json 数组，数组中的每个元素为一条符合 Sensors Analytics 的数据;
