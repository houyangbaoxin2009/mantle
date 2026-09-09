# Mantle / 地幔

NeoForge 生态的模组化并发服务器 —— **clean-room 自研**：受 **Folia**（PaperMC）region
分区多线程调度**设计思想启发**（Knot / Region / Entity 调度器模型），**不拷贝其任何代码**
（Folia 为 GPL-3.0，本仓库不引用）；以 **NeoForge 为宿主**承载模组生态（**FML 仅作外部依赖，
不并入本仓库源码**）；支持 **Paper 插件 / Fabric 模组 / Forge 模组**（宿主 + 适配层模型，见下）；
内置**增强互联**：玩家网络层增强通道（tink v2 帧 + tsha1f 强校验 + zd 载荷 + x25519/AEAD）、
跨线程调度通信、服务端间 P2P 互联（可配置开关）。

与同谱系项目：**Subterra**（模组框架，运行于 NeoForge，本服务器承载其运行）、
**Toterra**（主模组）、**tie / tink / zd**（语言与网络协议生态）。

## Ecosystem support / 生态支持（宿主 + 适配层模型）

四个生态互不直接共存（Fabric / Forge 与 NeoForge 都改造原版内部），因此 Mantle 只以
**NeoForge 为宿主**，其余生态经适配层接入：

| 生态 | 路线 | 许可 |
|------|------|------|
| Paper 插件 | 依赖 **Paper API**（MIT，可直连引用）并提供 region 线程模型兼容的调度执行层（clean-room） | MIT 依赖 / 自研 TPL |
| NeoForge 模组 | 宿主加载链：FML（fancymodloader）外部依赖驱动 mod 装载，不并源码 | LGPL-2.1 外部依赖 |
| Fabric 模组 | 适配层：Connector 式兼容桥（Fabric Loader / Fabric API 外部依赖；机制参照 Sinytra Connector） | Apache-2.0 外部依赖 / MIT 参照 |
| Forge 模组 | 摸底仲裁：1.20.2+ Forge 与 NeoForge 分流后兼容桥面大，可能需限版本窗口或桥层取舍 | LGPL-2.1 外部依赖 |

## Status

骨架初始化完成，尚未引入代码。规划（草案，编号 p 轨）：

* p.0.1 —— 摸底与定策：tie 并发能力现状（actor 1:1 / **trm-lite preview.3** Go 式 M:N 常驻池：
  spawn + channel + WaitGroup + 协作抢占 + race 纪律 S1–S4）与语言间 ABI（Java FFM → tie
  任务/通道；线程亲和）、NeoForge 模组装载链桥（外部依赖，不并源码）、**生态支持矩阵**
  （Fabric 适配层可行性、Forge 路线仲裁）、通信三件套与 **Subterra engine.network**
  （tink v2 / tsha1f / zd / x25519+AEAD，规范同源）对接点、许可与合规清单
* p.0.2 —— 工程骨架：构建体系、能开服、确定性开服门禁（事件驱动 marker，沿用 Subterra 范式）
* p.0.3 —— **region 调度自研（clean-room，tie 原生）**：以 trm-lite 为底座（region tick = spawn
  任务、跨 region 消息 = channel、生命周期 = WaitGroup；actor 作强隔离备选）+ 确定性探针
* p.0.4 —— 通信三件套：玩家增强通道 / 跨线程调度通信 / P2P 互联（可配置）
* p.0.5 —— 验收与发布基座：全量门禁 + 合规声明清单

## License / 许可

* 本仓库**整体以 Tie Public License v2.0（TPL 2.0）对外许可**，见 [LICENSE](LICENSE)。
  自研代码（含 clean-room 实现的 region 调度层）统一适用 TPL 条款（TPL 为宽松署名制许可，
  详见正文 Section 4）。
* **不引入任何 Folia 代码**：Folia 为 GPL-3.0，本仓库仅借鉴其公开设计思想、独立实现，
  无 copyleft 传染（见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)）。
* Paper API（MIT）与 NeoForge / Fabric / Forge 相关组件均作**外部依赖**引用，不并入本仓库源码
  （各依其许可，见 NOTICES）。
* 分发与运行另需遵守 Minecraft EULA。