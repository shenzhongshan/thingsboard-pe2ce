# 每次导出生产数据库，验证归档目录后发布成功文件，仅清理本脚本生成的成功备份。
[CmdletBinding()]
param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbName = 'thingsboardpe',
    [string]$DbUser = 'postgres',
    [ValidateRange(1, 3650)][int]$KeepCount = 60
)

$ErrorActionPreference = 'Stop'
$backupRoot = $PSScriptRoot
$archiveDir = Join-Path $backupRoot 'backups'
$logDir = Join-Path $backupRoot 'logs'
$lockStream = $null
$exitCode = 0
$oldPassFile = $env:PGPASSFILE
$oldPassword = $env:PGPASSWORD
$oldTimeout = $env:PGCONNECT_TIMEOUT

# 日志不记录命令行凭据；数据库密码由独立 pgpass.conf 提供。
function Write-BackupLog([string]$Message) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Add-Content -LiteralPath $script:runLog -Value $line -Encoding UTF8
    Write-Output $line
}

try {
    New-Item -ItemType Directory -Path $archiveDir, $logDir -Force | Out-Null
    $script:runLog = Join-Path $logDir ('backup-' + (Get-Date -Format 'yyyy-MM') + '.log')
    # 文件句柄排他锁可跨不同 Windows 账号阻止并发；进程退出自动释放。
    try {
        $lockStream = [System.IO.File]::Open((Join-Path $backupRoot 'backup.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    } catch [System.IO.IOException] {
        throw '无法获得备份锁，可能已有备份运行或锁文件不可访问。'
    }
    if ($DbName -notmatch '^[A-Za-z0-9_]+$') { throw '数据库名称只能包含字母、数字和下划线。' }
    $dumpExe = Join-Path $PgBin 'pg_dump.exe'
    $restoreExe = Join-Path $PgBin 'pg_restore.exe'
    foreach ($tool in @($dumpExe, $restoreExe)) {
        if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) { throw "找不到 PostgreSQL 工具：$tool" }
    }
    $passFile = Join-Path $backupRoot 'pgpass.conf'
    if (-not (Test-Path -LiteralPath $passFile -PathType Leaf)) { throw '请先配置 database_backup\pgpass.conf。' }
    $env:PGPASSFILE = $passFile
    $env:PGPASSWORD = $null
    $env:PGCONNECT_TIMEOUT = '30'
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $baseName = '{0}-{1}' -f $DbName, $stamp
    $partial = Join-Path $archiveDir ($baseName + '.dump.partial')
    $final = Join-Path $archiveDir ($baseName + '.dump')
    $dumpLog = Join-Path $logDir ($baseName + '-dump.log')
    $verifyLog = Join-Path $logDir ($baseName + '-verify.log')
    Write-BackupLog "开始备份 $DbHost`:$DbPort/$DbName。"
    # --no-password 保证计划任务认证失败时直接退出，不等待交互输入。
    & $dumpExe --host $DbHost --port $DbPort --username $DbUser --dbname $DbName --no-password --format custom --lock-wait-timeout=60000 --file $partial 2> $dumpLog
    if ($LASTEXITCODE -ne 0) { throw "pg_dump 失败，详细日志：$dumpLog" }
    if (-not (Test-Path -LiteralPath $partial) -or (Get-Item -LiteralPath $partial).Length -eq 0) {
        throw '导出文件不存在或为空。'
    }
    & $restoreExe --list $partial 1> $verifyLog 2>> $dumpLog
    if ($LASTEXITCODE -ne 0) { throw "归档目录检查失败，详细日志：$dumpLog" }
    $hash = (Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash
    Move-Item -LiteralPath $partial -Destination $final
    Write-BackupLog "备份成功：$final；SHA256=$hash。目录检查不代替完整恢复验证。"

    # 只匹配本库、本脚本命名的成功 dump；不删除失败 partial 或其他人工备份。
    $pattern = '^' + [regex]::Escape($DbName) + '-\d{8}-\d{6}-\d{3}\.dump$'
    $expired = @(Get-ChildItem -LiteralPath $archiveDir -File |
        Where-Object { $_.Name -match $pattern } |
        Sort-Object Name -Descending | Select-Object -Skip $KeepCount)
    foreach ($item in $expired) {
        Remove-Item -LiteralPath $item.FullName
        Write-BackupLog "已清理超出保留数量的备份：$($item.Name)"
    }
    Write-BackupLog "本次完成，最多保留 $KeepCount 份成功备份。"
} catch {
    $exitCode = 1
    $message = '备份任务失败：' + $_.Exception.Message
    if ($script:runLog) {
        try { Write-BackupLog $message } catch { Write-Warning $message }
    } else { Write-Warning $message }
} finally {
    $env:PGPASSFILE = $oldPassFile
    $env:PGPASSWORD = $oldPassword
    $env:PGCONNECT_TIMEOUT = $oldTimeout
    if ($null -ne $lockStream) { $lockStream.Dispose() }
}
exit $exitCode
