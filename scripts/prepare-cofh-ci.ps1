param(
    [Parameter(Mandatory = $true)]
    [string] $Root
)

$ErrorActionPreference = "Stop"

function Update-BuildFile {
    param(
        [string] $Path,
        [string[]] $Excludes,
        [bool] $KeepCuriosApi
    )

    $content = Get-Content -LiteralPath $Path -Raw

    $blockedRepositories = @(
        "maven.tterrag.com",
        "maven.covers1624.net",
        "dvs1.progwml6.com/files/maven",
        "maven.blamejared.com",
        "maven.theillusivec4.top"
    )
    foreach ($repository in $blockedRepositories) {
        if ($KeepCuriosApi -and $repository -eq "maven.theillusivec4.top") {
            continue
        }
        $escaped = [Regex]::Escape($repository)
        $content = [Regex]::Replace(
            $content,
            "(?m)^\s*maven\s*\{\s*url\s*['""]https://$escaped/?['""]\s*\}\s*\r?\n",
            "")
    }

    $content = [Regex]::Replace(
        $content,
        "(?m)^\s*(compileOnly|runtimeOnly)\s+fg\.deobf\(""mezz\.jei:[^""]+""\)\s*\r?\n",
        "")
    $content = [Regex]::Replace(
        $content,
        "(?m)^\s*(compileOnly|runtimeOnly)\s+fg\.deobf\(""vazkii\.patchouli:[^""]+""\)\s*\r?\n",
        "")

    if ($KeepCuriosApi) {
        $content = [Regex]::Replace(
            $content,
            "(?m)^\s*runtimeOnly\s+fg\.deobf\(""top\.theillusivec4\.curios:[^""]+""\)\s*\r?\n",
            "")
    } else {
        $content = [Regex]::Replace(
            $content,
            "(?m)^\s*(compileOnly|runtimeOnly)\s+fg\.deobf\(""top\.theillusivec4\.curios:[^""]+""\)\s*\r?\n",
            "")
    }

    $excludeLines = ($Excludes | ForEach-Object {
        "sourceSets.main.java.exclude `"$($_)`""
    }) -join "`r`n"
    $content = $content -replace
        'sourceSets\.main\.resources\.srcDirs \+= "src/main/generated"',
        "sourceSets.main.resources.srcDirs += `"src/main/generated`"`r`n$excludeLines"

    Set-Content -LiteralPath $Path -Value $content -Encoding utf8NoBOM
}

Update-BuildFile `
    -Path (Join-Path $Root "CoFHCore/build.gradle") `
    -Excludes @("cofh/core/compat/jei/**") `
    -KeepCuriosApi $true

Update-BuildFile `
    -Path (Join-Path $Root "Thermal/build.gradle") `
    -Excludes @(
        "cofh/thermal/core/compat/jei/**",
        "cofh/thermal/core/compat/patchouli/**",
        "cofh/thermal/lib/compat/jei/**"
    ) `
    -KeepCuriosApi $false

Update-BuildFile `
    -Path (Join-Path $Root "ThermalExpansion/build.gradle") `
    -Excludes @("cofh/thermal/expansion/compat/jei/**") `
    -KeepCuriosApi $false
