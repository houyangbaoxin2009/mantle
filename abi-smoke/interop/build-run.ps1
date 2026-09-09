# ============================================================
# build-run.ps1 —— p.0.1.4 跨语言 tink-v2 一帧互通探针（构建+运行）
# EN: build-run.ps1 — p.0.1.4 cross-language tink-v2 one-frame interop probe (build+run)
#
# 1) 编译 tie 探针（probe.tie → probe.exe，经 tiec）
# 2) 编译 Java 参考实现（FfmInteropProbe.java → .class）
# 3) 运行 Java：双向（Java→tie / tie→Java）一帧互通 + 金向量自证
# 退出码：0 = 全绿；非 0 = 失败。要求已安装 java / javac 且 tiec 可用。
# EN: exit 0 = all green; non-zero = failure. Requires java/javac on PATH and a tiec.
#
# tiec 定位 / tiec locating：优先 $env:TIEC，否则回退到 tie-main 官方 preview.6 二进制。
# (TIEC env var first; else fallback to the tie-main official preview.6 binary.)
# ============================================================
param(
    [string]$Tiec = ""
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot                      # interop/
$tieSide = Join-Path $root "tie_side"
$probeTie = Join-Path $tieSide "probe.tie"
$probeExe = Join-Path $tieSide "probe.exe"
$javaSrc = Join-Path $root "FfmInteropProbe.java"

if (-not (Test-Path $probeTie)) { Write-Error "missing probe.tie: $probeTie" }
if (-not (Test-Path $javaSrc))  { Write-Error "missing FfmInteropProbe.java: $javaSrc" }

# ---- 定位 tiec ----
if ([string]::IsNullOrEmpty($Tiec)) { $Tiec = $env:TIEC }
if ([string]::IsNullOrEmpty($Tiec)) {
    $cand = "F:\Projects\tie-repo\tie-main\dist\tie-Harbor-2026.1-preview.6\bin\tiec.exe"
    if (Test-Path $cand) { $Tiec = $cand } else {
        $c = Get-Command tiec -ErrorAction SilentlyContinue
        if ($c) { $Tiec = $c.Source } else { Write-Error "tiec not found; set -Tiec or env TIEC" }
    }
}
Write-Host "[build-run] tiec = $Tiec"

# ---- java / javac ----
$javac = (Get-Command javac -ErrorAction Stop).Source
$java  = (Get-Command java  -ErrorAction Stop).Source

# ---- 1) 编译 tie 探针 ----
Write-Host "[build-run] compiling tie probe..." -ForegroundColor Cyan
& $Tiec $probeTie -o $probeExe
if ($LASTEXITCODE -ne 0) { Write-Error "tiec failed (exit $LASTEXITCODE)"; exit 1 }
if (-not (Test-Path $probeExe)) { Write-Error "probe.exe not produced"; exit 1 }

# ---- 2) 编译 Java 参考实现 ----
Write-Host "[build-run] compiling FfmInteropProbe.java..." -ForegroundColor Cyan
Push-Location $root
try {
    & $javac (Split-Path $javaSrc -Leaf)
    if ($LASTEXITCODE -ne 0) { Write-Error "javac failed (exit $LASTEXITCODE)"; exit 1 }
} finally { Pop-Location }

# ---- 3) 运行 Java 双向互通探针 ----
Write-Host "[build-run] running interop probe..." -ForegroundColor Cyan
$prop = "tieProbe=" + ($probeExe -replace '\\','/')
& $java ("-D" + $prop) -cp $root FfmInteropProbe
$code = $LASTEXITCODE

Write-Host "------------------------------------------------------"
if ($code -eq 0) {
    Write-Host "[build-run] ALL GREEN (exit 0)" -ForegroundColor Green
} else {
    Write-Host "[build-run] FAILED (exit $code)" -ForegroundColor Red
}
exit $code