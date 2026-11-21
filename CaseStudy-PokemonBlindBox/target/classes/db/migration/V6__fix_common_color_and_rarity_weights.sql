-- ============================================================================
-- Flyway Migration: V6__fix_common_color_and_rarity_weights.sql
-- Nhiệm vụ:
--   1. Chuẩn hóa màu độ hiếm Common sang xám trung tính (#64748B).
--   2. Sửa lỗi ngược trọng số rơi giữa Rare và Illustration Rare (nếu có).
-- ============================================================================

UPDATE rarities
SET color_hex = '#64748B'
WHERE LOWER(TRIM(name)) = 'common';

DROP PROCEDURE IF EXISTS fix_inverted_rare_weights_v6;
DELIMITER //
CREATE PROCEDURE fix_inverted_rare_weights_v6()
BEGIN
    DECLARE v_rare_id BIGINT DEFAULT NULL;
    DECLARE v_rare_weight INT DEFAULT NULL;
    DECLARE v_ir_id BIGINT DEFAULT NULL;
    DECLARE v_ir_weight INT DEFAULT NULL;

    SELECT id, drop_weight INTO v_rare_id, v_rare_weight
    FROM rarities
    WHERE LOWER(TRIM(name)) = 'rare'
    LIMIT 1;

    SELECT id, drop_weight INTO v_ir_id, v_ir_weight
    FROM rarities
    WHERE LOWER(TRIM(name)) = 'illustration rare'
    LIMIT 1;

    IF v_rare_id IS NOT NULL AND v_ir_id IS NOT NULL AND v_rare_weight < v_ir_weight THEN
        UPDATE rarities SET drop_weight = v_ir_weight WHERE id = v_rare_id;
        UPDATE rarities SET drop_weight = v_rare_weight WHERE id = v_ir_id;
    END IF;
END //
DELIMITER ;

CALL fix_inverted_rare_weights_v6();
DROP PROCEDURE IF EXISTS fix_inverted_rare_weights_v6;
