# Mantle p.0.1 摸底与定策 · 单会话交接提示词
# EN: Mantle p.0.1 Recon & Decision — one-session handoff prompt

## 0. 角色与目标 / Role & Goal

你是 Mantle（NeoForge 生态模组化并发服务器，仓库 F:\Projects\Toterra-Repo\Mantle，
整体许可 **TPL 2.0**）的开发代理。本提示词完成 **p.0.1 摸底与定策**：只读调研 +
最小可执行验证，产出「技术决策文档 + 最小 ABI 冒烟探针」，不写业务功能。
执行模式与 Subterra 一致：小任务逐个提交（English message）、全部完成后统一 review+清理+推送
（GitHub origin only，禁 git.franj2.top）；编号 **p.0.1.x**（≤三级），禁止「阶段X / 第N块」表述。

EN: You are the dev agent for Mantle (NeoForge-ecosystem threaded server, TPL 2.0). This prompt
completes p.0.1 recon & decisions: read-only research + minimal executable proof, delivering a
decision document and a minimal ABI smoke probe. Same discipline as Subterra: per-task commits,
single wrap-up review/cleanup/push to GitHub origin only.

## 1. 项目上下文 / Context（必读 / must read）

* 仓库根：F:\Projects\Toterra-Repo\Mantle（当前仅骨架：README / LICENSE / THIRD-PARTY-NOTICES.md / .gitignore）。
* **定位（已定稿，不得改动）**：Mantle = **clean-room 服务端**——不引入任何 Folia 代码
  （Folia 为 GPL-3.0，本仓库 cite 其设计思想、独立实现，无 copyleft 传染）；以
  **NeoForge 为宿主**承载模组（FML 等**仅外部依赖、不并源码**）；支持 **Paper 插件 / Fabric 模组 /
  Forge 模组**（宿主 + 适配层模型）；内置**增强互联**三件套（玩家网络层增强通道 / 跨线程调度通信 /
  服务端间 P2P 可配置）。
* **tie 并发现状（已核实，2026-09-09）**：
  * actor：语言原生，**1:1 OS 线程 + mailbox 串行**（免锁）；tests/m6_actor 全套已落地。
  * **trm-lite**（独立仓库 F:\Projects\tie-repo\trm-lite，master，**TPL 2.0**）：Go 式 **M:N 常驻线程池**，
    已到 **preview.3**（p.6.7 全落地：spawn 真并行、双形态池 + work-stealing、per-P 细锁、协作抢占
    gosched+时间片、WaitGroup、Go 式 channel close 广播 + select 多路、全量并发安全、生命周期确定性
    链上临时释放）；当前 p.6.10（tl_tbl refcount API）推进中；race 纪律见其
    docs/superpowers/specs/2026-09-03-trm-lite-p67-parallel-design.md §8（S1 原子计数 / S2 独立槽位 /
    S3 锁保护复合态 / S4 运行时容器安全；U1–U6 不安全模式；并发断言禁墙钟、主证 = tid 去重 ≥ 2）。
  * 语言间桥先例：Subterra p.2.1 tiec → DLL → Java 25 FFM 直调（导出 `<ns>$<func>`，标量参数），
    TieBridgeProbe 已验证 —— 但**起 spawn/任务、channel 收发、句柄生命周期、线程亲和**的 Java 侧接入
    ABI 形态未验证（trm-lite 待决项）。
* **通信栈复用源**：Subterra（F:\Projects\Toterra-Repo\Subterra）p.2.4 engine.network ——
  FrameV1/FrameV2（tink v2 帧 + tsha1f 强校验 + v1 兼容）、Tsha1f（金标向量一致）、StrategySelector
  三级载荷、BandwidthOptimizer、SecureChannel/EncryptionConfig（x25519+AEAD，默认开）；tie-main std
  有同规范实现（tink_v2.tie / tsha1.tie / x25519.tie / hkdf.tie）。Mantle 侧要么复用规范（Java），
  要么直接 tie 原生实现，**规范同源、金标向量互通**（p.0.1 需定取舍并验证一帧跨语言互通）。
* 生态适配先例：插件 = Paper API（MIT 依赖）；Fabric = Connector 式桥（Sinytra Connector，MIT 参照，
  机制借鉴不照抄）；Forge = 1.20.2+ 与 NeoForge 分裂，**路线待仲裁**（限版本窗口 / 桥层取舍 / 暂不承诺）。
* 分层：Mantle 主体 tie 优先（能 tie 就 tie）；Java 壳只保「MC 内部补丁 / mixin（改主循环、tick、
  区块）」与「FML/生态加载桥」两类绕不开的层；FFM 桥为 bone（p.2.1 先例）。

## 2. 本次范围 / Scope（p.0.1.x 只读调研 + ABI 冒烟）

* 目标产物：`docs/plans/p0.1-decision.tmd`(或 .md) 双语决策文档 + 最小 ABI 冒烟探针全绿。每子项独立提交。

1. **p.0.1.1 tie 并发能力摸底（只读）**：核定 trm-lite 当前能力全集（spawn/channel/WaitGroup/抢占/
   窃取/per-P 锁/生命周期）与限制清单（已知限制、无 GC、fn() 原子任务体、非可增长栈、syscall 不出借）；
   actor 适用范围界定。产出「region 调度 = trm-lite 底座」的能力映射表（region tick=spawn 任务、
   跨 region 消息=channel、生命周期=WaitGroup；actor 备选强隔离）。
2. **p.0.1.2 语言间 ABI 冒烟（最小可执行闭环）**：tie DLL（trm-lite 栈）→ Java 25 FFM 直调：
   spawn 一个任务 + channel 发/收 + WaitGroup 等齐，java main 里验证 tid 去重 ≥ 2 且数据正确
   （确定性探针，禁墙钟）。验证线程亲和/句柄生命周期（析构语义）。失败路径计数只自增。
3. **p.0.1.3 生态加载桥摸底（只读 + 结论）**：NeoForge FML 作为外部依赖的最小可行面——需要哪些
   vanilla 内部接缝（注册表/事件点/tick 挂载点）被 Mantle 的 clear-room 改造触及；Fabric Connector 式
   桥可行性；**Forge 路线仲裁**（给出选项 + 建议，标记为决策项交主代理/用户）。产出接缝清单。
4. **p.0.1.4 通信栈取舍与跨语言互通（只读 + 一帧验证）**：engine.network（Java）与 tie std（tink_v2 等）
   的规范同源性核对；定 Mantle 侧实现语言取舍（Java 复用 or tie 原生 or 双栈互通）；**最小互通验证**：
   同一 payload 一帧 Java 编码 → tie 解析（或反向），tsha1f 金标向量两侧一致。
5. **p.0.1.5 生态支持矩阵 + 许可合规清单（只读 + 文档）**：四生态支持矩阵（含 Forge 仲裁输入）终稿；
   许可合规：TPL 2.0 整体（无 Folia 代码事实核查、NOTICES 与声明路径）、LGPL-2.1/Apache-2.0/MIT
   依赖边界（外部依赖不并库）、Minecraft EULA 提示。产出合规清单条目。

* 收尾：决策文档合并评审 + 全部子项完成后统一 review+清理+push（GitHub origin only）。

## 3. 子代理纪律 / Sub-agent discipline

* 大任务用子代理：只读调研用 Explore / 浅代理；ABI 冒烟（p.0.1.2/4）用实现代理；同文件同一时刻
  仅一子代理编辑；编译/API 依赖时严格串行（ABI 冒烟依赖 trm-lite 可编译产物）。
* 每小任务完成即提交一次并报告一句；全部完成后收尾子代理统一 review+清理+push。
* 主代理 trust but verify：关键改动（build.gradle/探针/注入核心）读实际文件核对，必要时重跑探针。

## 4. 纪律（硬性） / Discipline (hard)

* 小任务逐个提交；English commit；作者本地 jiro；**只推 GitHub**（禁 git.franj2.top）。
* 版本 p.0.1.x 最多三级；禁「阶段X / 第N块 / block N」；文档双语。
* 性能纪律：禁 O(n²)；热路径先建模；注入只在装载窗口，不阻塞主线程热路径。
* 确定性：验收一律确定性探针；并发断言沿用 trm-lite race 纪律（主证 = tid 去重 ≥ 2，禁墙钟时序）；
  失败计数只在失败路径自增；断言前先做前置动作。
* 编译纪律：改探针源后必须重编；FFM 探针 JavaExec 需 `--enable-native-access=ALL-UNNAMED`。
* 文件编辑：同文件连续编辑串行单发、改后读文件核对；同文件禁止并行多 Edit。
* 许可：**绝不引入 Folia/GPL-3 代码**（clean-room 纪律）；LGPL/Apache/MIT 一律外部依赖不并库；
  移植他人实现须先 javap/阅读核实，存疑按不允许处理。
* 铁律接线：若 p.0.1 引入 Java ABI 测试，落在仓库独立模块（如 `abi-smoke/`），不污染服务器主体。

## 5. 验收门 / Acceptance

* 决策文档（双语）覆盖全部三项决策：region 调度 = trm-lite 底座的最终建议、通信栈语言取舍、
  Forge 路线仲裁输入。
* ABI 冒烟探针全绿（exit 0；tid 去重 ≥ 2；channel/WaitGroup 数据正确；失败路径计数单调）。
* 跨语言一帧互通验证通过（tsha1f 金标向量两侧一致）。
* 合规清单条目齐备（无 GPL-3 引入、外部依赖边界清晰、NOTICES 声明路径可用）。

## 6. 关键路径 / Key paths

* Mantle 工作区：F:\Projects\Toterra-Repo\Mantle；决策文档建议 docs/plans/。
* trm-lite 参考：F:\Projects\tie-repo\trm-lite（master，preview.3+，TPL 2.0）+
  docs/superpowers/specs/2026-09-03-trm-lite-p67-parallel-design.md §8。
* tie-main 参考：F:\Projects\tie-repo\tie-main（std/tink_v2.tie、tsha1.tie、x25519.tie）；
  Subterra 参考：F:\Projects\Toterra-Repo\Subterra（engine.network 与 TieBridgeProbe 先例）。
* Folia 设计参考（仅查看公开设计文档/REGION_LOGIC.md 层次，不取代码）：github.com/PaperMC/Folia。

## 7. 输出与汇报 / Output & Report

* 最终汇报：决策文档路径 + 三决策结论 + ABI 探针/互通验证全绿证据 + 子项提交清单 + push 结果。