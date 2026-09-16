# Ollama 部署验证指南

## ✅ 你已经完成的步骤

1. ✅ 已安装 Ollama
2. ✅ 已下载 qwen2.5:1.5b 模型
3. ✅ 已测试模型可以正常运行

---

## 📋 接下来的配置步骤

### 步骤 1：确保 Ollama 服务正在运行

打开 PowerShell 或 CMD，运行：

```bash
ollama list
```

如果能看到 `qwen2.5:1.5b`，说明服务正常。

**如果服务未运行**，执行：
```bash
ollama serve
```
（这个命令会一直运行，不要关闭这个窗口）

---

### 步骤 2：配置后端

**文件**：`backend/src/main/resources/application.yml`

确认配置如下：

```yaml
app:
  chat:
    ai-type: ollama  # 使用 Ollama
    ollama:
      base-url: http://localhost:11434
      model: qwen2.5:1.5b
      timeout: 30000
```

---

### 步骤 3：重启后端服务

1. 停止当前运行的后端（如果正在运行）
2. 重新启动 Spring Boot 项目
3. 查看控制台日志，确认没有错误

**检查日志**：
- 应该能看到后端启动成功的日志
- 如果有错误，检查 Ollama 服务是否在运行

---

### 步骤 4：验证后端接口

#### 方法一：使用 Postman 或浏览器测试

**注意**：由于需要登录，建议使用 Postman。

1. **先登录获取 Token**：
   - `POST http://192.168.200.120:8080/api/user/login`
   - Body: `{"code": "test_code"}`（开发环境会自动处理）

2. **测试对话接口**：
   - `POST http://192.168.200.120:8080/api/chat/send`
   - Headers: `Authorization: Bearer 你的Token`
   - Body:
   ```json
   {
     "content": "你好，请介绍一下自己"
   }
   ```

3. **预期结果**：
   ```json
   {
     "code": 200,
     "message": "success",
     "data": {
       "sessionId": "xxx-xxx-xxx",
       "messages": [
         {
           "role": "user",
           "content": "你好，请介绍一下自己"
         },
         {
           "role": "assistant",
           "content": "你好！我是通义千问..."  // 这是真实的 AI 回复
         }
       ]
     }
   }
   ```

#### 方法二：查看后端日志

发送消息后，查看后端控制台日志：

```
调用 Ollama API: url=http://localhost:11434/api/generate, model=qwen2.5:1.5b
Ollama 回复成功，长度: 123
```

如果看到这些日志，说明调用成功！

---

### 步骤 5：在小程序中验证

1. **打开微信开发者工具**
2. **进入"对话"页面**（底部导航栏）
3. **输入消息**，例如："你好，请介绍一下自己"
4. **点击发送**

**预期结果**：
- ✅ 看到你的消息显示在右侧（用户消息）
- ✅ 看到 AI 的回复显示在左侧（助手消息）
- ✅ AI 回复是真实的、有意义的文本（不是"这是一条AI回复（功能开发中）"）

---

## 🔍 常见问题排查

### 问题 1：后端报错 "Connection refused"

**原因**：Ollama 服务未启动

**解决**：
```bash
ollama serve
```
保持这个窗口运行，不要关闭。

### 问题 2：后端报错 "model not found"

**原因**：模型名称不匹配

**解决**：
1. 运行 `ollama list` 查看已安装的模型
2. 修改 `application.yml` 中的 `model` 为实际模型名称

### 问题 3：小程序显示"发送失败"

**检查**：
1. 后端服务是否正常运行（查看后端控制台）
2. 网络连接是否正常
3. 查看小程序控制台的错误信息

### 问题 4：AI 回复是模拟回复

**原因**：配置未生效

**解决**：
1. 确认 `application.yml` 中 `ai-type: ollama`
2. 确认已重启后端服务
3. 查看后端日志，确认使用的是 `ollama` 类型

---

## ✅ 验证成功的标志

1. ✅ 后端日志显示 "调用 Ollama API"
2. ✅ 后端日志显示 "Ollama 回复成功"
3. ✅ 小程序能收到真实的 AI 回复（不是模拟回复）
4. ✅ AI 回复内容有意义，符合 qwen2.5 模型的回答风格

---

## 🎯 下一步

验证成功后，你可以：
1. 尝试不同的对话内容，测试 AI 能力
2. 查看对话历史功能是否正常
3. 测试图片上传功能（如果已实现）

---

**如果遇到问题，请查看后端控制台的详细错误日志！**

