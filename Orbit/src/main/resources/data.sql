-- 種子資料：使用 MERGE，不會 DELETE 既有任務（保留 AI 存入的 ID 10 等）
-- 編碼：UTF-8

-- 種子帳號密碼皆為 password123（BCrypt）
MERGE INTO users (id, username, email, password, role, created_at, updated_at) KEY (id) VALUES
(1, 'sinlin', 'sinyi1212@example.com', '$2a$10$PKR5WEkj/6DA9dflOsvqYuo7vdXQficMcrJsYD07VPviSadD2El6m', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO users (id, username, email, password, role, created_at, updated_at) KEY (id) VALUES
(2, 'leo1103', 'leo1103@example.com', '$2a$10$PKR5WEkj/6DA9dflOsvqYuo7vdXQficMcrJsYD07VPviSadD2El6m', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Orbit — AI Job Execution Console 預設開發帳號（密碼 nelly00）
MERGE INTO users (id, username, email, password, role, created_at, updated_at) KEY (id) VALUES
(3, 'nelly0', 'nelly0@example.com', '$2a$10$ZlMWuxsU/QqO4x6fPYFikOynXF97C9VzZ5x8OtF.Br3QYqdnUzt9m', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================
-- 林正弦（sinyi）的任務
-- =====================
MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(1, '準備下週二的專案簡報', '整理 Navigator Nest 專案的重點功能與進度', 'IN_PROGRESS', 'HIGH', DATE '2026-07-08', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(2, '晚上7點前去超商繳電費', '記得帶繳費單，超商繳比較方便', 'TODO', 'URGENT', DATE '2026-07-04', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(3, '買媽媽的生日禮物', '媽媽生日快到了，想送她一個按摩器', 'TODO', 'MEDIUM', DATE '2026-07-10', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(4, '完成客戶 API 文件修正', '把上週客戶反饋的修改點更新進去', 'IN_PROGRESS', 'HIGH', DATE '2026-07-07', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(5, '下班後去健身房運動', '這週已經連續三天沒去了，要保持習慣', 'TODO', 'LOW', DATE '2026-07-05', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================
-- 黎昂（leo）的任務
-- =====================
MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(6, '回覆主管關於需求變更的郵件', '把技術可行性與工時評估寫清楚', 'TODO', 'HIGH', DATE '2026-07-04', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(7, '研究 Spring Data Redis 的快取用法', '想把任務列表加上 Redis 快取，提升效能', 'IN_PROGRESS', 'MEDIUM', DATE '2026-07-09', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(8, '完成 LeetCode 每日一題', '連續打卡第 47 天，不要斷掉', 'DONE', 'LOW', DATE '2026-07-03', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO tasks (id, title, description, status, priority, due_date, user_id, created_at, updated_at) KEY (id) VALUES
(9, '整理本週的技術筆記', '把這週學到的 JWT 與 Security 問題整理成文件', 'TODO', 'MEDIUM', DATE '2026-07-06', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 注意：不要固定 RESTART WITH 10，否則若已有 ID 10 會在下次 INSERT 撞鍵。
-- identity 由 DataIdentityFixer 在啟動後依 MAX(id)+1 調整。

-- =====================
-- Orbit — AI Job Execution Console
-- 示範用 Jobs（20 筆，中性軟體開發/HPC 測試情境，歸屬 nelly0 / id=3）
-- 使用 MERGE 固定 ID（101~120），避免與動態建立的 Job 撞鍵；
-- DataIdentityFixer 會在啟動後把 identity 調到 MAX(id)+1。
-- 涵蓋狀態：SUCCESS / FAILED / RUNNING / PENDING
-- 涵蓋模型：llama3:8b / taide-b5-7b / code-llama:7b
-- =====================
MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(101, '會議記錄重點三點摘要',
 E'請將以下會議記錄重點整理為三點摘要，以繁體中文回覆：\n1. HPC 節點擴充計畫\n2. LLM 模型部署測試\n3. 系統改版上線時程',
 'llama3:8b', 'SUCCESS',
 E'1. 確認 HPC 節點擴充計畫，預計新增 8 個 GPU 節點。\n2. 預計下週進行 LLM 模型部署測試，先以 7B 量級驗證。\n3. 系統改版預計本月上線。',
 3421, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(102, '繁體中文情感分析測試',
 '判斷以下文本的情感傾向（正面/負面/中立）並說明理由：「這版介面反應速度比上一版快很多，操作也更直覺了。」',
 'taide-b5-7b', 'SUCCESS',
 '語意分析結果：正面 (Positive)，信心度：98.5%。\n理由：「快很多」「更直覺」為比較級正面評價詞彙，針對具體面向給予改善肯定。',
 2876, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(103, 'Java Spring Boot 重構建議',
 E'請優化以下 RestController 代碼並條列建議：\n@GetMapping("/jobs")\npublic List<Job> getJobs() { return jobRepository.findAll(); }',
 'code-llama:7b', 'SUCCESS',
 E'重構建議：\n1. 使用 ResponseEntity<List<JobResponse>> 封裝回應與 HTTP 狀態碼。\n2. 引入 @RestControllerAdvice + @ExceptionHandler 統一處理全域例外。\n3. 改透過 Service 層存取，避免 Controller 直接依賴 Repository。\n4. 加上分頁（Pageable）避免大量資料一次回傳。',
 4108, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(104, 'TAIDE 國家主權 AI 問答測試',
 '請以正體中文簡要說明：什麼是 TAIDE 模型？它與通用開源 LLM 的差異為何？（兩點內即可）',
 'taide-b5-7b', 'FAILED',
 'Execution failed: Failed to call Ollama at http://localhost:11344/api/generate. Is Ollama running? Connection refused: no further information',
 152, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(105, '超級電腦 GPU 節點調度排程測試',
 '分析目前 Slurm 排程佇列的工作負載分布，指出 GPU 資源使用率最高的時段與可能原因。',
 'llama3:8b', 'RUNNING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(106, '大型語言模型微調 (LoRA) 參數推薦',
 '針對 7B 模型在單張 A100 上進行 LoRA 微調，請推薦最佳的 Learning Rate、Rank (r) 與 Alpha 設定組合。',
 'llama3:8b', 'PENDING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(107, 'Python 自動化爬蟲模組分析',
 '檢查以下 BeautifulSoup 同步爬蟲程式的效能瓶頸，並提出改善方案。',
 'code-llama:7b', 'SUCCESS',
 '效能分析：主要瓶頸在同步逐頁請求的網路等待。建議改用 aiohttp 搭配 async/await 實現非同步併發，並加入連線池與重試機制，可將吞吐量提升約 5-10 倍。',
 1980, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(108, 'HPC 叢集 InfiniBand 網路連線診斷',
 '診斷 node-01 至 node-08 之間 InfiniBand 延遲異常問題，列出可能的檢查步驟。',
 'llama3:8b', 'FAILED',
 'Execution failed: Task timed out after 30000 ms waiting for node response.',
 30000, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(109, '繁中新聞標題自動生成',
 '以下是一篇關於國家算力平台啟用的新聞長文，請生成一個 15 字以內的繁體中文標題。',
 'taide-b5-7b', 'SUCCESS',
 '【國網焦點】自主 AI 算力平台正式啟用，賦能在地產業。',
 2410, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(110, 'Docker Container 資源限制最佳化',
 '請提供 docker-compose.yml 中限制容器記憶體上限 4G、CPU 2 核的設定範例。',
 'code-llama:7b', 'SUCCESS',
 E'services:\n  app:\n    deploy:\n      resources:\n        limits:\n          cpus: "2"\n          memory: 4G\n        reservations:\n          memory: 2G',
 3120, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(111, '跨節點分散式推論延遲測試',
 '模擬 4 張 A100 跨節點平行推論 70B 模型，記錄各節點間的通訊延遲與吞吐量數據。',
 'llama3:8b', 'RUNNING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(112, 'SQL 查詢語法效能優化 (Index 分析)',
 '以下查詢對 jobs 表全表掃描，請分析並建議適當的索引策略：SELECT * FROM jobs WHERE user_id = ? AND status = ? ORDER BY id DESC',
 'code-llama:7b', 'SUCCESS',
 '建議建立複合索引 (Composite Index)：CREATE INDEX idx_jobs_user_status ON jobs(user_id, status, id DESC)；可同時覆蓋過濾與排序條件，避免 filesort。',
 1850, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(113, '氣象海嘯模擬數據批次分析',
 '根據浮標觀測數據，預測未來 48 小時台灣東部海域波浪高度變化趨勢。',
 'llama3:8b', 'PENDING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(114, 'Ollama API 併發壓力測試',
 '同時向本地 Ollama 發送 50 個併發推理請求（模型 llama3:8b），觀察 VRAM 用量與回應延遲。',
 'llama3:8b', 'FAILED',
 'Execution failed: Out of Memory (OOM) - VRAM allocation limit reached.',
 410, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(115, '生醫基因序列比對算法說明',
 '請以淺顯方式簡述 BLAST 演算法的核心邏輯與用途。',
 'llama3:8b', 'SUCCESS',
 'BLAST 透過比對短序列片段（Words）快速定位相似區域，再進行區域擴展與評分，大幅加速序列比對，廣泛應用於基因功能註解與親緣關係分析。',
 5200, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(116, '自動化 Git Commit Message 生成',
 '根據以下 git diff 產生一則符合 conventional commits 規範的 commit message：新增 Job 批次刪除 API 與種子資料初始化。',
 'code-llama:7b', 'SUCCESS',
 'feat(job): add batch delete endpoint and seed data initialization',
 1620, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(117, '繁體中文醫療問答輔助測試',
 '請以正體中文回答：一般成人每日建議飲水量是多少？',
 'taide-b5-7b', 'SUCCESS',
 '一般成人每日建議飲水量約為體重（kg）乘以 30~35 mL；以 60 kg 者為例約 1,800~2,100 mL。運動或高溫環境應適度增加。',
 3890, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(118, '智慧城市 IoT 感測器數據異常檢測',
 '分析某測站 PM2.5 數值於兩小時內由 15 突升至 120 的可能原因，並建議即時警報門檻設定。',
 'llama3:8b', 'RUNNING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(119, '模型量化 GGUF Format 轉換說明',
 '請解釋 GGUF 量化格式中 Q4_K_M 與 Q8_0 的差異，以及各自的適用場景。',
 'llama3:8b', 'SUCCESS',
 'Q4_K_M 佔用顯示記憶體較小且精度損失極低，適合消費級 GPU 或 CPU 推理；Q8_0 幾乎接近 FP16 精度但模型體積較大，適合有充足 VRAM 且要求高品質輸出的場景。',
 2740, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO jobs (id, job_name, prompt, model_name, status, result, execution_time_ms, user_id, created_at, updated_at) KEY (id) VALUES
(120, '邊緣運算 (Edge AI) 資源調度策略',
 '評估在 Raspberry Pi 5 上部署 3B 參數量化模型的可行性，包含記憶體需求與推論速度預估。',
 'llama3:8b', 'PENDING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
