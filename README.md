[![Banners](docs/images/index.png)](https://github.com/dbcjl/RuoYi-xiaozhi)

<h1 align="center">基于若依管理系统的xiaozhi-esp32服务端</h1>

<p align="center">
本项目为开源智能硬件项目
<a href="https://github.com/78/xiaozhi-esp32" target="_blank">xiaozhi-esp32</a>提供后端服务<br/>
根据<a href="https://github.com/78/xiaozhi-esp32/blob/main/docs/websocket.md" target="_blank">小智WebSocket通信协议</a>使用RuoYi-Vue、Spring AI、轻量级Java-WebSocket、ffmpeg实现<br/>
帮助您快速搭建小智服务器
</p>

<p align="center">
<a href="https://github.com/dbcjl/RuoYi-xiaozhi/issues" target="_blank">反馈问题</a>
· <a href="./README.md#项目部署" target="_blank">项目部署</a>
· <a href="https://github.com/dbcjl/RuoYi-xiaozhi/releases" target="_blank">更新日志</a>
</p>
<p align="center">
  <a href="https://github.com/dbcjl/RuoYi-xiaozhi/releases">
    <img alt="GitHub Contributors" src="https://img.shields.io/github/v/release/dbcjl/RuoYi-xiaozhi?logo=docker" />
  </a>
  <a href="https://github.com/dbcjl/RuoYi-xiaozhi/graphs/contributors">
    <img alt="GitHub Contributors" src="https://img.shields.io/github/contributors/dbcjl/RuoYi-xiaozhi?logo=github" />
  </a>
  <a href="https://github.com/dbcjl/RuoYi-xiaozhi/issues">
    <img alt="Issues" src="https://img.shields.io/github/issues/dbcjl/RuoYi-xiaozhi?color=0088ff" />
  </a>
  <a href="https://github.com/dbcjl/RuoYi-xiaozhi/pulls">
    <img alt="GitHub pull requests" src="https://img.shields.io/github/issues-pr/dbcjl/RuoYi-xiaozhi?color=0088ff" />
  </a>
  <a href="https://github.com/dbcjl/RuoYi-xiaozhi/blob/main/LICENSE">
    <img alt="GitHub pull requests" src="https://img.shields.io/badge/license-MIT-white?labelColor=black" />
  </a>
  <a href="https://github.com/dbcjl/RuoYi-xiaozhi">
    <img alt="stars" src="https://img.shields.io/github/stars/dbcjl/RuoYi-xiaozhi?color=ffcb47&labelColor=black" />
  </a>
</p>

---

## 一、系统要求

- 操作系统：Linux / macOS / Windows
- Java 版本：**Java 21**
- Maven 版本：建议使用 **Apache Maven 3.8+**
- Node.js（仅用于前端模块编译）：**Node.js 18+**

> 备注：本项目基于 **Java 21** 开发，主要为了使用 **虚拟线程（Virtual Threads）** 提升聊天服务的并发处理能力。相比传统线程，虚拟线程更轻量，可支持大量并发 WebSocket 连接，显著提高系统吞吐量和响应性能。

## 二、项目技术栈

- **后端框架**：RuoYi-Vue 后端子项目（Spring Boot）
- **通信协议**：<a href="https://github.com/78/xiaozhi-esp32/blob/main/docs/websocket.md" target="_blank">小智 WebSocket 通信协议</a>
- **WebSocket 库**：Java-WebSocket（轻量级嵌入式服务）
- **AI 接口**：Spring AI（OpenAI / 百度 / 阿里等大模型接入）
- **音视频处理**：JavaCV-FFmpeg（音频编码、格式转换等）

> 备注：WebSocket 服务采用 **轻量级 Java-WebSocket 实现**，**不依赖 Tomcat 或 Servlet 容器**，启动更快、运行更轻量。

## 三、功能清单

| 模块名称           | 功能说明                             | 状态       |
|--------------------|----------------------------------|------------|
| 核心服务架构       | 基于 WebSocket 和 HTTP 提供控制台管理与认证系统 | ✅ 已实现   |
| 管理后台           | 提供 Web UI 管理，支持用户、配置和设备管理        | ✅ 已实现   |
| 语音交互系统       | 支持 ASR、流式 TTS、VAD，兼容多语言语音处理      | ✅ 已实现   |
| 智能对话系统       | 集成多种 LLM，实现上下文对话与响应              | ✅ 已实现   |
| 意图识别系统       | LLM + Function Call 实现插件化意图识别与调用 | ✅ 已实现   |
| 记忆系统           | 支持本地短期记忆                         | ✅ 已实现   |
| IOT/MCP控制协议    | 支持设备注册、控制接口，兼容 IOT 与 MCP 协议      | 🔧 开发中   |
| 插件系统           | 支持插件扩展、热加载与自定义插件开发               | 🔧 开发中   |


## 四、语音能力支持列表

### 🎙️ VAD（Voice Activity Detection）声音活动检测

| 名称         | 是否免费 | 是否本地 | 是否支持流式 | 备注说明                                       |
|--------------|----------|----------|----------------|------------------------------------------------|
| Silero VAD   | ✅ 是     | ✅ 是     | ✅ 支持        | 基于神经网络模型，轻量准确，适合边缘端使用      |

> ⚙️ Silero VAD v5 版本已集成并优化，适配低延迟处理场景。

---

### 🗣️ ASR（Automatic Speech Recognition）语音识别

| 名称         | 是否免费 | 是否支持流式 | 备注说明                 |
|--------------|----------|--------|--------------------------|
| SenseVoice   | ✅ 免费  | ❌ 不支持  | 本地化私有部署，适配 ESP32 上传音频 |

> 备注：未来可扩展 Whisper、AliASR、百度 ASR 等大模型方案，支持多语种和更复杂环境下的识别。

---

### 🔊 TTS（Text To Speech）语音合成

| 名称             | 是否免费       | 是否支持流式 | 备注说明                    |
|------------------|------------|----------------|-------------------------|
| EdgeTTS          | ✅ 免费       | ✅ 支持        | 微软 Edge 浏览器接口，支持多语种、多角色 |
| 火山引擎 TTS     | ❌ 收费，有免费额度 | ✅ 双流式支持 | 支持双流，音色丰富、响应快、体验好       |

## 五、项目部署