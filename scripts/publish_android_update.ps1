param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [Parameter(Mandatory = $true)]
    [int]$VersionCode,

    [Parameter(Mandatory = $true)]
    [string]$VersionName,

    [Parameter(Mandatory = $true)]
    [string]$VpsTarget,

    [string]$SshKey = "$env:USERPROFILE\.ssh\id_ed25519",
    [string]$RemoteDir = "/root/services/caddy/data/site/azkary/releases",
    [string]$PublicBaseUrl = "https://apiexpenserabbit.hearo.support/azkary/releases",
    [string]$ApkFileName = "",
    [string]$LatestApkFileName = "Azkary-latest-selfHosted-release.apk",
    [string]$ManifestFileName = "update.json",
    [int]$MinSupportedVersionCode = 1,
    [bool]$Mandatory = $false,
    [string]$ReleaseNotes = ""
)

$ErrorActionPreference = "Stop"

function Get-ReleaseNotesFromChangelog {
    param(
        [string]$RequestedVersionName
    )

    $changelogPath = Join-Path $PSScriptRoot "..\CHANGELOG.md"
    if (-not (Test-Path -LiteralPath $changelogPath)) {
        return @()
    }

    $lines = Get-Content -LiteralPath $changelogPath
    $startIndex = -1
    $versionPattern = "^## \[$([regex]::Escape($RequestedVersionName))\]"

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $versionPattern) {
            $startIndex = $i + 1
            break
        }
    }

    if ($startIndex -lt 0) {
        return @()
    }

    $notes = @()
    for ($i = $startIndex; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match "^## \[") {
            break
        }
        if ($line -match "^\s*-\s+(.+)$") {
            $notes += $matches[1].Trim()
        }
    }

    return $notes
}

$resolvedApk = Resolve-Path -LiteralPath $ApkPath
$apkFileName = if ($ApkFileName) { $ApkFileName } else { "Azkary-v$VersionName-$VersionCode-selfHosted-release.apk" }
$shaFileName = "$apkFileName.sha256"
$latestShaFileName = "$LatestApkFileName.sha256"
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "azkary-update-release"
$stagedApk = Join-Path $tempDir $apkFileName
$stagedSha = Join-Path $tempDir $shaFileName
$stagedLatestSha = Join-Path $tempDir $latestShaFileName
$stagedManifest = Join-Path $tempDir $ManifestFileName

New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
Copy-Item -LiteralPath $resolvedApk -Destination $stagedApk -Force

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $stagedApk).Hash.ToLowerInvariant()
Set-Content -Path $stagedSha -Value "$hash  $apkFileName" -NoNewline
Set-Content -Path $stagedLatestSha -Value "$hash  $LatestApkFileName" -NoNewline

$releaseNoteList = @()
if (-not [string]::IsNullOrWhiteSpace($ReleaseNotes)) {
    $releaseNoteList = $ReleaseNotes.Split("|") |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
} else {
    $releaseNoteList = Get-ReleaseNotesFromChangelog -RequestedVersionName $VersionName
}

$apkUrl = "$PublicBaseUrl/$apkFileName"
$latestApkUrl = "$PublicBaseUrl/$LatestApkFileName"
$manifestUrl = "$PublicBaseUrl/$ManifestFileName"

$manifest = [ordered]@{
    enabled = $true
    latestVersionCode = $VersionCode
    latestVersionName = $VersionName
    minSupportedVersionCode = $MinSupportedVersionCode
    apkUrl = $apkUrl
    sha256 = $hash
    mandatory = $Mandatory
    releaseNotes = @($releaseNoteList)
}

$manifest | ConvertTo-Json -Depth 4 | Set-Content -Path $stagedManifest -Encoding utf8

Write-Host "Uploading $apkFileName to $($VpsTarget):$RemoteDir"
ssh -i $SshKey $VpsTarget "mkdir -p '$RemoteDir'"
scp -i $SshKey $stagedApk "${VpsTarget}:$RemoteDir/$apkFileName"
scp -i $SshKey $stagedSha "${VpsTarget}:$RemoteDir/$shaFileName"
ssh -i $SshKey $VpsTarget "cp '$RemoteDir/$apkFileName' '$RemoteDir/$LatestApkFileName'"
scp -i $SshKey $stagedLatestSha "${VpsTarget}:$RemoteDir/$latestShaFileName"
scp -i $SshKey $stagedManifest "${VpsTarget}:$RemoteDir/$ManifestFileName"

Write-Host ""
Write-Host "Upload complete."
Write-Host "Manifest:"
Write-Host $manifestUrl
Write-Host ""
Write-Host "Versioned APK:"
Write-Host $apkUrl
Write-Host ""
Write-Host "Latest alias:"
Write-Host $latestApkUrl
Write-Host ""
Write-Host "SHA-256:"
Write-Host $hash
