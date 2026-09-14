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

`event_id` 是幂等主键；客户端使用普通 insert，重复主键返回 409 时按成功处理，因此重试不会产生重复诊断记录。当前直接 Data API
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
- 测试卡顿：在前台宽限期结束后阻塞主线程约 650ms；
- 写入 Crash/ANR：只写入本地待确认事件，不退出应用；
- 写入 UI_BLOCK：写入后保持现有自动上报规则；
- 提交模拟事件：对写入的 Crash/ANR 显示与崩溃重启相同的上报确认框；
- 事件统计每秒自动刷新；
- 失败原因：显示最近一次 `lastError` 和最大 `retryCount`，便于定位 RLS、表结构和网络问题；

上传范围约定：后台自动调度只上传不需要确认的真实 `ANR`/`UI_BLOCK` 和模拟 `UI_BLOCK`。
`CRASH` 以及带有 `requires_user_consent=true` 的模拟 `ANR` 会留在本地；点击“提交模拟事件”后，
仍需在确认框中点击“上报”才会进入上传任务。点击“删除”会移除当前事件，不操作或关闭确认框
不会上传。用户同意上报时会升级同一个 WorkManager 唯一任务，避免两个 Worker 并发处理同一批事件。

UI_BLOCK 降噪约定：监控只在应用前台运行；BK 因包含 3 秒自定义启动页，每次冷启动或从后台
恢复后跳过前 5 秒（公共库默认 2 秒）；只记录
500ms 以上且小于 5 秒的主线程帧间隔；同一前台会话最多 5 条，并使用 60 秒冷却。
因此后台停留、启动首帧和达到 ANR 阈值的阻塞都不会产生额外的 `UI_BLOCK`。

ANR 降噪约定：Watchdog 只在应用前台运行；进入后台会取消定时检查并重置主线程 heartbeat；
每次冷启动或从后台恢复后设置 5 秒宽限期，再建立新的检测基线。后台停留时间不计入阻塞时长，
所以后台停留超过 5 秒再恢复不会产生 `ANR`；宽限期结束后真实阻塞主线程超过 5 秒仍会记录。

## 当前验证

```bash
./gradlew :app:assembleDebug --offline --no-daemon
./gradlew :app:testDebugUnitTest --offline --no-daemon
```

上述 Debug 构建和单元测试已通过。真机还需验证：

1. 点击“写入 Crash”或“写入 ANR”，确认事件保持 `PENDING`，Supabase 不新增记录；
2. 点击“提交模拟事件”，再在确认框点击“上报”，确认状态变为 `REPORTED` 且 Supabase 出现记录；
3. 点击“写入 UI_BLOCK”，确认仍会自动上传；
4. 点击“测试崩溃”，重启应用后确认崩溃弹窗的“上报/删除/稍后处理”；
5. 应用进入后台后等待一段时间再恢复，确认没有新增 `UI_BLOCK`；
6. 应用在后台停留至少 30 秒再恢复，确认没有新增 `ANR`；
7. 启动或恢复 5 秒后点击“测试卡顿”，确认只生成一条约 650ms 的 `UI_BLOCK`；
8. 点击“测试 ANR”，确认生成 `ANR` 且不重复生成 `UI_BLOCK`；
9. 使用 Release 构建再次确认 R8 和配置缺失时不会崩溃。
