# 手动运行此脚本才注册计划任务；不会执行备份，也不会覆盖已有同名任务。
[CmdletBinding()]
param([string]$At = '02:30')
$ErrorActionPreference = 'Stop'
$taskName = 'ThingsBoard-Production-Database-Backup'
$scriptPath = Join-Path $PSScriptRoot 'Backup-ThingsBoard.ps1'
if (-not (Test-Path -LiteralPath $scriptPath)) { throw '找不到备份脚本。' }
if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'pgpass.conf'))) { throw '请先配置 pgpass.conf。' }
if (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue) {
    throw '同名任务已存在，请先检查现有任务；本脚本不覆盖。'
}
$runTime = [datetime]::ParseExact($At, 'HH:mm', [Globalization.CultureInfo]::InvariantCulture)
$powershellExe = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$action = New-ScheduledTaskAction -Execute $powershellExe -Argument ('-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}"' -f $scriptPath) -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -Daily -At $runTime
# SYSTEM 不依赖用户登录；使用显式 pgpass 文件，避免依赖用户配置目录。
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit ([TimeSpan]::Zero)
Register-ScheduledTask -TaskName $taskName -Description '每天备份 thingsboardpe，保留最近60份成功归档。' -Action $action -Trigger $trigger -Principal $principal -Settings $settings | Out-Null
Write-Output "已注册 $taskName，每天 $At 按服务器本地时间运行。未立即启动备份。"
