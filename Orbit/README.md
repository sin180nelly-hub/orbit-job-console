# Orbit Job Console

Spring Boot 本機 LLM Job 控制台（MVP）。
從既有 NavigatorNest（登入／任務／日記）加上 Job 模組：一次推論做成可查的狀態紀錄。

## 現況
- Job：PENDING → RUNNING → SUCCESS / FAILED
- 同步執行 Ollama（不是 Queue，RestTemplate 未設 timeout）
- Health Check：`GET /api/health`
- 資料：H2 + JPA `ddl-auto: update` + `data.sql`
- 尚無 CI/CD pipeline

## 怎麼跑
1. 安裝 Java 21、Ollama
2. 雙擊 `start-orbit.bat`，或進入 `Orbit/` 執行 `mvnw.cmd spring-boot:run`
3. 開啟 http://localhost:8080/index.html

## Demo
https://www.youtube.com/watch?v=jCsSvtyqJLk

原始碼主目錄在 `Orbit/`。
