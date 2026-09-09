<#
Mantle abi-smoke (p.0.1.2) - tie DLL -> Java 25 FFM build + run.
Pipeline:
  1) tiec --keep-ir abi_smoke.tie -> abi_smoke.opt.ll
  2) manually link trm_lite.a into the DLL (reason: tiec preview.6 shared/DLL mode
     does NOT append g_used_trmlite in driver.link_shared, see compiler/driver.tie
     line 1276; only EXE mode link_exe auto-links trm_lite.a). So the opt IR is
     linked -shared by the bundled clang together with trm_lite.a.
  3) javac FfmAbiProbe.java (JDK 25)
  4) java --enable-native-access=ALL-UNNAMED FfmAbiProbe; exit 0 = green.

Env: TIE_TRM_LITE_LIB points to trm_lite.a (optional; falls back to a local path).
tiec locates its bundled LLVM; no TIE_LLVM_HOME needed.
Exit: 0 = PASS; non-zero = FAIL.
#>
$ErrorActionPreference = "Continue"   # native stderr warnings must not abort; check $LASTEXITCODE explicitly
$Dir = $PSScriptRoot

# ---- tool discovery ----
$tiec = "F:\Projects\tie-repo\tie-main\dist\tie-Harbor-2026.1-preview.6\bin\tiec.exe"
if (-not (Test-Path $tiec) -and $env:TIE_HOME) {
    $tiec = Join-Path $env:TIE_HOME 'bin\tiec.exe'
}
if (-not $tiec -or -not (Test-Path $tiec)) {
    Write-Host "FAIL: tiec (preview.6 dist) not found" -ForegroundColor Red; exit 1
}

# bundled LLVM: <tiec-bin>\llvm\bin
$tiecBin = Split-Path $tiec -Parent
$clang = Join-Path (Join-Path $tiecBin 'llvm') 'bin\clang.exe'
if (-not (Test-Path $clang)) { $clang = 'clang' }

$trmLite = $env:TIE_TRM_LITE_LIB
if (-not $trmLite -or -not (Test-Path $trmLite)) { $trmLite = "F:\Projects\tie-repo\trm-lite\trm_lite.a" }
if (-not (Test-Path $trmLite)) { Write-Host "FAIL: trm_lite.a not found (TIE_TRM_LITE_LIB)" -ForegroundColor Red; exit 1 }

$javac = 'javac'; $javaExe = 'java'
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
}

Write-Host "tiec      : $tiec"
Write-Host "clang     : $clang"
Write-Host "trm_lite.a: $trmLite"

Push-Location $Dir
try {
    Write-Host "=== 1. tiec --keep-ir -> abi_smoke.opt.ll ==="
    Remove-Item "$Dir\abi_smoke.opt.ll", "$Dir\abi_smoke.dll" -ErrorAction SilentlyContinue
    Remove-Item "$Dir\abi_smoke.exp", "$Dir\abi_smoke.lib", "$Dir\FfmAbiProbe.class" -ErrorAction SilentlyContinue
    $env:TIE_TRM_LITE_LIB = $trmLite
    & $tiec "$Dir\abi_smoke.tie" --keep-ir -o "$Dir\abi_smoke.dll" 2>&1 | Out-Host
    # IR is the real product of this pass (the auto-link failure below is expected and ignored)
    if (-not (Test-Path "$Dir\abi_smoke.opt.ll")) { Write-Host "FAIL: IR not produced" -ForegroundColor Red; exit 1 }

    Write-Host "=== 2. manual -shared link trm_lite.a -> abi_smoke.dll ==="
    Remove-Item "$Dir\abi_smoke.dll" -ErrorAction SilentlyContinue
    & $clang -shared "$Dir\abi_smoke.opt.ll" $trmLite -o "$Dir\abi_smoke.dll" 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path "$Dir\abi_smoke.dll")) {
        Write-Host "FAIL: DLL link failed" -ForegroundColor Red; exit 1
    }

    Write-Host "=== 3. javac FfmAbiProbe ==="
    & $javac "$Dir\FfmAbiProbe.java" 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { Write-Host "FAIL: javac failed" -ForegroundColor Red; exit 1 }

    Write-Host "=== 4. java --enable-native-access=ALL-UNNAMED ==="
    & $javaExe --enable-native-access=ALL-UNNAMED FfmAbiProbe "$Dir\abi_smoke.dll"
    $rc = $LASTEXITCODE
    if ($rc -ne 0) { Write-Host "FAIL: probe rc=$rc" -ForegroundColor Red; exit $rc }
    Write-Host "=== abi-smoke PASS (status 0) ==="
} finally { Pop-Location }