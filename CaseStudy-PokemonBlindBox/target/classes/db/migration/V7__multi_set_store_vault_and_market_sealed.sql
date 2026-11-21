-- ============================================================================
-- Flyway Migration: V7__multi_set_store_vault_and_market_sealed.sql
-- Nhiệm vụ:
--   1. Bổ sung hạn mức bán ngày (daily_pack_limit, daily_box_limit) vào card_sets
--      và chuẩn hóa mặc định 6 thẻ/pack, 20 pack/box, giá box = 20 * giá pack * 0.9 (-10%).
--   2. Tạo bảng set_daily_sales (hạn mức bán toàn cầu theo ngày, reset 00:00).
--   3. Tạo bảng user_sealed_items (kho chứa Pack lẻ & Box nguyên seal của người chơi).
--   4. Cho phép user_cards.booster_pack_id NULL để hỗ trợ mở Pack từ Kho đồ.
--   5. Mở rộng market_listings & auctions hỗ trợ 3 loại vật phẩm: CARD, PACK, BOX.
-- ============================================================================

DROP PROCEDURE IF EXISTS migrate_v7_store_vault_market;
DELIMITER //
CREATE PROCEDURE migrate_v7_store_vault_market()
BEGIN
    -- 1. card_sets: daily_pack_limit & daily_box_limit
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'card_sets' AND COLUMN_NAME = 'daily_pack_limit'
    ) THEN
        ALTER TABLE card_sets
            ADD COLUMN daily_pack_limit INT NOT NULL DEFAULT 200 AFTER box_price,
            ADD COLUMN daily_box_limit INT NOT NULL DEFAULT 15 AFTER daily_pack_limit;

        UPDATE card_sets
        SET cards_per_pack = 6,
            packs_per_box = 20,
            box_price = ROUND(pack_price * 20 * 0.9, 2);
    END IF;

    -- 2. user_cards: cho phép booster_pack_id NULL khi mở Pack từ Kho đồ
    ALTER TABLE user_cards MODIFY COLUMN booster_pack_id BIGINT NULL;

    -- 3. market_listings: item_type, sealed_item_id, user_card_id NULL
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_listings' AND COLUMN_NAME = 'item_type'
    ) THEN
        ALTER TABLE market_listings
            MODIFY COLUMN user_card_id BIGINT NULL,
            ADD COLUMN item_type VARCHAR(10) NOT NULL DEFAULT 'CARD' AFTER seller_id,
            ADD COLUMN sealed_item_id BIGINT NULL AFTER user_card_id;
    END IF;

    -- 4. auctions: item_type, sealed_item_id, user_card_id NULL
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auctions' AND COLUMN_NAME = 'item_type'
    ) THEN
        ALTER TABLE auctions
            MODIFY COLUMN user_card_id BIGINT NULL,
            ADD COLUMN item_type VARCHAR(10) NOT NULL DEFAULT 'CARD' AFTER seller_id,
            ADD COLUMN sealed_item_id BIGINT NULL AFTER user_card_id;
    END IF;
END //
DELIMITER ;

CALL migrate_v7_store_vault_market();
DROP PROCEDURE IF EXISTS migrate_v7_store_vault_market;

-- 5. Bảng theo dõi số lượng bán trong ngày của từng Bộ sưu tập (Daily Global Stock)
CREATE TABLE IF NOT EXISTS set_daily_sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_set_id BIGINT NOT NULL,
    sale_date DATE NOT NULL,
    packs_sold INT NOT NULL DEFAULT 0,
    boxes_sold INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_set_daily_sales_set_date UNIQUE (card_set_id, sale_date),
    CONSTRAINT fk_set_daily_sales_set FOREIGN KEY (card_set_id) REFERENCES card_sets (id) ON DELETE CASCADE,
    INDEX idx_set_daily_sales_date (sale_date)
);

-- 6. Bảng kho chứa Pack lẻ & Box nguyên seal của người chơi
CREATE TABLE IF NOT EXISTS user_sealed_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_set_id BIGINT NOT NULL,
    item_type VARCHAR(10) NOT NULL,
    cards_per_pack INT NOT NULL DEFAULT 6,
    packs_per_box INT NOT NULL DEFAULT 20,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_consumed BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_sealed_set FOREIGN KEY (card_set_id) REFERENCES card_sets (id) ON DELETE CASCADE,
    INDEX idx_user_sealed_owner (user_id, is_consumed, is_locked),
    INDEX idx_user_sealed_set_type (user_id, card_set_id, item_type, is_consumed)
);
