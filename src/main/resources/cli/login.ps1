# Hwan'Story device-flow sign-in for AI agents and scripts (Windows PowerShell 5.1+ / PowerShell 7). Never prints the token.
#
#   powershell -ExecutionPolicy Bypass -File login.ps1 start [-ClientName "..."] [-Scope "posts social"]
#   powershell -ExecutionPolicy Bypass -File login.ps1 wait      wait for the approval, save the token
#   powershell -ExecutionPolicy Bypass -File login.ps1 status    who the saved token belongs to
#   powershell -ExecutionPolicy Bypass -File login.ps1 logout    revoke the saved token and delete it
#
# Token file: $HOME\.config\hwanstory\auth-header ("Authorization: Bearer hsp_..."), the same file login.sh uses.
param(
  [ValidateSet('start', 'wait', 'status', 'logout')][string]$Command = 'start',
  [string]$ClientName = "AI agent on $env:COMPUTERNAME",
  [string]$Scope = 'posts social'
)
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
$Api = if ($env:HWANSTORY_API_URL) { $env:HWANSTORY_API_URL } else { '{{API_URL}}' }
$Dir = if ($env:HWANSTORY_CONFIG) { $env:HWANSTORY_CONFIG } else { Join-Path $HOME '.config/hwanstory' }
$Header = Join-Path $Dir 'auth-header'
$Pending = Join-Path $Dir 'device'
New-Item -ItemType Directory -Force $Dir | Out-Null

function Save-Private([string]$Path, [string]$Text) {
  [IO.File]::WriteAllText($Path, $Text, (New-Object Text.UTF8Encoding($false)))
  if ($IsLinux -or $IsMacOS) { chmod 600 $Path }
  else { try { icacls $Path /inheritance:r /grant:r "$($env:USERNAME):(R,W)" | Out-Null } catch { } }
}

function Get-Auth {
  if (-not (Test-Path $Header)) { throw 'not signed in' }
  return ((Get-Content $Header -Raw).Trim() -replace '^Authorization:\s*', '')
}

switch ($Command) {
  'start' {
    $r = Invoke-RestMethod -Method Post -Uri "$Api/api/v1/authentication/device/code" -Body @{ client_name = $ClientName; scope = $Scope }
    $deadline = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() + [int]$r.expires_in
    Save-Private $Pending ("{0}`n{1}`n" -f $r.device_code, $deadline)
    "Ask the user to open:  $($r.verification_uri_complete)"
    "and approve the code:  $($r.user_code)   (valid for $([int]($r.expires_in / 60)) minutes)"
    "Then run:              powershell -ExecutionPolicy Bypass -File `"$PSCommandPath`" wait"
  }
  'wait' {
    if (-not (Test-Path $Pending)) { throw "no pending sign-in - run the 'start' command first" }
    $lines = Get-Content $Pending
    $dc = $lines[0]; $deadline = [long]$lines[1]; $interval = 5
    while ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds() -lt $deadline) {
      $body = $null
      try {
        $r = Invoke-RestMethod -Method Post -Uri "$Api/api/v1/authentication/device/token" -Body @{ device_code = $dc }
        Save-Private $Header ("Authorization: Bearer {0}`n" -f $r.access_token)
        Remove-Item $Pending -Force
        "Signed in. Token $($r.access_token.Substring(0, 21))... saved to $Header"
        "  scope: $($r.scope), expires: $($r.expires_at)"
        return
      } catch {
        try { $body = $_.ErrorDetails.Message | ConvertFrom-Json } catch { $body = $null }
      }
      if ($body -and $body.error -eq 'slow_down') { $interval += 5 }
      elseif ($body -and $body.error -ne 'authorization_pending') {
        Remove-Item $Pending -Force
        throw "sign-in failed: $($body.error) - $($body.error_description)"
      }
      Start-Sleep -Seconds $interval
    }
    Remove-Item $Pending -Force -ErrorAction SilentlyContinue
    throw "the code expired before it was approved - run the 'start' command again"
  }
  'status' {
    Invoke-RestMethod -Uri "$Api/api/v1/tokens/current" -Headers @{ Authorization = (Get-Auth) } | ConvertTo-Json -Depth 5
  }
  'logout' {
    if (Test-Path $Header) {
      try { Invoke-RestMethod -Method Delete -Uri "$Api/api/v1/tokens/current" -Headers @{ Authorization = (Get-Auth) } | Out-Null } catch { }
      Remove-Item $Header -Force
    }
    'signed out'
  }
}
