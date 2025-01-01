## **项目基本情况**

基于 **Java Socket API** 实现的简单 HTTP 客户端与服务器端应用

---

### **1. 基本功能**

#### **服务器端功能**

- 支持处理简单的 **GET** 和 **POST** 请求
- 支持简单的 **200**，**301**，**302**，**304**，**400**，**401**，**403**，**404**，**409**，**500**等状态码响应
- 实现**长连接**
- 支持**重定向规则**

####  **客户端功能**

- 支持发送 **GET** 和 **POST** 请求
- 处理 **301**、**302**、**304** 等重定向状态码
- 支持**长连接**和**缓存控制**

####  **配置管理**

- 支持使用 **JSON文件**进行配置。



### **2. 框架**

- **编程语言**：Java 1.8
- **网络通信**：Java Socket API
- **构建工具**：Maven

| **项目依赖**                               |
| ------------------------------------------ |
| **org.projectlombok:lombok**               |
| **org.json:json**                          |
| **org.junit.jupiter:junit-jupiter-api**    |
| **org.junit.jupiter:junit-jupiter-engine** |
| **maven-compiler-plugin**                  |
| **maven-assembly-plugin**                  |
| **maven-surefire-plugin**                  |



---



## 构建运行

### 服务端

```
// 构建
mvn clean package -Pbuild-server // 输出 target/HTTP-Server-jar-with-dependencies.jar
// 运行
java -jar HTTP-Server-jar-with-dependencies.jar
```

### 客户端

```
// 构建
mvn clean package -Pbuild-client // 输出 target/HTTP-Client-jar-with-dependencies.jar
// 运行
java -jar HTTP-Client-jar-with-dependencies.jar
```

```
send <host>:<port>[/<path>][?<query>] [-m <method>] [-h <header>:<value> ...] [-b "<body>"]
disconnect <host>:<port>
stop
exit
help
```



---



## 项目结构

```
├─ src/
│  └─ main/
│     ├─ java/
│     │   └─ edu.nju.http/
│     │      ├─ client/                // 客户端
│     │      │  ├─ Cache.java          // 缓存管理
│     │      │  ├─ ClientDriver.java   // 客户端交互类
│     │      │  ├─ Config.java         // 客户端配置类
│     │      │  └─ HttpClient.java     // HTTP 客户端
│     │      │
│     │      ├─ message/               // HTTP 消息
│     │      │  ├─ constant/
│     │      │  │  ├─ Header.java      // HTTP 头部字段常量
│     │      │  │  ├─ Method.java      // HTTP 方法常量
│     │      │  │  ├─ Status.java      // HTTP 状态码常量
│     │      │  │  └─ Version.java     // HTTP 版本常量
│     │      │  ├─ HttpMessage.java    // HTTP 消息基类
│     │      │  ├─ HttpRequest.java    // HTTP 请求类
│     │      │  ├─ HttpResponse.java   // HTTP 响应类
│     │      │  └─ MIME.java           // MIME 类型管理
│     │      │
│     │      ├─ server/                // 服务端
│     │      │  ├─ Config.java         // 服务端配置类
│     │      │  ├─ HttpServer.java     // HTTP 服务器
│     │      │  ├─ ResponseBuilder.java// HTTP 响应构建类
│     │      │  ├─ ServerHandler.java  // HTTP 请求处理类
│     │      │  └─ UserSystem.java     // 简单的用户系统
│     │      │
│     │      └─ utils/                 // 工具类
│     │         ├─ Log.java            // 日志工具
│     │         └─ Searcher.java       // 资源搜索工具
│     │
│     ├─ resources/                    // 内部资源目录（优先级低于外部资源）
│         ├─ config/                   // 内部配置文件
│         │  └─ config.json            // 默认配置文件，当外部配置不存在时使用
│         │
│         └─ static/                   // 内部静态资源
|
├─ config/                             // 外部配置文件目录，若没有则启动时会在工作目录下生成
│  └─ config.json 
│
├─ resources/                          // 外部资源文件目录，若没有则启动时会在工作目录下生成
│  └─ static/                          // 外部静态资源
│
├─ data/                               // 数据存储目录
│
├─ .gitignore                          
├─ pom.xml                            
└─ README.md                           


```



---



## 配置文件说明

#### 示例

```json
{
  "client": {
    "client_name": "SimpleHttpClient",
    "client_version": "1.0",
    "keep_alive": true,
    "connection_timeout": 5000,
    "buffer_size": 2048,
    "enable_cache": true,
    "cache_max_age": 3600,
    "cache_control": "public, max-age=3600",
    "log_level": 1,
    "log_dir": "logs",
    "data_dir": "data",
    "max_display_size": 1024
  },
  "server": {
    "server_name": "SimpleHttpServer",
    "server_version": "1.0",
    "host": "localhost",
    "port": 8080,
    "keep_alive": true,
    "timeout": 5000,
    "thread_pool": false,
    "max_threads": 8,
    "max_connections": 1000,
    "buffer_size": 2048,
    "session_expiry_time": 3600,
    "enable_cache": true,
    "cache_control": "public,max-age=3600",
    "default_page": "index.html",
    "default_encoding": "UTF-8",
    "static_resource_dir": "static",
    "user_path": "user",
    "data_dir": "data",
    "log_dir": "data/log",
    "log_level": 1,
    "redirects": [
      {
        "path": "/old-path",
        "target": "/new-path",
        "status": 301
      },
      {
        "path": "/",
        "target": "http://xxx.xxx.xxx.xxx:xxx/xxx",
        "status": 302
      },
      {
         "path": "/google",
         "target": "http://www.google.com",
         "status": 302
      }
    ]
  }
}

```



#### client

| **字段**             | **类型** | **含义**                              | **默认值**          |
| -------------------- | -------- | ------------------------------------- | ------------------- |
| `client_name`        | String   | 客户端名称                            | SimpleHttpClient    |
| `client_version`     | String   | 客户端版本                            | 1.0                 |
| `keep_alive`         | Boolean  | 是否启用长连接                        | true                |
| `connection_timeout` | Integer  | 连接超时时间（毫秒）                  | 5000                |
| `buffer_size`        | Integer  | 缓冲区大小（字节）                    | 2048                |
| `enable_cache`       | Boolean  | 是否启用缓存                          | true                |
| `cache_max_age`      | Integer  | 缓存最大有效时间（秒）                | 3600                |
| `cache_control`      | String   | 缓存控制策略                          | public,max-age=3600 |
| `log_level`          | Integer  | 日志级别（0: 关闭, 1: 信息, 2: 调试） | 1                   |
| `log_dir`            | String   | 日志存储目录                          | logs                |
| `data_dir`           | String   | 数据存储目录                          | data                |
| `max_display_size`   | Integer  | 命令行界面消息体展示限制              | 1024                |



#### server

| **字段**              | **类型** | **含义**                              | **默认值**          |
| --------------------- | -------- | ------------------------------------- | ------------------- |
| `server_name`         | String   | 服务器名称                            | SimpleHttpServer    |
| `server_version`      | String   | 服务器版本                            | 1.0                 |
| `host`                | String   | 服务器主机地址                        | localhost           |
| `port`                | Integer  | 服务器监听端口                        | 8080                |
| `keep_alive`          | Boolean  | 是否启用长连接                        | true                |
| `timeout`             | Integer  | 超时时间（毫秒）                      | 5000                |
| `thread_pool`         | Boolean  | 是否启用线程池                        | false               |
| `max_threads`         | Integer  | 最大线程数                            | 8                   |
| `max_connections`     | Integer  | 最大连接数                            | 1000                |
| `buffer_size`         | Integer  | 缓冲区大小（字节）                    | 2048                |
| `session_expiry_time` | Integer  | 会话过期时间（秒）                    | 3600                |
| `enable_cache`        | Boolean  | 是否启用缓存                          | true                |
| `cache_control`       | String   | 缓存控制策略                          | public,max-age=3600 |
| `default_page`        | String   | 默认首页文件名                        | index.html          |
| `default_encoding`    | String   | 默认编码                              | UTF-8               |
| `static_resource_dir` | String   | 静态资源目录                          | static              |
| `user_path`           | String   | 用户文件存储路径                      | user                |
| `data_dir`            | String   | 数据存储目录                          | data                |
| `log_dir`             | String   | 日志文件存储目录                      | data/log            |
| `log_level`           | Integer  | 日志级别（0: 关闭, 1: 信息, 2: 调试） | 1                   |



#### redirects

| **字段** | **类型** | **含义**         | 示例        |
| -------- | -------- | ---------------- | ----------- |
| `path`   | String   | 需要重定向的路径 | `/old-path` |
| `target` | String   | 重定向的目标路径 | `/new-path` |
| `status` | Integer  | HTTP 状态码      | 301         |
