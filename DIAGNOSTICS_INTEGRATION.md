# BillKeeper Diagnostics 第一版接入说明

当前分支：`codex/k-sharednav-integration`

本版本通过 composite build 接入本地 `k-diagnostics`，不修改原 `k-sharednav`。BK 侧只负责宿主初始化、界面交互、上传调度和后端适配。

## Supabase 接口位置

待补充的数据库接口位于：

```text
app/src/main/java/com/example/billkeeper/diagnostics/SupabaseDiagnosticApi.kt
```

需要实现：

```kotlin
fun interface SupabaseDiagnosticApi {
    suspend fun upload(events: List<DiagnosticEvent>): UploadResult
}
```

然后在 `BillKeeperApplication` 中将 `UnconfiguredSupabaseDiagnosticApi` 替换为真实实现。Supabase URL、anon key、请求体、RLS 和表结构均只放在 BK，不写入 `k-diagnostics`。

## 相关接入文件

```text
app/src/main/java/com/example/billkeeper/BillKeeperApplication.kt
app/src/main/java/com/example/billkeeper/diagnostics/BillKeeperDiagnosticReporter.kt
app/src/main/java/com/example/billkeeper/diagnostics/DiagnosticEventSummary.kt
app/src/main/java/com/example/billkeeper/ui/CrashReportDialog.kt
app/src/main/java/com/example/billkeeper/ui/diagnostics/DiagnosticTestPanel.kt
```

## Debug 测试入口

Debug 构建启动后，测试面板提供：

- 测试崩溃：抛出未捕获异常，重启后检查崩溃事件；
- 测试 ANR：阻塞主线程约 6 秒；
- 测试卡顿：阻塞主线程约 400ms；
- 刷新：读取本地事件及状态；
- 触发上传：执行 WorkManager 唯一上传任务；
- 清理待处理：删除 `PENDING` 事件。

## 当前验证

```bash
./gradlew :app:assembleDebug --offline --no-daemon
./gradlew :app:testDebugUnitTest --offline --no-daemon
```

上述 Debug 构建和单元测试已通过。真机测试、Supabase 联调、断网重试和 Release/R8 验证由后续补充。
