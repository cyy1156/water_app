# IntelliJ IDEA 配置 JDK 和 Maven 指南

## ⚠️ 当前问题

IDE 显示 **"未定义项目 JDK"**，这会导致：
- 无法编译代码
- Maven 无法下载依赖
- 无法运行项目

---

## 🔧 解决步骤

### 步骤1：配置项目 JDK

#### 方法A：使用 IDE 自动安装（推荐）

1. **点击警告栏中的"安装 SDK"按钮**
   - 在 IDE 顶部黄色警告栏中，点击蓝色的 **"安装 SDK"** 按钮
   - IDE 会自动检测并提示安装 JDK

2. **选择 JDK 版本**
   - 推荐选择 **JDK 1.8** 或 **JDK 11**（项目配置的是 1.8）
   - 点击"下载"或"安装"

3. **等待安装完成**
   - IDE 会自动下载并配置 JDK

#### 方法B：手动配置已安装的 JDK

如果你已经安装了 JDK：

1. **打开项目设置**
   - File → Project Structure（或按 `Ctrl + Alt + Shift + S`）

2. **配置 Project SDK**
   - 在左侧选择 **"Project"**
   - 在右侧找到 **"SDK"** 下拉框
   - 点击下拉框，选择已安装的 JDK
   - 如果没有，点击 **"Add SDK"** → **"Download JDK"**

3. **配置 Project language level**
   - 设置为 **8 - Lambdas, type annotations etc.**（对应 JDK 1.8）

4. **点击 OK 保存**

---

### 步骤2：配置 Maven

1. **打开 Maven 设置**
   - File → Settings（或按 `Ctrl + Alt + S`）
   - 在左侧找到 **Build, Execution, Deployment** → **Build Tools** → **Maven**

2. **配置 Maven**
   - **Maven home path**：选择 Maven 安装路径
     - 如果已安装 Maven，选择安装目录
     - 如果没有，使用 IDEA 内置的 Maven（通常已配置好）
   - **User settings file**：使用默认或自定义 settings.xml
   - **Local repository**：本地仓库路径（默认即可）

3. **配置 Maven 镜像（可选，但推荐）**
   - 点击 **"User settings file"** 旁边的文件夹图标
   - 打开 `settings.xml` 文件
   - 在 `<mirrors>` 标签中添加：
     ```xml
     <mirror>
         <id>aliyun</id>
         <mirrorOf>central</mirrorOf>
         <name>Aliyun Maven</name>
         <url>https://maven.aliyun.com/repository/public</url>
     </mirror>
     ```
   - 保存文件

4. **点击 OK 保存**

---

### 步骤3：让 Maven 下载依赖

配置好 JDK 后，Maven 会自动开始下载依赖。

#### 方法A：自动下载（推荐）

1. **等待自动下载**
   - 配置好 JDK 后，IDE 右下角会显示进度条
   - 显示 "Indexing..." 或 "Downloading dependencies..."
   - 首次下载需要几分钟，请耐心等待

2. **查看下载进度**
   - 点击 IDE 右下角的进度条
   - 或查看底部的 **"Event Log"** 窗口

#### 方法B：手动触发下载

如果自动下载没有开始：

1. **打开 Maven 工具窗口**
   - 点击右侧边栏的 **"Maven"** 图标
   - 或 View → Tool Windows → Maven

2. **刷新项目**
   - 在 Maven 工具窗口中，点击刷新按钮 🔄
   - 或右键项目 → Maven → Reload project

3. **手动下载依赖**
   - 在 Maven 工具窗口中，展开项目
   - 展开 **"Lifecycle"**
   - 双击 **"install"** 或 **"compile"**

---

## ✅ 验证配置

### 检查 JDK 配置

1. **查看项目设置**
   - File → Project Structure
   - 确认 **Project SDK** 已选择 JDK
   - 确认 **Project language level** 为 8

2. **查看代码**
   - 代码不应该有红色错误
   - 导入语句应该正常（没有红色波浪线）

### 检查 Maven 配置

1. **查看 Maven 工具窗口**
   - 右侧边栏打开 Maven
   - 应该能看到项目结构
   - 依赖应该显示在 **"Dependencies"** 下

2. **查看依赖下载**
   - 在 Maven 工具窗口中
   - 展开 **"Dependencies"**
   - 应该能看到所有依赖包（如：spring-boot-starter-web、mybatis-plus 等）

---

## 📋 完整配置流程

### 第一次打开项目

1. **打开项目**
   - File → Open → 选择 `D:\小程序\backend`

2. **配置 JDK**（必须）
   - 点击警告栏的"安装 SDK"
   - 或手动配置 Project Structure

3. **等待 Maven 下载依赖**（自动）
   - 右下角显示进度条
   - 首次需要 3-10 分钟
   - 下载完成后，依赖会显示在 Maven 工具窗口中

4. **配置 Maven 镜像**（可选，但推荐）
   - 使用阿里云镜像加速下载

5. **运行项目**
   - 找到 `WaterAppApplication.java`
   - 右键 → Run 'WaterAppApplication'

---

## 🔍 如何查看 Maven 下载进度

### 方法1：查看 Event Log

1. 点击 IDE 右下角的通知图标 🔔
2. 查看 "Maven" 相关的通知
3. 会显示下载进度和状态

### 方法2：查看 Maven 工具窗口

1. 打开右侧 Maven 工具窗口
2. 查看底部的输出信息
3. 会显示下载的依赖和进度

### 方法3：查看底部状态栏

1. 查看 IDE 底部状态栏
2. 会显示 "Indexing..." 或 "Downloading..."
3. 完成后会显示 "Indexing completed"

---

## ⚠️ 常见问题

### 问题1：JDK 配置后仍然报错

**解决方法**：
1. File → Invalidate Caches / Restart
2. 选择 "Invalidate and Restart"
3. 等待 IDE 重启并重新索引

### 问题2：Maven 下载很慢

**解决方法**：
1. 配置 Maven 镜像（使用阿里云镜像）
2. 检查网络连接
3. 使用 VPN（如果访问国外仓库）

### 问题3：依赖下载失败

**解决方法**：
1. 检查网络连接
2. 配置 Maven 镜像
3. 清理并重新下载：
   - 在 Maven 工具窗口中
   - 右键项目 → Maven → Reload project

### 问题4：找不到 Maven 工具窗口

**解决方法**：
1. View → Tool Windows → Maven
2. 或点击右侧边栏的 Maven 图标

---

## 🎯 快速检查清单

配置完成后，确认：

- [ ] JDK 已配置（Project Structure 中可以看到）
- [ ] 代码没有红色错误
- [ ] Maven 工具窗口可以打开
- [ ] 依赖列表中有 spring-boot-starter-web 等包
- [ ] 可以右键运行 `WaterAppApplication.java`

---

## 💡 提示

1. **首次打开项目**：Maven 下载依赖是自动的，但需要时间
2. **如果下载很慢**：配置阿里云镜像可以加速
3. **如果一直不下载**：检查 JDK 是否配置正确
4. **下载完成后**：依赖会缓存在本地，下次打开项目会很快

---

**配置好 JDK 后，Maven 会自动开始下载依赖，请耐心等待几分钟！**

