
### 项目结构
```
├─ config/                             // 配置文件目录
├─ resources/                          // 资源文件目录
├─ src/
│  └─ main/
│     └─ java/
│        └─ edu.nju.http/
│           ├─ client/                // 客户端
│           │  ├─ Cache.java
│           │  ├─ ClientDriver.java
│           │  ├─ Config.java
│           │  └─ HttpClient.java
│           │
│           ├─ message/               // HTTP 消息
│           │  ├─ constant/
│           │  │  ├─ Header.java
│           │  │  ├─ Method.javagit
│           │  │  ├─ Status.java
│           │  │  └─ Version.java
│           │  ├─ HttpMessage.java
│           │  ├─ HttpRequest.java
│           │  ├─ HttpResponse.java
│           │  └─ MIME.java
│           │
│           ├─ server/                // 服务端
│           │  ├─ Config.java
│           │  ├─ HttpServer.java
│           │  ├─ ResponseBuilder.java
│           │  ├─ ServerHandler.java
│           │  └─ UserSystem.java
│           │
│           └─ utils/                 // 工具类
│              ├─ Log.java
│              └─ Searcher.java
└─ ...
```
