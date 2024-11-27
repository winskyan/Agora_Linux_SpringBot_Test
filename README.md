# Agora Linux Java SDK 演示项目

本项目展示了如何在 Spring Boot 应用中使用 Agora Linux Java SDK。

## 创建 Spring Boot 工程

首先，使用以下 Maven 命令创建一个空的 Spring Boot 工程：

```
mvn archetype:generate -DgroupId=com.example -DartifactId=demo -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false
```

这个命令会创建一个基本的 Maven 项目结构。之后，您需要修改 `pom.xml` 文件以添加 Spring Boot 依赖和插件。

## 前置要求

- Java JDK（21 版本或更高）
- Maven
- Linux 环境

## 设置和安装

1. 解压 Agora SDK JAR 文件：

   ```
   jar xvf Agora-Linux-Java-SDK-2.0-440-20240925_102541-a586711f5c.jar
   ```

2. 将本地 JAR 安装到 Maven 本地仓库：

   ```
   mvn install:install-file -Dfile=lib/agora-sdk.jar -DgroupId=io.agora.rtc -DartifactId=linux-java-sdk -Dversion=4.0.1.5 -Dpackaging=jar
   ```

3. 构建项目：

   ```
   mvn clean package
   ```

## 运行应用

### 本地jar运行

使用以下命令运行应用：

```
LD_LIBRARY_PATH="$LD_LIBRARY_PATH:lib/native/linux/x86_64" java -Dserver.port=18080 -jar target/demo-0.0.1-SNAPSHOT.jar
```

此命令执行以下操作：

- 设置必要的库路径以包含原生库
- 配置应用在 18080 端口上运行
- 运行 Spring Boot 应用 JAR 文件


要启动一个房间，使用以下 API 端点：

```
http://10.200.0.206:18080/api/start?roomId=aga
```

将 `aga` 替换为您想要的房间 ID。

### tomcat运行

```
sudo cp -f target/agora-demo.war /opt/tomcat/webapps/
sudo cp -f target/agora-demo.war /opt/tomcat8/webapps/

sudo /opt/tomcat/bin/catalina.sh run
sudo /opt/tomcat8/bin/catalina.sh run

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/yanzhennan/Agora_Linux_SpringBot_Test/lib/native/linux/x86_64
export JAVA_OPTS="$JAVA_OPTS -Djava.library.path=/home/yanzhennan/Agora_Linux_SpringBot_Test/lib/native/linux/x86_64"
```

要启动一个房间，使用以下 API 端点：
```
http://10.200.0.25:8080/agora-demo/api2/start?roomId=aga
```

## 停止运行

```
 sudo lsof -i :18080
 sudo lsof -i :8080
 sudo kill -9 <PID>
```

## 注意事项

- 确保 `lib/native/linux/x86_64` 目录包含所有必要的原生库。
- 应用默认在 18080 端口上运行。您可以通过修改 `-Dserver.port` 参数来更改端口。
- 如果您的服务器 IP 不同，请确保更新 API 使用示例中的 IP 地址。

## 故障排除

如果遇到任何问题：

- 验证所有依赖项是否正确安装
- 检查 Agora SDK JAR 是否正确解压和安装
- 确保原生库位于正确位置且可访问

## 其他资源

- [Agora.io 文档](https://docs.agora.io/cn/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
