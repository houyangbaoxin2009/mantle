# Third-Party Notices / 第三方声明

本仓库整体以 **Tie Public License v2.0（TPL 2.0）** 对外许可；以下第三方组件随代码实际引入时补齐
对应许可全文与版权声明。所属组件的版权与既有许可条款不因仓库整体许可而受影响。

## Folia (PaperMC) — 版权归 PaperMC，MIT 许可条款保留

Mantle 分叉自 [Folia](https://github.com/PaperMC/Folia)（Copyright (c) PaperMC and
contributors）。Folia 分叉部分代码按 **MIT License** 条款授权使用：按 MIT 第 1 条的规定，其
**许可文本与版权声明须随对应文件/目录保留**；该 MIT 许可与其允许的再授权，以「保留声明」为条件
叠加于仓库整体的 TPL 2.0 授予之下。引入时将 MIT 许可全文放置于对应目录（如 `licenses/`）。

## NeoForge — GNU LGPL v2.1 (external dependency only)

Mantle 通过构建期/运行期依赖引用 NeoForge 组件（模组装载链等相关 artifact），
**不**将 NeoForge 源码并入本仓库。相关 artifact 遵循 LGPL-2.1，见其上游仓库。

## Other

构建产生的传递依赖（如 netty 等）遵循各自上游许可，随构建产物附带。