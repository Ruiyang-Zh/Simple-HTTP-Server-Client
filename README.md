```
HTTP-Server-Client
├── src/
│   ├── main/
│   │   ├── java/              
│   │   │   ├── edu.nju.http/   
│   │   │   │   ├── client/     // 客户端
│   │   │   │   │   ├── Cache          // 客户端缓存管理
│   │   │   │   │   ├── ClientHandler  // 客户端消息处理
│   │   │   │   │   ├── Config         // 客户端配置
│   │   │   │   │   ├── HttpClient     // HTTP 客户端主类
│   │   │   │   │
│   │   │   │   ├── message/    // HTTP 消息
│   │   │   │   │   ├── constant/      // HTTP 常量定义
│   │   │   │   │   │   ├── Header     // HTTP 请求头
│   │   │   │   │   │   ├── Method     // HTTP 方法（GET, POST 等）
│   │   │   │   │   │   ├── Status     // HTTP 状态码
│   │   │   │   │   │   ├── Version    // HTTP 版本
│   │   │   │   │   ├── HttpMessage    // HTTP 消息抽象类
│   │   │   │   │   ├── HttpRequest    // HTTP 请求类
│   │   │   │   │   ├── HttpResponse   // HTTP 响应类
│   │   │   │   │   ├── MIME           // MIME 类型
│   │   │   │   │
│   │   │   │   ├── server/     // 服务器
│   │   │   │   │   ├── Config        // 服务器配置
│   │   │   │   │   ├── HttpServer    // HTTP 服务器主类
│   │   │   │   │   ├── ServerHandler // 服务器消息处理
│   │   │   │   │
│   │   │   │   ├── utils/      // 工具类
│   │   │   │   │   ├── Log           // 日志工具
│   │   │   │   │   ├── Searcher      // 搜索工具
│   │   │
│   │   ├── resources/         
│   │   │   ├── config/        // 内置配置文件
│   │   │   │   ├── config.json      
│   │   │   ├── static/        // 内置静态资源
│   │   │   │   ├── 400.html          // 400 错误页面
│   │   │   │   ├── 404.html          // 404 错误页面
│   │   │   │   ├── 405.html          // 405 错误页面
│   │   │   │   ├── 500.html          // 500 错误页面
│   │   │   │   ├── index.html        // 主页
│   │
│   ├── test/                 // 单元测试目录
│
├── static/        // 外部静态资源
├── target/            
├── .gitignore               
├── pom.xml                  
├── README.md 

```
