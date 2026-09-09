# abi-smoke —— Mantle p.0.1.2 语言间 ABI 冒烟（tie DLL → Java 25 FFM）
# EN: abi-smoke — Mantle p.0.1.2 cross-language ABI smoke (tie DLL → Java 25 FFM)

> 证明内容 / What is proven: trm-lite 并发原语（spawn + channel + WaitGroup）被打包成
> 一组「标量/string 边界」的 tie 导出 `pub func`，由 Java 25 通过 `java.lang.foreign` (FFM)
> 加载 `abi_smoke.dll` 并驱动确定性断言（无墙钟时序断言）。
> The probe loads `abi_smoke.dll` from Java 25 FFM and asserts deterministically (no wall-clock).

## 形态选择 / Chosen form: 简单形态（simple-form fallback）

### 为什么放弃复杂形态 / Why NOT complex form (import tl_runtime_ctx) — 开放 ABI 项
p.0.1.1 决策首推复杂形态（`ctx_spawn/ctx_drain` 常驻多 OS 线程池），但这在**动态库模式硬阻塞**：
`import tl_runtime_ctx` 会把 `trm_lite_ws` / `trm_lite_chan` / `trm_lite_wg` / `trm_lite_tgc` 的
`pub func` 一并纳入 DLL 导出面校验，而其中 `trm_lite_ws::spawn` 的参数类型是 `fn() -> i64`
（函数值），不属于可跨动态库边界类型。tiec 在 DLL 模式下直接拒绝（实测错误）：

```
IR 生成失败: 动态库边界错误: 导出函数 trm_lite_ws::spawn 参数类型 'fn() -> i64'
不能跨动态库边界（仅标量/string/slice/repr(C) pod struct 可跨库）
```

因此本探针落地为**简单形态回退**（tiec 内置 spawn/yield + ch_* + wg_*），并如实把
「复杂形态注入 DLL」记为**开放 ABI 项**（正是冒烟要暴露的边界）。其后果：**多 OS 线程池
证据（pool_threads ≥ 2）无法经 DLL 证明**——简单形态为协作式单 OS 线程（该函数如实返回 1）。

### 附（第 2 个实测介入）/ Second hard limitation met — 开放 ABI 项
即使简单形态，tiec preview.6 在 `--shared`/DLL 模式也不会自动链接 `trm_lite.a`：
`compiler/driver.tie` 的 `link_shared` 传参是 `(need_interp, need_wsock)`，**漏传
`g_used_trmlite`**（仅 `link_exe` 传了，见第 1231/1276 行）。故此脚本先 `tiec --keep-ir`
生成 `abi_smoke.opt.ll`，再手工 `clang -shared abi_smoke.opt.ll trm_lite.a -o abi_smoke.dll`。
（编译器层面补 `g_used_trmlite` 到 `link_shared` 即可消除该 workaround。）

## 已知限制（preview.3，沿用）/ Known limits met

- **协作抢占、非 OS 硬抢占**：任务在显式 `yield()` 让出点之间不可打断；简单形态为单 OS
  线程 M:N，故 `pool_threads()==1`。确定性并发证据 = 所有 24 个任务都被执行
  （`run(24)==24`、`completed()==24`），而非并行度。
- **channel 消息为标量 i64**；`ch_recv` 关闭且排空返回 **-1**（探针正是用此确定性信号）。
- **tie 既有缺陷**：标量全局初值静默丢弃（本 shim 只写不用初值，无影响）；单行块须 `;`。
- **`wg_count` 并非编译器内建**（tiec 仅内建 `wg_new/wg_add/wg_done/wg_wait`）——本 shim
  以计数镜像 `g_wgcnt` 提供 WaitGroup 计数（唯一调用方即本模块，与真实组同步增减）。
- **tie string 返回跨 DLL 的 FFM 解码布局未被探针验证过**（recon 只实打实走通 i64）——
  故 `abi$version` 以版本码 i64 返回（`0x0003_0000`），string 返回记为开放 ABI 项。

## 运行 / Run

```powershell
cd abi-smoke
powershell -File .\build-run.ps1     # exit 0 = 全绿 / green
```
要求：JDK 25（zulu 25.0.2 验证）；tiec 取 tie-main preview.6 dist（捆绑 LLVM，免
TIE_LLVM_HOME）；`TIE_TRM_LITE_LIB` 指向 `trm-lite/trm_lite.a`（缺省用本机路径）。
构建产物（`abi_smoke.dll/.exp/.lib/.ll/.opt.ll`、`FfmAbiProbe.class`）见 `abi-smoke/.gitignore`，不入仓。

## 导出面 / Export surface（符号 = `abi$<fn>`，全部 i64/标量）

`ensure` `set_workers`（简单形态 no-op=0）`run` `completed` `pool_threads` `wg_new`
`wg_add` `wg_done` `wg_count` `ch_open` `ch_send` `ch_recv` `ch_close` `fail_count` `version`。

## 最近一次全绿证据行 / Evidence lines (last green run)

```
PASS ensure() = 0
PASS run(24) executed count = 24
PASS completed() after run = 24
PASS pool_threads() [simple form == 1] = 1
PASS wg_count(wg) == 0 after done = 0
PASS ch_send(1..5) = 0 x5
PASS ch_recv order -> 1..5 = 1..5
PASS ch_close(ch) = 0
PASS ch_recv on closed&drained == -1 = -1
PASS fail_count() == 0 = 0
PASS version() [code 0x00030000] = 196608
=== FfmAbiProbe PASS (simple-form fallback green) ===
```

> 退出方式：Java 侧用 `Runtime.halt(0/1)` 而非 `System.exit`——trm-lite 可能遗留后台 worker
> 线程，优雅关闭时 JVM 卸载已加载 DLL 会与仍持临界区的原生线程赛跑（0xC0000005）；
> halt 直接终止进程，确定性且全绿。

## 开放 ABI 项 / Open ABI items

1. **复杂形态（import tl_runtime_ctx）注入 DLL** 被导出面校验拒绝（`fn()->i64` 参数）。
2. **tiec --shared 不自动链 trm_lite.a**（`link_shared` 漏传 `g_used_trmlite`）→ 需手工链接。
3. **`wg_count` 非内建** → shim 计数镜像。
4. **tie string 返回跨 FFM 的布局**未验证 → 本探针以 i64 版本码替代。

## 目录 / Layout

* `abi_smoke.tie`       —— tie shim（简单形态，导出面见上；并发全在 tie 内）。
* `FfmAbiProbe.java`    —— 纯 JVM 主类，FFM 加载 DLL + 确定性断言；exit 0=PASS/1=FAIL。
* `build-run.ps1`       —— 一键：tiec --keep-ir → 手工链接 trm_lite.a → javac → java --enable-native-access。
* `interop/`            —— 并列的 p.0.1.4（通信栈 tink_v2 互通探针），独立 README/提交。