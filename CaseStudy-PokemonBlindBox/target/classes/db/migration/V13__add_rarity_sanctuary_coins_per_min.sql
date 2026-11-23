-- =====================================================================
-- V13: Bổ sung cột sanctuary_coins_per_min vào bảng rarities để Đảo Pokémon
--      đọc tốc độ sinh Xu/phút động 100% từ Database (bỏ hardcode cũ).
-- =====================================================================

DROP PROCEDURE IF EXISTS sp_v13_add_rarity_sanctuary_coins;
DELIMITER //
CREATE PROCEDURE sp_v13_add_rarity_sanctuary_coins()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rarities' AND COLUMN_NAME = 'sanctuary_coins_per_min'
    ) THEN
        ALTER TABLE rarities ADD COLUMN sanctuary_coins_per_min INT NOT NULL DEFAULT 2 AFTER reveal_effect;
    END IF;
END //
DELIMITER ;
CALL sp_v13_add_rarity_sanctuary_coins();
DROP PROCEDURE IF EXISTS sp_v13_add_rarity_sanctuary_coins;

-- Seed tốc độ sinh Xu/phút tăng dần cho 9 độ hiếm hiện tại
UPDATE rarities SET sanctuary_coins_per_min = 1
WHERE (LOWER(name) LIKE '%common%' AND LOWER(name) NOT LIKE '%uncommon%') OR LOWER(name) = 'phổ biến';

UPDATE rarities SET sanctuary_coins_per_min = 2
WHERE LOWER(name) LIKE '%uncommon%' OR LOWER(name) LIKE '%không phổ biến%';

UPDATE rarities SET sanctuary_coins_per_min = 3
WHERE LOWER(name) = 'rare' OR LOWER(name) = 'hiếm';

UPDATE rarities SET sanctuary_coins_per_min = 5
WHERE LOWER(name) LIKE '%double%';

UPDATE rarities SET sanctuary_coins_per_min = 8
WHERE LOWER(name) LIKE '%illustration%' AND LOWER(name) NOT LIKE '%special%';

UPDATE rarities SET sanctuary_coins_per_min = 10
WHERE LOWER(name) LIKE '%ultra%';

UPDATE rarities SET sanctuary_coins_per_min = 15
WHERE LOWER(name) LIKE '%special illustration%';

UPDATE rarities SET sanctuary_coins_per_min = 20
WHERE LOWER(name) LIKE '%futuristic%';

UPDATE rarities SET sanctuary_coins_per_min = 30
WHERE LOWER(name) LIKE '%hyper%' OR LOWER(name) LIKE '%secret%' OR LOWER(name) LIKE '%gold%';
