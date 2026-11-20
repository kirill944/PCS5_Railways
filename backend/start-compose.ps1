$ErrorActionPreference = "Stop"
Push-Location $PSScriptRoot\..
try {
    if (-not (Test-Path ".env")) { throw "Файл .env не найден в корне проекта" }
    docker compose --env-file .env -f backend/docker-compose.yml up -d --wait
    Write-Host "Backend доступен: http://localhost:8080/" -ForegroundColor Green
    Write-Host "Swagger UI:       http://localhost:8080/swagger-ui.html" -ForegroundColor Green
    Write-Host "Health:           http://localhost:8080/actuator/health" -ForegroundColor Green
} finally {
    Pop-Location
}