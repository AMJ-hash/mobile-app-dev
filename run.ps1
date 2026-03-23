$GRADLE_VERSION = "8.5"
$GRADLE_URL = "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
$INSTALL_DIR = "$env:USERPROFILE\.gradle-installs"
$GRADLE_HOME = "$INSTALL_DIR\gradle-$GRADLE_VERSION"
$GRADLE_EXE = "$GRADLE_HOME\bin\gradle.bat"
$JAR_FILE = "GradeOS.jar"

Write-Host "GradeOS - Student Grade Calculator" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path "build.gradle.kts")) {
    Write-Host "ERROR: Run this script from inside the GradeCalculatorKotlin folder." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Checking Java..." -ForegroundColor Yellow
try {
    $jv = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "Java OK: $jv" -ForegroundColor Green
} catch {
    Write-Host "Java not found! Install Java 17 from https://adoptium.net/" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (Test-Path $JAR_FILE) {
    Write-Host "GradeOS.jar found. Launching..." -ForegroundColor Green
} else {
    Write-Host "First run: need to build GradeOS.jar (takes 5-10 min, only once)" -ForegroundColor Yellow

    if (-not (Test-Path $GRADLE_EXE)) {
        Write-Host "Downloading Gradle $GRADLE_VERSION (~120 MB)..." -ForegroundColor Yellow
        New-Item -ItemType Directory -Force -Path $INSTALL_DIR | Out-Null
        $zipPath = "$INSTALL_DIR\gradle-$GRADLE_VERSION-bin.zip"
        try {
            $wc = New-Object System.Net.WebClient
            $wc.DownloadFile($GRADLE_URL, $zipPath)
            Expand-Archive -Path $zipPath -DestinationPath $INSTALL_DIR -Force
            Remove-Item $zipPath
            Write-Host "Gradle ready." -ForegroundColor Green
        } catch {
            Write-Host "Download failed: $_" -ForegroundColor Red
            Read-Host "Press Enter to exit"
            exit 1
        }
    }

    Write-Host "Building GradeOS.jar..." -ForegroundColor Yellow
    Write-Host "(Downloading ~200MB dependencies, please wait...)" -ForegroundColor Gray
    & "$GRADLE_EXE" fatJar --project-dir "."
    if (-not (Test-Path $JAR_FILE)) {
        Write-Host "Build failed. See errors above." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "Build complete! GradeOS.jar created." -ForegroundColor Green
    Write-Host "Next time you can run it directly with: java -jar GradeOS.jar" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Type 1 for GUI, 2 for Console" -ForegroundColor White
$choice = Read-Host "Choice"

if ($choice -eq "2") {
    java -jar $JAR_FILE --console
} else {
    java -jar $JAR_FILE
}
