$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
try {
    Invoke-WebRequest -Uri 'http://localhost:8080/login' -Method Post -Body @{username='admin'; password='admin123'} -WebSession $session -UseBasicParsing -ErrorAction Stop
    Write-Host 'LOGIN: ok'
} catch {
    Write-Host 'LOGIN: failed'
    Write-Host $_.Exception.Message
    exit 2
}

try {
    Write-Host '=== BATCHES ==='
    (Invoke-RestMethod -Uri 'http://localhost:8080/admin/api/batches' -WebSession $session) | ConvertTo-Json -Depth 8
} catch {
    Write-Host 'Batches fetch failed:'
    Write-Host $_.Exception.Message
}

try {
    Write-Host '=== TEACHERS ==='
    (Invoke-RestMethod -Uri 'http://localhost:8080/admin/api/teachers' -WebSession $session) | ConvertTo-Json -Depth 8
} catch {
    Write-Host 'Teachers fetch failed:'
    Write-Host $_.Exception.Message
}

try {
    Write-Host '=== STUDENTS ==='
    (Invoke-RestMethod -Uri 'http://localhost:8080/admin/api/students' -WebSession $session) | ConvertTo-Json -Depth 8
} catch {
    Write-Host 'Students fetch failed:'
    Write-Host $_.Exception.Message
}
