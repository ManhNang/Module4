-- =====================================================================
-- V12: Bổ sung cột reveal_effect vào bảng rarities để đồng bộ động 100%
--      hiệu ứng mở thẻ từ Database (bỏ hoàn toàn hardcode tên độ hiếm).
-- =====================================================================

DROP PROCEDURE IF EXISTS sp_v12_add_rarity_reveal_effect;
DELIMITER //
CREATE PROCEDURE sp_v12_add_rarity_reveal_effect()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rarities' AND COLUMN_NAME = 'reveal_effect'
    ) THEN
        ALTER TABLE rarities ADD COLUMN reveal_effect VARCHAR(32) NOT NULL DEFAULT 'NONE' AFTER is_holographic;
    END IF;
END //
DELIMITER ;
CALL sp_v12_add_rarity_reveal_effect();
DROP PROCEDURE IF EXISTS sp_v12_add_rarity_reveal_effect;

-- Seed đồng bộ 9 độ hiếm hiện tại khớp đúng 7 preset (NONE -> MEGA_HYPER)
UPDATE rarities SET reveal_effect = 'NONE'
WHERE LOWER(name) LIKE '%common%' OR LOWER(name) = 'rare' OR LOWER(name) = 'hiếm' OR LOWER(name) LIKE '%phổ biến%';

UPDATE rarities SET reveal_effect = 'DOUBLE_RARE'
WHERE LOWER(name) LIKE '%double%';

UPDATE rarities SET reveal_effect = 'ILLUSTRATION'
WHERE LOWER(name) LIKE '%illustration%' AND LOWER(name) NOT LIKE '%special%';

UPDATE rarities SET reveal_effect = 'ULTRA'
WHERE LOWER(name) LIKE '%ultra%';

UPDATE rarities SET reveal_effect = 'SPECIAL_ILLUSTRATION'
WHERE LOWER(name) LIKE '%special illustration%';

UPDATE rarities SET reveal_effect = 'FUTURISTIC'
WHERE LOWER(name) LIKE '%futuristic%';

UPDATE rarities SET reveal_effect = 'MEGA_HYPER'
WHERE LOWER(name) LIKE '%hyper%' OR LOWER(name) LIKE '%secret%' OR LOWER(name) LIKE '%gold%';
