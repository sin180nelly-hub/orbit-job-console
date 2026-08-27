package com.nelly.navigatornest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 種子資料使用固定 id 後，把 H2 identity 調到 MAX(id)+1，
 * 避免下一次 AI 存檔撞到既有主鍵，也避免寫死 RESTART WITH 10 蓋掉較大 id。
 */
@Component
@RequiredArgsConstructor
public class DataIdentityFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        bumpIdentity("users");
        bumpIdentity("tasks");
        // diaries 由 Hibernate ddl-auto 建立；表存在時再調整 identity
        try {
            bumpIdentity("diaries");
            // 舊日記補 diary_date（取 created_at 日期），方便日曆對應
            jdbcTemplate.update(
                    "UPDATE diaries SET diary_date = CAST(created_at AS DATE) WHERE diary_date IS NULL"
            );
        } catch (Exception ignored) {
            // 首次啟動若表尚未就緒可略過，下次啟動會再調
        }
    }

    private void bumpIdentity(String table) {
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM " + table,
                Long.class
        );
        long next = (maxId == null ? 0L : maxId) + 1L;
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + next
        );
    }
}
