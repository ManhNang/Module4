-- =====================================================================
-- V11: Bổ sung ảnh PNG tùy chỉnh cho Pack và Box trên từng Bộ Thẻ (card_sets)
-- =====================================================================

DROP PROCEDURE IF EXISTS sp_v11_add_card_set_pack_box_images;
DELIMITER //
CREATE PROCEDURE sp_v11_add_card_set_pack_box_images()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'card_sets' AND COLUMN_NAME = 'pack_image_url'
    ) THEN
        ALTER TABLE card_sets ADD COLUMN pack_image_url VARCHAR(500) NULL AFTER is_active;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'card_sets' AND COLUMN_NAME = 'box_image_url'
    ) THEN
        ALTER TABLE card_sets ADD COLUMN box_image_url VARCHAR(500) NULL AFTER pack_image_url;
    END IF;
END //
DELIMITER ;
CALL sp_v11_add_card_set_pack_box_images();
DROP PROCEDURE IF EXISTS sp_v11_add_card_set_pack_box_images;
