# 小小账本

一款使用 Kotlin 与 Jetpack Compose 开发的本地记账应用，支持收支记录、月度统计、分类预算、周期记账、提醒设置与个性化外观。项目以 Room 作为本地数据源，通过 Repository、ViewModel 和 Flow 驱动界面更新。

## 功能概览

- 记录、编辑和删除支出与收入
- 按月份查看账单、收入、支出和结余
- 按分类汇总支出并显示预算进度
- 设置月度分类预算和超支状态
- 创建每周或每月自动执行的周期账目
- 删除账目后通过 Snackbar 撤销
- 设置中午和晚间记账提醒
- 自定义主题色、背景色和裁剪后的背景图片
- 支持 Room 数据库版本迁移

## 应用截图

| 月度总览 | 账单记录 | 收入记录 |
| --- | --- | --- |
| ![月度总览](imgForREADME/monthly-summary.png) | ![账单记录](imgForREADME/expense-entry.png) | ![收入记录](imgForREADME/income-entry.png) |

| 分类预算 | 周期记账 | 外观设置 |
| --- | --- | --- |
| ![分类预算](imgForREADME/category-budget.png) | ![周期记账](imgForREADME/recurring-entry.png) | ![外观设置](imgForREADME/appearance-settings.png) |

## 技术栈

- Kotlin、Coroutines、Flow、StateFlow、SharedFlow
- Jetpack Compose、Material 3、ViewModel
- Room、Migration、DataStore Preferences
- WorkManager、系统通知与定时提醒
- Android Photo Picker 与图片裁剪
- JUnit、Room 仪器测试

## 项目架构

```text
Compose UI
    │ 用户事件
    ▼
ViewModel
    │ 业务调用
    ▼
LedgerRepository
    ├── Room / DAO
    ├── DataStore / SharedPreferences
    └── WorkManager / ReminderScheduler

Room Flow
    ▼
Repository
    ▼
StateFlow / SharedFlow
    ▼
collectAsStateWithLifecycle
    ▼
Compose 重组
```

主要目录：

```text
app/src/main/java/com/example/billkeeper/
├── background/    # 周期任务、背景与外观设置
├── data/          # Entity、DAO、Database、Repository 和数据模型
├── domain/        # 周期账目等领域规则
├── notification/  # 通知、提醒计划与广播接收器
├── ui/            # Compose 页面、组件和主题
└── viewmodel/     # UI 状态、Flow 组合与用户操作
```

## 核心实现

### 月度查询与状态聚合

月份变化后，ViewModel 使用 `flatMapLatest` 取消旧月份查询并订阅新时间范围。查询采用“本月第一天至下月第一天”的左闭右开区间，避免月底、闰年和最后一毫秒的边界错误。账单、收入、分类汇总和上月支出通过 `combine` 聚合为统一的 `MonthlyUiState`。

### 分类预算

预算按年份、月份和分类保存。ViewModel 将预算 Flow 与分类支出 Flow 组合，计算已支出金额、剩余金额、使用比例和超支状态，数据库数据改变后界面自动刷新。

### 周期记账

周期账目支持每周和每月规则。WorkManager 负责后台检查到期任务，数据库事务负责生成账目并更新下次执行时间，避免任务只完成一半。时间计算逻辑可通过固定 `Clock` 进行稳定测试。

### 删除撤销

删除成功后，ViewModel 通过 `SharedFlow` 发送一次性 Snackbar 事件并暂存被删除对象。用户点击撤销后重新写入 Room，相关 Flow 发出新数据并驱动列表恢复。

### 数据库迁移

项目使用显式 Room Migration 升级数据库结构，保留已有用户数据，并通过仪器测试验证旧版本数据库能够正确迁移到新版本。

## 测试

JVM 单元测试位于 `app/src/test/`，覆盖：

- 金额模型和表单校验
- 月份范围与时区转换
- 周期日期规则
- 提醒调度规则

Android 仪器测试位于 `app/src/androidTest/`，覆盖：

- Room 数据库迁移
- 周期账目事务执行

运行 JVM 单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```

连接模拟器或真机后运行仪器测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## 运行项目

环境要求：

- Android Studio
- JDK 17
- Android SDK 34
- 最低 Android 版本：Android 8.0（API 26）

使用 Android Studio 打开项目根目录，完成 Gradle Sync 后运行 `app` 配置。也可以在项目根目录执行：

```powershell
.\gradlew.bat assembleDebug
```

生成的 Debug APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 后续计划

1. 使用navigation3：把月/周自动更新账单功能和主题/背景自定义功能以及闹钟功能放在一个二级页面入口进去各自为一个选项做成三级页面，在该二级页面还会增加一个查询单日账单的功能
2. 新增自我获取更新功能
3. 后续需要改接口为下面这样
   LedgerRepository 接口
   RoomLedgerRepository 真实实现
   FakeLedgerRepository 测试实现
