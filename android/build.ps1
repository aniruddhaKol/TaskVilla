$ErrorActionPreference = 'Stop'
# Author: Aniruddha DuttaChowdhury ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â· ADChowdhury
$sdkTask = if (Test-Path -LiteralPath "$env:ANDROID_SDK_ROOT\build-tools\36.0.0\aapt.exe") { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$buildToolsTask = Join-Path $sdkTask 'build-tools\36.0.0'
$jdkTask = 'C:\jdk-27'
$env:JAVA_HOME = $jdkTask
$env:SKIP_JDK_VERSION_CHECK = '1'
$env:JAVA_OPTS = '--enable-native-access=ALL-UNNAMED'
function CheckBuild([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed ($LASTEXITCODE)" } }
Push-Location $PSScriptRoot
try {
 New-Item -ItemType Directory -Force -Path work\classes,work\dex,signing,releases\v0.58.26 | Out-Null
 if (!(Test-Path -LiteralPath 'signing\taskvilla-review.keystore')) {
  & "$jdkTask\bin\keytool.exe" -genkeypair -keystore 'signing\taskvilla-review.keystore' -alias taskvilla-review -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname 'CN=Aniruddha DuttaChowdhury, OU=ADChowdhury, O=ADChowdhury, C=IN'
  CheckBuild 'Create review signing key'
 }
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
 & "$buildToolsTask\apksigner.bat" sign --ks signing\taskvilla-review.keystore --ks-key-alias taskvilla-review --ks-pass pass:android --key-pass pass:android --out releases\v0.58.26\taskvilla-v0.58.26.apk work\aligned.apk
 CheckBuild 'Sign APK'
 & "$buildToolsTask\apksigner.bat" verify --verbose --print-certs releases\v0.58.26\taskvilla-v0.58.26.apk
 CheckBuild 'Verify APK'
} finally { Pop-Location }
