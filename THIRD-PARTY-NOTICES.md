# Third-Party Notices / 第三方声明

本仓库整体以 **Tie Public License v2.0（TPL 2.0）** 对外许可；以下第三方组件随代码实际引入时补齐
对应许可全文与版权声明。所属组件的版权与既有许可条款不因仓库整体许可而受影响。

## Folia (PaperMC) — GNU GPL-3.0，**仅设计参考，不拷贝**

Mantle **clean-room 自研**：仅参考 [Folia](https://github.com/PaperMC/Folia) 公开的
region 分区调度**设计思想**（Knot / Region / Entity 调度器模型、跨线程任务语义），
**不引入、不复制其任何源码或补丁**。Folia 本体为 **GPL-3.0**（见其仓库 `PATCHES-LICENSE`）；
因不拷贝其代码，其 copyleft 条款不适用于本仓库，本仓库主体保持 TPL 2.0。

## Paper API — MIT License (external dependency)

Mantle 的 Paper 插件生态层依赖 Paper API 相关 artifact（MIT，可直连引用）；不将源码并入本仓库。
region 线程模型兼容的调度执行层为自研（TPL）。

## NeoForge — GNU LGPL v2.1 (external dependency only)

Mantle 通过构建期/运行期依赖引用 NeoForge 组件（模组装载链等相关 artifact），
**不**将 NeoForge 源码并入本仓库。相关 artifact 遵循 LGPL-2.1，见其上游仓库。

## Fabric Loader / Fabric API — Apache-2.0 (external dependency)

Fabric 适配层引用 Fabric Loader 与（必要时）Fabric API 相关 artifact，**不**将源码并入本仓库；
遵循 Apache License 2.0，见其上游仓库。适配层机制参照 **Sinytra Connector**（MIT License，
实现时按其许可保留声明，不直接拷贝）。

## Forge — GNU LGPL v2.1 (external dependency only)

Forge 路线（如经摸底仲裁采用）同样只作构建期/运行期外部依赖引用，遵守 LGPL-2.1，见其上游仓库。

## Other

构建产生的传递依赖（如 netty 等）遵循各自上游许可，随构建产物附带。