-- ============================================================================
-- Flyway Migration: V5__sanctuary_and_buyback_schema.sql
-- Nhiệm vụ: Bổ sung giá thu mua NPC (buyback_price) cho bảng rarities và
--           các bảng cho Đảo Nuôi Dưỡng & Thám Hiểm (Poke-Sanctuary Idle 2.5D).
-- ============================================================================

DROP PROCEDURE IF EXISTS migrate_v5_sanctuary_and_buyback;
DELIMITER //
CREATE PROCEDURE migrate_v5_sanctuary_and_buyback()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'rarities'
          AND COLUMN_NAME = 'buyback_price'
    ) THEN
        ALTER TABLE rarities
            ADD COLUMN buyback_price DECIMAL(15, 2) NOT NULL DEFAULT 3000.00 AFTER is_holographic;

        UPDATE rarities SET buyback_price = CASE
            WHEN LOWER(name) REGEXP 'hyper|secret|gold|special illustration' THEN 100000.00
            WHEN LOWER(name) REGEXP '(^|[^a-z])ex([^a-z]|$)|double rare|ultra rare|full art|illustration rare' THEN 40000.00
            WHEN LOWER(name) REGEXP 'uncommon|không phổ biến' THEN 6000.00
            WHEN LOWER(name) REGEXP 'rare|hiếm' OR is_holographic = TRUE THEN 15000.00
            ELSE 3000.00
        END;
    END IF;
END //
DELIMITER ;

CALL migrate_v5_sanctuary_and_buyback();
DROP PROCEDURE IF EXISTS migrate_v5_sanctuary_and_buyback;

-- 1. Ô thả Pokémon trên Đảo (tối đa 5 thẻ / người chơi)
CREATE TABLE IF NOT EXISTS sanctuary_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_card_id BIGINT NOT NULL,
    last_harvest_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sanctuary_slot_card UNIQUE (user_card_id),
    CONSTRAINT fk_sanctuary_slot_user_card FOREIGN KEY (user_card_id) REFERENCES user_cards (id) ON DELETE CASCADE,
    INDEX idx_sanctuary_slots_user (user_id)
);

-- 2. Chuyến thám hiểm (Expeditions) — toàn bộ mốc thời gian tính trên Server
CREATE TABLE IF NOT EXISTS expeditions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    zone_code VARCHAR(30) NOT NULL,
    zone_name VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    reward_coins DECIMAL(15, 2) NOT NULL,
    bonus_pack_chance INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    reward_pack_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_expeditions_user_status (user_id, status)
);

-- 3. Danh sách thẻ bài tham gia từng chuyến thám hiểm
CREATE TABLE IF NOT EXISTS expedition_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expedition_id BIGINT NOT NULL,
    user_card_id BIGINT NOT NULL,
    CONSTRAINT fk_exp_card_expedition FOREIGN KEY (expedition_id) REFERENCES expeditions (id) ON DELETE CASCADE,
    CONSTRAINT fk_exp_card_user_card FOREIGN KEY (user_card_id) REFERENCES user_cards (id) ON DELETE CASCADE,
    INDEX idx_exp_cards_exp (expedition_id),
    INDEX idx_exp_cards_card (user_card_id)
);

-- 4. Nhiệm vụ ngày (Daily Quests)
CREATE TABLE IF NOT EXISTS daily_quests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quest_date DATE NOT NULL,
    quest_code VARCHAR(40) NOT NULL,
    title VARCHAR(150) NOT NULL,
    target_count INT NOT NULL DEFAULT 1,
    current_count INT NOT NULL DEFAULT 0,
    reward_coins DECIMAL(15, 2) NOT NULL,
    is_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_daily_quest_user_date_code UNIQUE (user_id, quest_date, quest_code),
    INDEX idx_daily_quests_user_date (user_id, quest_date)
);

-- 5. Giới hạn thu nhập Xu từ game mỗi ngày (Daily Cap chống lạm phát)
CREATE TABLE IF NOT EXISTS sanctuary_daily_earnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    earning_date DATE NOT NULL,
    coins_earned DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT uk_sanctuary_earnings_user_date UNIQUE (user_id, earning_date)
);
