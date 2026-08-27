@echo off
REM ═══════════════════════════════════════════════════════════
REM  Orbit - AI Job Execution Console : 自動化啟動腳本 v2（防閃退版）
REM ═══════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

REM 編碼設 UTF-8（必須在任何含中文的輸出之前）
chcp 950 >nul
title Orbit - AI Job Execution Console

echo.
echo ============================================
echo   Orbit - AI Job Execution Console 啟動中...
echo ============================================
echo.

REM ── 步驟 1：檢查並關閉佔用 Port 8080 的舊程序 ──
echo [步驟 1] 檢查 Port 8080 是否被佔用...
set "FOUND_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    set "FOUND_PID=%%P"
)

if not "!FOUND_PID!"=="" (
    echo   偵測到舊程序 PID: !FOUND_PID! ，正在關閉...
    taskkill /F /PID !FOUND_PID! >nul 2>&1
    timeout /t 2 /nobreak >nul
    echo   [OK] Port 8080 已釋放。
) else (
    echo   [OK] Port 8080 目前空閒。
)
echo.

REM ── 步驟 2：檢查本地 Ollama API 是否運行 ──
echo [步驟 2] 檢查本地 Ollama AI 引擎 ...
curl -s -o nul --max-time 3 http://127.0.0.1:11434/api/tags >nul 2>&1
if not errorlevel 1 (
    echo   [OK] Ollama 本地 AI 引擎運作中。模型: llama3:8b
) else (
    echo   [警告] 未偵測到 Ollama，嘗試自動在背景啟動 ollama serve...
    where ollama >nul 2>&1
    if not errorlevel 1 (
        start "Ollama Server" /min cmd /c "ollama serve"
        set /a TRIES=0
        :wait_ollama
        timeout /t 2 /nobreak >nul
        curl -s -o nul --max-time 3 http://127.0.0.1:11434/api/tags >nul 2>&1
        if errorlevel 1 (
            set /a TRIES+=1
            if !TRIES! LSS 5 goto wait_ollama
            echo   [警告] Ollama 自動啟動逾時，請手動執行 ollama serve 後重試。
        ) else (
            echo   [OK] Ollama 已於背景成功啟動。
        )
    ) else (
        echo   [錯誤] 找不到 ollama 指令，請先安裝 Ollama 或確認其在 PATH 中。
    )
)
echo.

REM ── 步驟 3：進入專案目錄（使用 %~dp0 相對路徑，避免長檔名/破折號解析問題）──
echo [步驟 3] 進入專案目錄...
cd /d "%~dp0Orbit"
if errorlevel 1 (
    echo   [錯誤] 無法進入專案目錄：%~dp0Orbit
    echo   請確認此 bat 檔與 Orbit 資料夾位於同一層！
    pause
    exit /b 1
)
if not exist "pom.xml" (
    echo   [錯誤] 找不到 pom.xml，請確認專案路徑！目前目錄：!CD!
    pause
    exit /b 1
)
echo   專案目錄：!CD!
echo.

REM ── 步驟 4：排定背景延遲開啟瀏覽器（等 Spring Boot 啟動）──
echo [步驟 4] 將在背景延遲開啟 http://localhost:8080/index.html ...
start "" /min cmd /c "timeout /t 15 /nobreak >nul & start "" http://localhost:8080/index.html"
echo   已排定，瀏覽器稍後會自動開啟。
echo.

REM ── 步驟 5：啟動 Spring Boot 後端 ──
echo [步驟 5] 啟動 Spring Boot 後端 ...
echo --------------------------------------------
echo   按 Ctrl+C 可停止服務
echo --------------------------------------------
echo.

set "MVN_CMD=%LOCALAPPDATA%\maven\apache-maven-3.9.16\bin\mvn.cmd"

if exist "!MVN_CMD!" (
    echo   使用本機 Maven：!MVN_CMD!
    call "!MVN_CMD!" spring-boot:run
) else if exist "mvnw.cmd" (
    echo   使用專案內 Maven Wrapper：mvnw.cmd
    call mvnw.cmd spring-boot:run
) else (
    where mvn >nul 2>&1
    if not errorlevel 1 (
        echo   使用 PATH 上的 mvn
        call mvn spring-boot:run
    ) else (
        echo.
        echo   [錯誤] 找不到任何可用的 Maven！
        echo   請安裝 Maven，或將其加入 PATH 後重試。
        echo.
        pause
        exit /b 1
    )
)

echo.
echo ============================================
echo   後端已停止。感謝使用 Orbit Console！
echo ============================================
REM 最末端 pause：就算後端出錯停止，視窗也會留著顯示錯誤 log
pause
