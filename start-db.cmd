@echo off
echo Starting Postgres database (service 'db') using Docker in WSL...
wsl docker compose up -d db
if %ERRORLEVEL% NEQ 0 (
    echo Failed to start database.
    exit /b %ERRORLEVEL%
)
echo Database started successfully.
