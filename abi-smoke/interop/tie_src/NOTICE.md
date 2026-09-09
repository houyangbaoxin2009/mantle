# tie_src —— vendored tie-main std（p.0.1.4 互通探针的可移植构建镜像）
# EN: tie_src — vendored tie-main std (portable build mirror for the p.0.1.4 interop probe)

> 许可 / License：以下文件逐字镜像 **tie-main 标准库（tie 语言标准库）**，许可为
> **TPL 2.0**（见 Mantle 根 `LICENSE` 与 tie-main 根 `LICENSE`）。同许可证文件在探针内
> 复制使用，保持探针可移植（免跨仓库 import）。它们是**权威只读参考**，探针构建/运行
> 会逐字消费它们；本目录仅作本地镜像，不做任何修改。
>
> These files are a verbatim local mirror of the **tie-main Standard Library**
> (TIE Programming Language std), distributed under **TPL 2.0** (see Mantle `LICENSE`
> and tie-main `LICENSE`). Same-license std files are copied here so the probe stays
> portable (no cross-repo import). They are the authoritative read-only reference and
> are consumed verbatim; this directory only mirrors them and changes nothing.

source: `F:\Projects\tie-repo\tie-main` (branch main), reused with permission under TPL 2.0.

## 镜像清单 / Mirrored set

命名空间 `/` 相对布局镜像 tie-main 的 `std/` + `rdu/`（`std/tink.tie` 引用
`../rdu/crc.tie`）：

* `std/tink_v2.tie`   —— tink 帧协议 v2（命名空间 `tink2`）：`frame_encode` /
  `frame_next`（本探针直接调用）
* `std/tsha1.tie`     —— TSHA1 v2 哈希族（命名空间 `tsha`）：`tsha1f` / `tsha1_digest`
* `std/tsha1_w48.tie` —— tsha1 n=48 W 特化内核（tsha1 的编译期依赖）
* `std/base48.tie`    —— base-48 编码（tsha1 的编译期依赖）
* `std/bytes.tie`     —— 字节表工具（tink_v2 依赖：`bytes.concat` / `bytes.from_ascii`）
* `std/tink.tie`      —— v1 帧 + CRC32（tink_v2 的 v1 兼容读依赖）
* `std/x25519.tie`    —— x25519（tink_v2 的加密位依赖；仅编译期需要符号）
* `std/bigint.tie`    —— 大整数（x25519 依赖）
* `std/hkdf.tie`      —— HKDF（tink_v2 加密位依赖）
* `std/hmac.tie`      —— HMAC（hkdf 依赖）
* `std/ascon_mac.tie` —— ascon MAC（tink_v2 加密位依赖）
* `rdu/crc.tie`       —— CRC32-IEEE（tink 依赖）

> 注：本地不需要 tie-main 的 `.a` 归档（`base48.a` / `tsha1.a` / `x25519.a`），探针
> 仅用 `.tie` 源即可完整编译。镜像特意保持精简。
>
> Note: the tie-main `.a` archives are not required; the probe compiles from the
> `.tie` sources alone. The mirror is intentionally kept minimal.