$ErrorActionPreference = "Stop"

# Prefer a local Gradle installation/cache, then fall back to Gradle Wrapper.
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if ($env:GRADLE_HOME) {
    $gradle = Join-Path $env:GRADLE_HOME "bin/gradle.bat"
    if (Test-Path $gradle) {
        & $gradle @args
        exit $LASTEXITCODE
    }
}

$distRoot = Join-Path $env:USERPROFILE ".gradle/wrapper/dists/gradle-8.9-bin"
if (Test-Path $distRoot) {
    $cached = Get-ChildItem $distRoot -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName "gradle-8.9/bin/gradle.bat" } |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1
    if ($cached) {
        & $cached @args
        exit $LASTEXITCODE
    }
}

$pathGradle = Get-Command gradle -ErrorAction SilentlyContinue
if ($pathGradle) {
    & $pathGradle.Source @args
    exit $LASTEXITCODE
}

& (Join-Path $projectDir "gradlew.bat") @args
exit $LASTEXITCODE
