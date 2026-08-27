# Navigator Nest

Spring Boot 個人任務與日記系統，整合本地 LLM（Ollama / LM Studio）。

- JWT 登入與使用者資料隔離
- 任務 CRUD、優先級／狀態／截止日期與可選時間
- AI：自然語言 → 任務建議 + 日記 + 歌詞，可一鍵存檔
- 獨立日記（可關聯任務）、月曆與「過去／現在／未來」時間軸
- 即將到期倒數（24h 內即時時分秒）

**Stack:** Java 21 · Spring Boot · JPA · Security/JWT · H2 · RestClient · 靜態前端
