$ErrorActionPreference = 'Stop'
foreach ($name in @('SIGNING_KEYSTORE','SIGNING_ALIAS','SIGNING_STORE_PASSWORD','SIGNING_KEY_PASSWORD')) { if (![Environment]::GetEnvironmentVariable($name)) { throw "Set $name locally before signing. Existing app upgrades require the original signing key." } }
$ErrorActionPreference = 'Stop'
$sdkTask = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$buildToolsTask = Join-Path $sdkTask 'build-tools\36.0.0'
$jdkTask = $env:JAVA_HOME
if (!$jdkTask) { throw 'Set JAVA_HOME to your JDK installation' }
$env:JAVA_HOME = $jdkTask
$env:SKIP_JDK_VERSION_CHECK = '1'
$env:JAVA_OPTS = '--enable-native-access=ALL-UNNAMED'
function CheckBuild([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed ($LASTEXITCODE)" } }
Push-Location $PSScriptRoot
try {
 New-Item -ItemType Directory -Force -Path work\classes,work\dex,signing,releases\v0.22.0 | Out-Null
 & "$buildToolsTask\aapt.exe" package -f -m -M AndroidManifest.xml -I "$sdkTask\platforms\android-36\android.jar" -S res -J src
 CheckBuild 'Generate resources'
 & "$jdkTask\bin\javac.exe" --release 8 -classpath "$sdkTask\platforms\android-36\android.jar" -d work\classes (Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' | ForEach-Object FullName)
 CheckBuild 'Compile Java'
 & "$buildToolsTask\d8.bat" --lib "$sdkTask\platforms\android-36\android.jar" --min-api 26 --output work\dex (Get-ChildItem -LiteralPath work\classes -Recurse -Filter '*.class' | ForEach-Object FullName)
 CheckBuild 'Compile DEX'
 & "$buildToolsTask\aapt.exe" package -f -M AndroidManifest.xml -I "$sdkTask\platforms\android-36\android.jar" -S res -A assets -F work\unsigned.apk
 CheckBuild 'Package APK'
 Push-Location work\dex
 try { & "$buildToolsTask\aapt.exe" add -f '..\unsigned.apk' classes.dex; CheckBuild 'Add DEX' } finally { Pop-Location }
 & "$buildToolsTask\zipalign.exe" -f -p 4 work\unsigned.apk work\aligned.apk
 CheckBuild 'Align APK'
 & "$buildToolsTask\apksigner.bat" sign --ks $env:SIGNING_KEYSTORE --ks-key-alias $env:SIGNING_ALIAS --ks-pass env:SIGNING_STORE_PASSWORD --key-pass env:SIGNING_KEY_PASSWORD --out releases\v0.22.0\taskvilla-v0.22.0.apk work\aligned.apk
 CheckBuild 'Sign APK'
 & "$buildToolsTask\apksigner.bat" verify --verbose --print-certs releases\v0.22.0\taskvilla-v0.22.0.apk
 CheckBuild 'Verify APK'
} finally { Pop-Location }
