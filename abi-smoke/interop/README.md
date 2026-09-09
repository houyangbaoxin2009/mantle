# abi-smoke/interop —— p.0.1.4 通信栈取舍与跨语言一帧互通探针
# EN: abi-smoke/interop — p.0.1.4 comms-stack trade-off + cross-language one-frame interop probe

## 结论（决策输入）/ Conclusion (decision input, comms-stack trade-off)

**通信栈语言分工 / Comms-stack language split (recommended):**

| 角色 / Role | 载体 / Language | 归属 / Owner | 契约 / Contract |
|---|---|---|---|
| 协议权威 / Protocol authority | tie 标准库 `tink_v2`（`std/tink_v2.tie`） | Mantle（经 tie std） | 字级线格式 + 权威金向量 |
| 同规格参考实现 / Same-spec reference | Java（`engine.network`，Subterra） | Subterra（只读基准） | 与 tie 逐字一致；金向量对等 |
| 互通锚点 / Interop anchor | 本探针（双向） | Mantle（本目录） | 一帧字节级对等 + 金向量互核 |

* **栈取舍**：`std/tink_v2`（tie）作为**协议权威**；Subterra `engine.network`（Java）作为
  **同规格参考实现**。互操作由两条机械约束钉死——(a) 字节级线格式（见下），
  (b) 权威金向量（4 快 + 1 强）。任一语言实现命中即视为对规范的一致，无需逐字维护双份。
* **p.0.4**：`Mantle` 可任选 tie 或 Java 一方（或两者）承载网络帧；本探针证明两条路线
  产出字节完全相同（`dir1` 逐字节一致），故切换代价 ≈ 0。决策矩阵：需要 tie 栈内联
  (LSP/zrpc/桥) → tie；需要纯 JVM 部署 → Java；混合可无缝（tie 编 → Java 解 或反之）。
* 金向量（authoritative 权威，见 `FfmInteropProbe.java` 与 `tie_side/probe.tie`）：
  `tsha1f("",8,48)="5juavlyl"`；`tsha1f("123456789",8,48)="3Kz1piuc"`；
  `tsha1f("abc",8,48)="5FGIxu1J"`；`tsha1f("a"*64,8,48)="5juavlyl"`；
  `digest("f","123456789",48)` 前 64 hex = `260c7340...76bf`。本探针两侧各自自证。

**The tink-v2 frame ABI**（权威 / authoritative，勿改）:
```
[sig 0x74 0x6B][v=2][flags][len u32 BE][ext_len u16 BE][ext TLV][payload][integrity]
flags bit0 = STRONG (1→32B slot / 0→8B slot); reserved bit5..7 = 0
fast  = ASCII(tsha1f(payload,8,48))  (8 字节)
strong= digest(f, payload, 48) hex→bytes 前 32 字节
```

## 运行 / Run

```powershell
./build-run.ps1            # 自动编译 tie+Java 并双向验证；exit 0 = 全绿
```
要求 `java`/`javac` 在 PATH；`tiec` 默认取 tie-main preview.6，可用 `-Tiec <path>` 或
`$env:TIEC` 覆盖。`build-run.ps1` 会重新生成 `tie_side/probe.exe` 与 `FfmInteropProbe*.class`
（两者已在 `.gitignore`，不入仓）。

EN: run `./build-run.ps1` (auto-compile tie+Java, run both interop directions; exit 0 = green).
Requires `java`/`javac`; `tiec` defaults to the tie-main preview.6 binary (override via
`-Tiec` or `$env:TIEC`). Build artifacts (`tie_side/probe.exe`, `*.class`) are gitignored.

## 目录 / Layout

* `tie_side/probe.tie`  —— tie 探针：自证金向量 + `tink_v2.frame_encode`/`frame_next` 行协议
  （`en:` 编码打印 FRAME/FAST；`pa:` 解析打印 ok/iok；`gv` 复打金向量）。 stdin/stdout。
* `FfmInteropProbe.java`——纯 JVM 参考实现驱动：内置 tsha1f f 模型参考 + 快校验帧编解码，
  五条金向量自证，并在两个方向调用 `probe.exe` 完成一帧互通（Java→tie 逐字节一致 + tie→Java
  解析对等）。
* `tie_src/`            —— vendored tie-main std（TPL 2.0 逐字镜像，见 `NOTICE.md`），
  免跨仓库 import，保证探针可移植。探针经相对 import 消费 `std/tink_v2` 与 `std/tsha1`。
* `build-run.ps1`       —— 一键构建+运行（tiec 编 tie → javac 编 Java → 双向验证）。

## 证据行（最近一次全绿）/ Evidence lines (last green run)

```
CHECK:gv:empty:OK  gv:nine:OK  gv:abc:OK  gv:a64:OK  gv:strong:OK
DIR1  Java→tie: tie parses Java frame ok=1 iok=1; tie_tsha1f==java; tie_encode_byte_identical:OK
      frameHex = 746b0200000000100000...3541796c7a304b39  fast=5Aylz0K9
DIR2  tie→Java: parse_ok:OK  payload_eq:OK  tsha1f_match:OK  fast=59p8eKu0
SUMMARY: InteropProbe: PASS   EXIT=0
```
（`JS1` 帧 `interop-frame-01` integrity 末 8 字节 `3541796c7a304b39` = ASCII `5Aylz0K9`，
tie 与 Java 逐字节输出相同。）