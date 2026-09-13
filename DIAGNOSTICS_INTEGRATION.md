# BillKeeper Diagnostics 第一版接入说明

当前分支：`codex/k-sharednav-integration`

本版本通过 composite build 接入本地 `k-diagnostics`，不修改原 `k-sharednav`。BK 侧只负责宿主初始化、界面交互、上传调度和后端适配。

## Supabase 配置与接口

Supabase 配置从本地 `local.properties` 读取，不进入 Git：

```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
```

只使用 publishable key；不要把数据库密码、`service_role` 或 `sb_secret_...` 放入 Android 项目。

数据库接口位于：

```text
app/src/main/java/com/example/billkeeper/diagnostics/SupabaseDiagnosticApi.kt
```

接口定义：

```kotlin
fun interface SupabaseDiagnosticApi {
    suspend fun upload(events: List<DiagnosticEvent>): UploadResult
}
```

当前分支已经提供 `SupabaseRestDiagnosticApi`，通过 Supabase Data API 的
`POST /rest/v1/diagnostic_events` 上传批次；`BillKeeperApplication` 会根据
`BuildConfig` 自动选择真实适配器或未配置占位实现。网络请求在 `Dispatchers.IO`
执行，2xx 和重复事件视为成功，408/425/429/5xx 返回可重试失败，其余 HTTP 错误标记为永久失败。

Supabase 表和权限建议在 SQL Editor 执行：

```sql
create table public.diagnostic_events (
    event_id varchar(100) primary key,
    event_type varchar(20) not null
        check (event_type in ('CRASH', 'ANR', 'UI_BLOCK')),
    occurred_at timestamptz not null,
    app_version varchar(100),
    device_model varchar(200),
    android_version varchar(100),
    thread_name varchar(200),
    duration_ms bigint check (duration_ms is null or duration_ms >= 0),
    message varchar(4000),
    stack_trace text check (
        stack_trace is null or octet_length(stack_trace) <= 262144
    ),
    metadata jsonb not null default '{}'::jsonb,
    schema_version integer not null default 1,
    created_at timestamptz not null default now()
);

alter table public.diagnostic_events enable row level security;
revoke all on table public.diagnostic_events from anon, authenticated;
grant insert on table public.diagnostic_events to anon, authenticated;

create policy "diagnostic clients can only insert"
    on public.diagnostic_events
    for insert
    to anon, authenticated
    with check (
        char_length(event_id) > 0
        and char_length(event_id) <= 100
        and event_type in ('CRASH', 'ANR', 'UI_BLOCK')
    );
```

`event_id` 是幂等主键；客户端重试不会产生重复诊断记录。当前直接 Data API
方案适合第一版真机验证，正式发布前建议改为 Edge Function 入口并增加限流。

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
- 写入 Crash/ANR/UI_BLOCK：只写入本地事件，不退出应用，用于测试上传和重试；
- 刷新：读取本地事件及状态；
- 失败原因：显示最近一次 `lastError` 和最大 `retryCount`，便于定位 RLS、表结构和网络问题；
- 触发上传：执行 WorkManager 唯一上传任务；
- 重试失败：将 `FAILED` 事件重置为 `PENDING` 并重新调度上传；
- 清理待处理：删除 `PENDING` 事件。

## 当前验证

```bash
./gradlew :app:assembleDebug --offline --no-daemon
./gradlew :app:testDebugUnitTest --offline --no-daemon
```

上述 Debug 构建和单元测试已通过。真机还需验证：

1. 点击“写入 Crash/ANR/UI_BLOCK”，确认事件进入 `PENDING`；
2. 点击“触发上传”，确认状态变为 `REPORTED`，并在 Supabase 表中出现对应记录；
3. 关闭网络后触发上传，确认 `PENDING` 和 `retryCount` 增加；恢复网络后确认 WorkManager 重试成功；
4. 点击“测试崩溃”，重启应用后确认崩溃弹窗的“上报/删除/稍后处理”；
5. 使用 Release 构建再次确认 R8 和配置缺失时不会崩溃。
