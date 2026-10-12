$ErrorActionPreference='Stop'
# Aniruddha DuttaChowdhury · ADChowdhury. Background emulator for app review.
$taskEmulator=Join-Path $env:LOCALAPPDATA 'Android/Sdk/emulator/emulator.exe'
$taskLogs=Join-Path $PSScriptRoot 'work'
New-Item -ItemType Directory -Force -Path $taskLogs | Out-Null
Start-Process -FilePath $taskEmulator -ArgumentList @('-avd','Pixel_8_API_36','-no-window','-no-boot-anim','-gpu','swiftshader') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $taskLogs 'emulator.stdout.log') -RedirectStandardError (Join-Path $taskLogs 'emulator.stderr.log')
Write-Output 'Started Pixel_8_API_36 in the background; no emulator window.'
