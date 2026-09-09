# Mantle / 地幔

NeoForge 生态的模组化并发服务器 —— 分叉自 **Folia**（PaperMC，MIT），继承 region
分区多线程调度（Knot / Region / Entity 调度器 + 跨线程任务通信）；融合 **NeoForge**
模组生态（**仅作外部依赖，不并入本仓库源码**）；内置**增强互联**：
玩家网络层增强通道（tink v2 帧 + tsha1f 强校验 + zd 载荷 + x25519/AEAD）、
跨线程调度通信、服务端间 P2P 互联（可配置开关）。

与同谱系项目：**Subterra**（模组框架，运行于 NeoForge，本服务器承载其运行）、
**Toterra**（主模组）、**tie / tink / zd**（语言与网络协议生态）。

## Status

骨架初始化完成，尚未引入代码。规划（草案，编号 p 轨）：

* p.0.1 —— 摸底与定策：Folia 与 NeoForge 生态差异分析、NeoForge 模组装载链桥（外部依赖，
  不并源码）、通信三件套范围、许可与合规清单
* p.0.2 —— 工程骨架：构建体系、能开服、确定性开服门禁（事件驱动 marker，沿用 Subterra 范式）
* p.0.3 —— region 调度移植：调度器迁移 + 确定性探针
* p.0.4 —— 通信三件套：玩家增强通道 / 跨线程调度通信 / P2P 互联（可配置）
* p.0.5 —— 验收与发布基座：全量门禁 + 合规声明清单

## License / 许可

* 本仓库**整体以 Tie Public License v2.0（TPL 2.0）对外许可**，见 [LICENSE](LICENSE)。
  自研代码与分叉引入的代码统一适用 TPL 条款（TPL 为宽松署名制许可，详见正文 Section 4）。
* 分叉自 Folia 的代码版权归 PaperMC 及贡献者所有：按 MIT 许可的要求，
  **MIT 许可文本与版权声明须随对应文件/目录保留**（见
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)），与 TPL 的整体授予叠加共存。
* NeoForge 相关组件仅作 **LGPL-2.1** 构建期/运行期外部依赖引用，不并入本仓库源码。
* 分发与运行另需遵守 Minecraft EULA。