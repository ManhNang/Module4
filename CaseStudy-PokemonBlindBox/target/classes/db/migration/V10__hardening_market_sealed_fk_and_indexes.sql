-- =====================================================================
-- V10: Hardening Foreign Keys & Composite Indexes cho Market & Auction Sealed Items
-- =====================================================================

-- 1. Dọn dẹp các dòng mồ côi (nếu có) trước khi gắn FK
UPDATE market_listings ml
LEFT JOIN user_sealed_items usi ON ml.sealed_item_id = usi.id
SET ml.sealed_item_id = NULL
WHERE ml.sealed_item_id IS NOT NULL AND usi.id IS NULL;

UPDATE auctions a
LEFT JOIN user_sealed_items usi ON a.sealed_item_id = usi.id
SET a.sealed_item_id = NULL
WHERE a.sealed_item_id IS NOT NULL AND usi.id IS NULL;

DROP PROCEDURE IF EXISTS sp_v10_hardening_market_sealed;
DELIMITER //
CREATE PROCEDURE sp_v10_hardening_market_sealed()
BEGIN
    -- Index cho market_listings(sealed_item_id)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_listings' AND INDEX_NAME = 'idx_market_listings_sealed_item'
    ) THEN
        CREATE INDEX idx_market_listings_sealed_item ON market_listings(sealed_item_id);
    END IF;

    -- Composite Index cho market_listings(status, item_type)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_listings' AND INDEX_NAME = 'idx_market_listings_status_type'
    ) THEN
        CREATE INDEX idx_market_listings_status_type ON market_listings(status, item_type);
    END IF;

    -- Index cho auctions(sealed_item_id)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auctions' AND INDEX_NAME = 'idx_auctions_sealed_item'
    ) THEN
        CREATE INDEX idx_auctions_sealed_item ON auctions(sealed_item_id);
    END IF;

    -- Composite Index cho auctions(status, item_type)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auctions' AND INDEX_NAME = 'idx_auctions_status_type'
    ) THEN
        CREATE INDEX idx_auctions_status_type ON auctions(status, item_type);
    END IF;

    -- FK market_listings.sealed_item_id -> user_sealed_items.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'market_listings' AND CONSTRAINT_NAME = 'fk_market_listing_sealed_item'
    ) THEN
        ALTER TABLE market_listings
            ADD CONSTRAINT fk_market_listing_sealed_item
            FOREIGN KEY (sealed_item_id) REFERENCES user_sealed_items(id);
    END IF;

    -- FK auctions.sealed_item_id -> user_sealed_items.id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auctions' AND CONSTRAINT_NAME = 'fk_auction_sealed_item'
    ) THEN
        ALTER TABLE auctions
            ADD CONSTRAINT fk_auction_sealed_item
            FOREIGN KEY (sealed_item_id) REFERENCES user_sealed_items(id);
    END IF;
END //
DELIMITER ;
CALL sp_v10_hardening_market_sealed();
DROP PROCEDURE IF EXISTS sp_v10_hardening_market_sealed;
