$ErrorActionPreference = "Stop"
$Base = "http://localhost:8080"
$Passed = 0
$Failed = 0
$Failures = @()

function Assert($name, $cond, $detail) {
    if ($cond) {
        $script:Passed++
        Write-Host "[PASS] $name" -ForegroundColor Green
    } else {
        $script:Failed++
        $msg = "$name :: $detail"
        $script:Failures += $msg
        Write-Host "[FAIL] $msg" -ForegroundColor Red
    }
}

function New-Session {
    return New-Object Microsoft.PowerShell.Commands.WebRequestSession
}

function Login($session, $user, $pass) {
    $body = @{ username = $user; password = $pass } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$Base/api/auth/login" -Method POST -Body $body -ContentType "application/json" -WebSession $session
    return $r
}

function Get-Json($session, $url) {
    return Invoke-RestMethod -Uri $url -WebSession $session
}

function Post-Json($session, $url, $bodyObj) {
    $body = if ($null -eq $bodyObj) { "{}" } else { $bodyObj | ConvertTo-Json -Depth 10 }
    try {
        return Invoke-RestMethod -Uri $url -Method POST -Body $body -ContentType "application/json; charset=utf-8" -WebSession $session
    } catch {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        return $errBody | ConvertFrom-Json
    }
}

function Put-Json($session, $url, $bodyObj) {
    $body = $bodyObj | ConvertTo-Json -Depth 10
    try {
        return Invoke-RestMethod -Uri $url -Method PUT -Body $body -ContentType "application/json; charset=utf-8" -WebSession $session
    } catch {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        return $errBody | ConvertFrom-Json
    }
}

function Delete-Req($session, $url) {
    return Invoke-RestMethod -Uri $url -Method DELETE -WebSession $session
}

Write-Host "=== SQLDM v0.06 Self Test ===" -ForegroundColor Cyan

# Health
try {
    $h = Invoke-RestMethod -Uri "$Base/actuator/health"
    Assert "Actuator health" ($h.status -eq "UP") $h.status
} catch { Assert "Actuator health" $false $_.Exception.Message }

# Static
try {
    $idx = Invoke-WebRequest -Uri "$Base/index.html" -UseBasicParsing
    Assert "index.html" ($idx.StatusCode -eq 200) $idx.StatusCode
    Assert "index uses origin API" ($idx.Content -match 'window\.location\.origin') "missing origin"
} catch { Assert "index.html" $false $_.Exception.Message }

# Login super admin
$rootSess = New-Session
$lr = Login $rootSess "root" "root"
Assert "Login root" $lr.success "login failed"

$cur = Get-Json $rootSess "$Base/api/auth/current"
Assert "current user role" ($cur.data.role -eq "SUPER_ADMIN") $cur.data.role
Assert "current canAccessAdmin" ($cur.data.canAccessAdmin -eq $true) "false"
Assert "current canApprove" ($cur.data.canApprove -eq $true) "false"

# Topics
$topics = Get-Json $rootSess "$Base/api/topics"
Assert "list topics" $topics.success "failed"
$topicId = if ($topics.data.Count -gt 0) { $topics.data[0].id } else { $null }

if (-not $topicId) {
    $t = Post-Json $rootSess "$Base/api/topics" @{ topicName = "自测主题_$([guid]::NewGuid().ToString().Substring(0,8))"; description = "auto" }
    $topicId = $t.data.id
}

# Unified list query
$list = Get-Json $rootSess "$Base/api/metrics?sort=createdTime,desc"
Assert "metrics list" $list.success "failed"
Assert "metrics has total" ($null -ne $list.total) "no total"

$search = Get-Json $rootSess "$Base/api/metrics?keyword=test&status=ACTIVE&sort=metricName,asc"
Assert "unified search+filter" $search.success "failed"

if ($topicId) {
    $byTopic = Get-Json $rootSess "$Base/api/metrics?topicId=$topicId"
    Assert "filter by topicId" $byTopic.success "failed"
}

# Tags
$tags = Get-Json $rootSess "$Base/api/tags"
Assert "list tags" $tags.success "failed"

# Create ACTIVE metric as super admin (no approval)
$code = "TST" + (Get-Random -Maximum 999999)
$metricBody = @{
    metricName = "SelfTest_$code"
    metricCode = $code
    businessCaliber = "自测口径"
    topicId = $topicId
    owner = "root"
    status = "ACTIVE"
    dataSource = "test_dw"
    sqlTemplate = "SELECT 1 AS v"
    paramDefinition = $null
    allowedIps = $null
    tags = @("selftest")
}
$created = Post-Json $rootSess "$Base/api/metrics" $metricBody
Assert "create metric ACTIVE" $created.success $created.message
$metricId = $created.data.id
Assert "metric has tags" ($created.data.tags.Count -ge 1) "no tags"

# Get by id enrich
$one = Get-Json $rootSess "$Base/api/metrics/$metricId"
Assert "get metric by id" $one.success "failed"
Assert "favorited field" ($null -ne $one.data.favorited) "missing favorited"
Assert "hasPendingUpdate field" ($null -ne $one.data.hasPendingUpdate) "missing hasPendingUpdate"

# Favorite
$fav = Post-Json $rootSess "$Base/api/metrics/$metricId/favorite" $null
Assert "add favorite" $fav.success $fav.message
$one2 = Get-Json $rootSess "$Base/api/metrics/$metricId"
Assert "favorited true" ($one2.data.favorited -eq $true) "not favorited"

# Open API ACTIVE only
$open = Invoke-RestMethod -Uri "$Base/api/open/metrics?code=$code"
Assert "open api by code" ($open.success -eq $true) $open.message
Assert "open api returns sql" ($open.data.sqlTemplate -match "SELECT") "no sql"

try {
    $openDraft = Invoke-RestMethod -Uri "$Base/api/open/metrics?code=NONEXIST999"
    Assert "open api missing code fails" ($openDraft.success -eq $false) "unexpected success"
} catch {
    $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    $openDraft = ($reader.ReadToEnd()) | ConvertFrom-Json
    Assert "open api missing code fails" ($openDraft.success -eq $false) "unexpected"
}

# Access logs stats (super admin)
$stats = Get-Json $rootSess "$Base/api/access-logs/stats"
Assert "access log stats" $stats.success "failed"
Assert "stats total field" ($null -ne $stats.data.total) "no total"

# Approval info
$apInfo = Get-Json $rootSess "$Base/api/approvals/info"
Assert "approval info" $apInfo.success "failed"

$pending = Get-Json $rootSess "$Base/api/approvals/pending"
Assert "approval pending list" $pending.success "failed"

$subs = Get-Json $rootSess "$Base/api/approvals/my-submissions"
Assert "my submissions" $subs.success "failed"

# Login user1 for approval flow
$userSess = New-Session
$userLogin = Login $userSess "user1" "user1"
if (-not $userLogin.success) {
    Write-Host "user1 login failed, trying init..." -ForegroundColor Yellow
}

$userCur = Get-Json $userSess "$Base/api/auth/current"
if ($userCur.success) {
    Assert "user1 roleHint present" ($null -ne $userCur.data.roleHint) "missing"
    $userCode = "USR" + (Get-Random -Maximum 999999)
    $userMetric = @{
        metricName = "UserMetric_$userCode"
        metricCode = $userCode
        businessCaliber = "用户提交"
        topicId = $topicId
        owner = "user1"
        status = "ACTIVE"
        dataSource = "dw"
        sqlTemplate = "SELECT 2"
        tags = @()
    }
    $uc = Post-Json $userSess "$Base/api/metrics" $userMetric
    if ($uc.success) {
        Assert "user create pending approval" ($uc.data.status -eq "PENDING_APPROVAL") $uc.data.status
        $pendingId = $uc.data.id
        $userSubs = Get-Json $userSess "$Base/api/approvals/my-submissions"
        Assert "user sees submission" ($userSubs.data.Count -ge 1) "empty"
        # Approve as root
        $ap = Post-Json $rootSess "$Base/api/approvals/create/$pendingId/approve" @{ comment = "ok" }
        Assert "approve create" $ap.success $ap.message
        $after = Get-Json $rootSess "$Base/api/metrics/$pendingId"
        Assert "approved ACTIVE" ($after.data.status -eq "ACTIVE") $after.data.status
        # Update as user -> pending update
        $userMetric2 = @{
            metricName = $userMetric.metricName
            metricCode = $userCode
            businessCaliber = "user update caliber"
            topicId = $topicId
            owner = "user1"
            status = "ACTIVE"
            dataSource = "dw"
            sqlTemplate = "SELECT 2"
            tags = @()
        }
        $upd = Put-Json $userSess "$Base/api/metrics/$pendingId" $userMetric2
        if ($upd.success) {
            Assert "user update pending" ($upd.pendingApproval -eq $true) "not pending"
            $pendList = Get-Json $rootSess "$Base/api/approvals/pending"
            $updateReq = $pendList.data | Where-Object { $_.requestType -eq "UPDATE" -and $_.metricId -eq $pendingId } | Select-Object -First 1
            if ($updateReq) {
                Assert "pending has changedFields" ($null -ne $updateReq.changedFields) "missing diff"
                Assert "pending has preview" ($null -ne $updateReq.preview) "missing preview"
                Assert "pending has current" ($null -ne $updateReq.current) "missing current"
                $apu = Post-Json $rootSess "$Base/api/approvals/update/$($updateReq.requestId)/approve" @{ comment = "ok" }
                Assert "approve update" $apu.success $apu.message
            } else {
                Assert "find update pending" $false "not found - user may be approver on topic"
            }
        } else {
            Assert "user update" $false $upd.message
        }
    } else {
        Assert "user create metric" $false $uc.message
    }
}

# Duplicate name per topic
$dupBody = @{
    metricName = $metricBody.metricName
    metricCode = "DUP$code"
    businessCaliber = "x"
    topicId = $topicId
    owner = "root"
    status = "ACTIVE"
    dataSource = "dw"
    sqlTemplate = "SELECT 1"
    tags = @()
}
$dup = Post-Json $rootSess "$Base/api/metrics" $dupBody
Assert "reject duplicate name in topic" (-not $dup.success) ($dup.message)

# Delete permission: user1 cannot delete
if ($metricId) {
    try {
        $delUser = Delete-Req $userSess "$Base/api/metrics/$metricId"
        Assert "user cannot delete" (-not $delUser.success) "should fail"
    } catch { Assert "user cannot delete" $true "403/400 expected" }
    $delRoot = Delete-Req $rootSess "$Base/api/metrics/$metricId"
    Assert "admin can delete" $delRoot.success $delRoot.message
}

# Versions / audit
if ($pendingId) {
    $vers = Get-Json $rootSess "$Base/api/metrics/$pendingId/versions"
    Assert "versions list" $vers.success "failed"
    $aud = Get-Json $rootSess "$Base/api/metrics/$pendingId/audit-logs"
    Assert "audit logs" $aud.success "failed"
}

# Reject + resubmit flow
$rejCode = "REJ" + (Get-Random -Maximum 999999)
$rejMetric = @{
    metricName = "RejectTest_$rejCode"
    metricCode = $rejCode
    businessCaliber = "reject test"
    topicId = $topicId
    owner = "user1"
    status = "ACTIVE"
    dataSource = "dw"
    sqlTemplate = "SELECT 3"
    tags = @()
}
$rc = Post-Json $userSess "$Base/api/metrics" $rejMetric
if ($rc.success) {
    $rejId = $rc.data.id
    $rj = Post-Json $rootSess "$Base/api/approvals/create/$rejId/reject" @{ comment = "need fix" }
    Assert "reject create" $rj.success $rj.message
    $rejected = Get-Json $rootSess "$Base/api/metrics/$rejId"
    Assert "status REJECTED" ($rejected.data.status -eq "REJECTED") $rejected.data.status
    $resubmit = @{
        metricName = $rejMetric.metricName
        metricCode = $rejCode
        businessCaliber = "fixed caliber"
        topicId = $topicId
        owner = "user1"
        status = "ACTIVE"
        dataSource = "dw"
        sqlTemplate = "SELECT 3"
        tags = @()
    }
    $rs = Put-Json $userSess "$Base/api/metrics/$rejId" $resubmit
    Assert "resubmit after reject" ($rs.success -and $rs.data.status -eq "PENDING_APPROVAL") ($rs.message)
    Post-Json $rootSess "$Base/api/approvals/create/$rejId/approve" @{ comment = "ok" } | Out-Null
}

# check-duplicates
$dupWarn = Post-Json $rootSess "$Base/api/metrics/check-duplicates" @{
    metricName = "SimilarName"; metricCode = "SIM123"; topicId = $topicId; owner = "x"; status = "DRAFT"; businessCaliber = "x"
}
Assert "check duplicates" $dupWarn.success "failed"

# engagement endpoints
$favList = Get-Json $rootSess "$Base/api/metrics/favorites"
Assert "favorites list" $favList.success "failed"
$recent = Get-Json $rootSess "$Base/api/metrics/recent"
Assert "recent list" $recent.success "failed"
$mine = Get-Json $rootSess "$Base/api/metrics/mine"
Assert "mine list" $mine.success "failed"

# tag filter
$tagList = Get-Json $rootSess "$Base/api/metrics?tag=selftest"
Assert "filter by tag" $tagList.success "failed"

# admin1 TOPIC_ADMIN without topic assignment cannot approve
$adminSess = New-Session
Login $adminSess "admin1" "admin1" | Out-Null
$adminCur = Get-Json $adminSess "$Base/api/auth/current"
Assert "admin1 not topic approver by default" ($adminCur.data.canApprove -eq $false) "should be false"
Assert "admin1 cannot delete" ($adminCur.data.canDeleteMetric -eq $false) "should be false"

# static pages
$admin = Invoke-WebRequest -Uri "$Base/admin.html" -UseBasicParsing
Assert "admin.html" ($admin.StatusCode -eq 200) $admin.StatusCode
Assert "admin uses origin" ($admin.Content -match 'window\.location\.origin') "missing"
$login = Invoke-WebRequest -Uri "$Base/login.html" -UseBasicParsing
Assert "login.html" ($login.StatusCode -eq 200) $login.StatusCode

# swagger
$sw = Invoke-WebRequest -Uri "$Base/swagger-ui.html" -UseBasicParsing
Assert "swagger ui" ($sw.StatusCode -eq 200) $sw.StatusCode

# access logs list
$logs = Get-Json $rootSess "$Base/api/access-logs"
Assert "access logs list" $logs.success "failed"

# users list (super admin)
$users = Get-Json $rootSess "$Base/api/users"
Assert "users list" $users.success "failed"

# webhooks list
$wh = Get-Json $rootSess "$Base/api/webhooks"
Assert "webhooks list" $wh.success "failed"

# api clients list
$clients = Get-Json $rootSess "$Base/api/api-clients"
Assert "api clients list" $clients.success "failed"

# logout
$lo = Post-Json $rootSess "$Base/api/auth/logout" $null
Assert "logout" $lo.success "failed"

Write-Host ""
Write-Host "=== Results: $Passed passed, $Failed failed ===" -ForegroundColor Cyan
if ($Failures.Count -gt 0) {
    $Failures | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
exit 0
