<p align="center">
  <img src="logo/app_logo.png" alt="StudyLock Logo" width="120" height="120" />
</p>

<h1 align="center">StudyLock</h1>
<p align="center"><strong>双模式深度工作助手</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-green?logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-1.9%2B-blue?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Compose-Material%203-purple?logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-orange" alt="Min SDK" />
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License" />
</p>

<p align="center">
  基于 Cal Newport《深度工作》理论，专为大学生打造的「小而精」专注锁机 APP。<br/>
  不需要复杂配置，不需要一堆权限。开箱即用，该锁就锁。
</p>

---

## 截图

<p align="center">
  <em>（截图占位 — 欢迎提交真实截图 PR）</em>
</p>

| 深度工作主屏 | 锁机页面 | 课表 OCR 导入 |
|:---:|:---:|:---:|
| 大按钮 + 链条 + 权限引导 | 全屏锁机 + 白名单工具 | 拍照识别 + 手动编辑 |

---

## 理念

### 为什么做这个 APP

市面上的专注类 APP 越来越像一个臃肿的平台——番茄钟、白噪音、Todo List、统计报表、社交激励、会员体系……功能堆砌得越多，离「专注」这个初心越远。

我要的是一个**在最需要的时刻，简单粗暴地让手机变得不可用**的工具。仅此而已。

### 理论基础：Cal Newport《深度工作》

Cal Newport 在《深度工作》一书中提出了四种深度工作策略：

| 策略 | 核心做法 | 典型人群 |
|------|---------|---------|
| **僧侣式** (Monastic) | 彻底消除浅薄工作，长期与世隔绝 | 作家、理论研究者 |
| **双模式** (Bimodal) | 把时间明确划分为「深度时段」和「浅薄时段」 | 大学生、知识工作者 |
| **节奏式** (Rhythmic) | 每天固定时间段深度工作，形成链条不打断 | 上班族、自由职业者 |
| **记者式** (Journalistic) | 在任何空闲缝隙中挤入深度工作 | 日程不固定的人 |

**StudyLock 选择了双模式**。

对于大学生来说，这个划分天然存在：

```
上课时间  ──→  自动深度时段（手机自动锁定）  ← 被动纪律
空闲时间  ──→  手动启动深度时段（自选 30/60/90/120 分钟）  ← 主动选择
```

你不需要在每节课开始前「决定」要不要好好听课。课表导入后，上课时间到了，手机自动锁死。系统替你执行这个决定——这就是 Newport 所说的**被动纪律（Passive Discipline）**。

### 设计原则

**一、小而精，不是大而全**

不做 Todo List。不做白噪音。不做统计报表。不做社交激励。不做会员。只做一件事：**在你需要专注的时候，锁住手机**。

这不是功能缺失，这是刻意设计。每一个被拒绝的功能，都是对「专注」的保护。

**二、被动纪律 > 主动意志**

意志力是有限资源。每次你「决定」不刷手机，都在消耗意志力。而如果在进入教室之前，手机就已经自动锁死了——你就不需要做任何决定。这就是被动纪律的力量。

**三、不可绕过**

- 按 Home 键 → 自动弹回锁机页面
- 按返回键 → 无效
- 从多任务界面划掉 → 没用
- 唯一的出口：**密码解锁**或**紧急解锁**（每天限 3 次）

**四、白名单而非黑名单**

其他锁机 APP 让你手动配置「要拦截哪些 APP」。这有两个问题：（1）配置麻烦；（2）总有漏网之鱼。

StudyLock 反过来——锁机期间**只放行** 4 个学习工具（计算器、时钟、日历、词典）。其他全部不可用。你不需要配置任何东西。

**五、最少权限原则**

| 必要 | 权限 | 原因 |
|:----:|------|------|
| ✅ | 显示在其他应用上层 | 锁机页面需要覆盖其它 APP |
| ⚪ | 忽略电池优化 | 防止系统后台杀死守护服务 |

不需要无障碍服务。不需要使用情况访问权限。不需要通知权限。开箱即用。

### 为什么不是番茄钟

番茄钟（25 分钟专注 + 5 分钟休息）是一种经典的时间管理方法，但它有一个根本性问题：

**你随时可以取消它。**

当手机弹出「休息时间到了 🎉」的通知，你很容易顺势「休息一下」——刷 10 分钟短视频，再告诉自己「等下一个番茄钟我一定认真」。这就是意志力陷阱。

StudyLock 不做番茄钟。你设定的就是连续深度工作时长（30/60/90/120 分钟）。期间手机就是一块砖头。没有休息提醒。没有完成庆祝。只有倒计时归零后你才能解锁。

**这才是真正的深度工作。**

---

## 功能

### 一、双模式锁机

| 模式 | 触发方式 | 时长 | 适用场景 |
|------|---------|------|---------|
| 🏫 **自动模式** | 导入课表后，上课时间自动触发 | 跟随课表节次 | 课堂专注 |
| ✋ **手动模式** | 主屏点击「开始深度工作」，选择时长 | 30 / 60 / 90 / 120 分钟 | 自习、图书馆、备考 |

### 二、课表 OCR 导入

- 📷 **拍照识别**：对准课表拍照，Google ML Kit 自动识别中文
- 🖼️ **相册导入**：从相册选择课表截图
- 🔍 **智能解析**：自动识别课程名称、时间、地点、周次
- ✏️ **手动修正**：识别后可逐门课程编辑，支持手动添加/删除
- 📝 **文字输入**：OCR 失败时可粘贴课表文字手动解析

### 三、白名单学习工具

锁机界面上可直接使用的 4 个学习工具：

| 工具 | 系统包名 |
|------|---------|
| 🔢 计算器 | com.android.calculator2 |
| ⏰ 时钟 | com.android.deskclock |
| 📅 日历 | com.android.calendar |
| 📖 词典 | com.pleco.chinesesystem |

### 四、紧急解锁

每天 **3 次**，每次 **5 分钟**。用于紧急电话、重要消息等场景。用完即止，不累积。

### 五、密码保护

- BCrypt 加密存储，不保存明文
- 连续 3 次输错 → 锁定 10 分钟
- 可选「冷静期」：密码正确后等待 60 秒才能真正解锁，防止冲动解锁

### 六、连续专注链条

> "Don't break the chain." — Jerry Seinfeld

主屏显示连续深度工作天数。今天有没有专注过？一目了然。

---

## 技术栈

| 组件 | 选型 | 说明 |
|------|------|------|
| 语言 | Kotlin | JVM Target 17 |
| UI | Jetpack Compose | Material 3 Design |
| 数据库 | Room | SQLite + Flow 响应式查询 |
| OCR | Google ML Kit | 中文文字识别（离线模型） |
| 密码 | jBCrypt | 单向哈希，不可逆 |
| 导航 | Navigation Compose | 单 Activity 架构 |
| 后台 | Foreground Service + WorkManager | 守护服务持久运行 |
| 最低 SDK | Android 8.0 (API 26) | 覆盖 98%+ 活跃设备 |
| 目标 SDK | Android 14 (API 34) | 适配最新系统限制 |

---

## 构建与安装

### 从源码构建

```bash
# 1. 克隆项目
git clone https://github.com/JIAChenyu525/StudyLock.git

# 2. 用 Android Studio 打开项目目录
# 3. Sync Gradle
# 4. Run 'app'
```

### 命令行构建

```bash
# Debug APK
./gradlew assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# Release APK（需要签名配置）
./gradlew assembleRelease
```

### 下载安装

前往 [Releases](https://github.com/JIAChenyu525/StudyLock/releases) 下载最新 APK。

在手机上：设置 → 安全 → 允许安装未知来源应用 → 安装。

---

## 项目结构

```
app/src/main/java/com/studylock/app/
├── data/                          # 数据层
│   ├── dao/                       # Room DAO 接口（Semester, Course, FocusRecord, etc.）
│   ├── database/                  # StudyLockDatabase + MIGRATION_1_2
│   ├── entity/                    # 实体类（Course, Semester, FocusRecord, UserSettings）
│   └── repository/                # 仓库层（封装 DAO 操作）
├── feature/
│   ├── deepwork/                  # 🔥 深度工作主屏
│   │   ├── DeepWorkScreen         # 大按钮 + 连续链条 + 今日时间线 + 权限面板
│   │   ├── DeepWorkViewModel      # 状态管理（Guard 状态、连续天数、时间线构建）
│   │   └── DeepWorkUiState        # UI 状态数据类
│   ├── focus/                     # 🔒 锁机页面 + 密码
│   │   ├── FocusBlockActivity     # 全屏锁机 Activity（Home 键自动弹回）
│   │   ├── PasswordSetupScreen    # 密码设置/修改
│   │   ├── PasswordUtils          # 密码强度评估
│   │   └── MotivationalQuotes     # 专注名言库
│   ├── permission/                # 🛡️ 权限引导
│   │   ├── PermissionGuideScreen  # 2 步快速引导
│   │   ├── PermissionUtils        # 权限检测工具
│   │   └── ManufacturerUtils      # 厂商保活指南（小米/华为/OPPO/vivo）
│   ├── schedule/                  # 📅 课表管理
│   │   ├── ScheduleScreen         # 7 天课程网格 + 周次导航
│   │   ├── CourseOcrImportScreen  # OCR 拍照导入界面
│   │   ├── CourseOcrParser        # OCR 文字→课程解析引擎
│   │   ├── AddEditCourseDialog    # 课程添加/编辑弹窗
│   │   └── ClassTimeConfigScreen  # 节次时间配置
│   └── settings/                  # ⚙️ 设置
│       ├── DataBackupScreen       # 数据备份/恢复
│       └── AboutAppScreen         # 关于 APP
├── service/                       # 后台服务
│   ├── FocusGuardService          # 前台守护服务（计时 + 通知）
│   ├── FocusAccessibilityService  # 无障碍服务（备用）
│   ├── FocusTimeChecker           # 课表时间判断工具
│   └── BootReceiver               # 开机自启
└── ui/
    ├── theme/                     # Material 3 主题（Color, Theme, Type）
    └── util/CourseColors          # 课程色彩方案
```

---

## 参与贡献

欢迎任何形式的贡献！Issue 报 Bug、Feature Request 提建议、PR 改代码都欢迎。

### 提 Issue

- 🐛 Bug 报告：描述复现步骤、设备型号、Android 版本
- 💡 功能建议：说明使用场景、为什么需要这个功能
- ❓ 使用问题：描述你遇到的困惑

### 提交 PR

1. Fork 本项目
2. `git checkout -b feature/your-feature`
3. `git commit -m 'feat: add your feature'`
4. `git push origin feature/your-feature`
5. 提交 Pull Request

### 开发约定

- 代码需编译通过（`./gradlew assembleDebug`）
- 新功能围绕「深度工作锁机」核心，保持「小而精」
- 不引入不必要的权限依赖
- Kotlin 代码风格遵循官方规范

---

## 路线图

- [x] 双模式锁机（自动课表 + 手动深度工作）
- [x] OCR 课表拍照导入
- [x] 白名单学习工具
- [x] 紧急解锁（3 次/天，5 分钟/次）
- [x] 密码保护 + 冷静期
- [x] 连续专注天数链条
- [x] 主页权限一键开启面板
- [ ] 教务系统账号导入课表
- [ ] 小组/班级共享课表
- [ ] Widget 桌面快捷启动深度工作
- [ ] 学期周次从课表日期自动计算
- [ ] 多学期切换
- [ ] 中文/英文多语言

---

## 常见问题

### Q: 为什么需要「显示在其他应用上层」权限？
A: 锁机页面是一个全屏 Activity，需要覆盖在其他 APP（包括系统桌面）之上。这是 Android 系统的要求，所有锁机类 APP 都需要这个权限。

### Q: 锁机期间手机来电话怎么办？
A: 你可以使用紧急解锁（每天 3 次，每次 5 分钟），或者在白名单中使用系统拨号应用。

### Q: 重启手机后锁机会失效吗？
A: 不会。FocusGuardService 会在开机后自动启动，继续计时。如果还在深度工作时段内，锁机页面会自动弹出。

### Q: 为什么不支持自定义白名单？
A: 刻意设计。自定义白名单 = 每个用户都要花时间配置 = 增加使用摩擦。我们预设了 4 个最常用的学习工具，覆盖 90% 的学习场景。如果你有特殊需求，欢迎提 Issue 讨论。

### Q: 数据存储在哪里？
A: 所有数据（课表、专注记录、设置）存储在手机本地 Room 数据库中。不上传任何服务器。可以使用「数据备份」功能导出为 JSON 文件。

---

## 开源协议

MIT License — 自由使用、修改、分发。

---

*"The ability to perform deep work is becoming increasingly rare at exactly the same time it is becoming increasingly valuable in our economy. As a consequence, the few who cultivate this skill, and then make it the core of their working life, will thrive."*

*— Cal Newport, Deep Work: Rules for Focused Success in a Distracted World*
