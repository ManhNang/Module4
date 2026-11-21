-- =====================================================================
-- V9: Phân tách màu sắc rõ ràng cho các bậc Độ hiếm (tránh trùng màu)
-- Common: Slate | Uncommon: Emerald | Rare: Sky Blue | Double Rare: Indigo
-- Ultra Rare: Orange | Illustration Rare: Purple | Special Illustration: Pink
-- Mega Hyper Rare / Hyper Rare / Secret: Gold/Amber
-- =====================================================================

UPDATE rarities SET color_hex = '#94A3B8' WHERE LOWER(name) LIKE '%common%' AND LOWER(name) NOT LIKE '%uncommon%';
UPDATE rarities SET color_hex = '#34D399' WHERE LOWER(name) LIKE '%uncommon%';
UPDATE rarities SET color_hex = '#38BDF8' WHERE LOWER(name) = 'rare' OR LOWER(name) = 'hiếm';
UPDATE rarities SET color_hex = '#818CF8' WHERE LOWER(name) LIKE '%double%';
UPDATE rarities SET color_hex = '#FB923C' WHERE LOWER(name) LIKE '%ultra%';
UPDATE rarities SET color_hex = '#C084FC' WHERE LOWER(name) LIKE '%illustration%' AND LOWER(name) NOT LIKE '%special%';
UPDATE rarities SET color_hex = '#EC4899' WHERE LOWER(name) LIKE '%special illustration%';
UPDATE rarities SET color_hex = '#F59E0B' WHERE LOWER(name) LIKE '%hyper%' OR LOWER(name) LIKE '%secret%' OR LOWER(name) LIKE '%gold%';
