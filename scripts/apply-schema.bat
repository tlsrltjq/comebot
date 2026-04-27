@echo off
setlocal

set "CONTAINER_NAME=comebot-postgres"
set "POSTGRES_DB=comebot"
set "POSTGRES_USER=comebot"
set "SCHEMA_FILE=src\main\resources\schema.sql"

if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        if /i "%%A"=="POSTGRES_DB" set "POSTGRES_DB=%%B"
        if /i "%%A"=="POSTGRES_USER" set "POSTGRES_USER=%%B"
    )
)

if not exist "%SCHEMA_FILE%" (
    echo schema.sql not found: %SCHEMA_FILE%
    exit /b 1
)

docker inspect -f "{{.State.Running}}" "%CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
    echo PostgreSQL container is not running: %CONTAINER_NAME%
    echo Run: docker compose up -d postgres
    exit /b 1
)

echo Applying schema.sql to PostgreSQL container %CONTAINER_NAME%...
docker exec -i "%CONTAINER_NAME%" psql -v ON_ERROR_STOP=1 -U "%POSTGRES_USER%" -d "%POSTGRES_DB%" < "%SCHEMA_FILE%"
if errorlevel 1 (
    echo Failed to apply schema.sql.
    exit /b 1
)

echo schema.sql applied.
