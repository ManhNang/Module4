-- =====================================================================
-- V14: Đồng bộ giá Bán nhanh (buyback_price / Quick Sell) tăng dần hợp lý
--      cho 9 bậc Độ hiếm trong bảng rarities.
-- =====================================================================

UPDATE rarities SET buyback_price = 5.00
WHERE (LOWER(name) LIKE '%common%' AND LOWER(name) NOT LIKE '%uncommon%') OR LOWER(name) = 'phổ biến';

UPDATE rarities SET buyback_price = 10.00
WHERE LOWER(name) LIKE '%uncommon%' OR LOWER(name) LIKE '%không phổ biến%';

UPDATE rarities SET buyback_price = 20.00
WHERE LOWER(name) = 'rare' OR LOWER(name) = 'hiếm';

UPDATE rarities SET buyback_price = 50.00
WHERE LOWER(name) LIKE '%double%';

UPDATE rarities SET buyback_price = 100.00
WHERE LOWER(name) LIKE '%illustration%' AND LOWER(name) NOT LIKE '%special%';

UPDATE rarities SET buyback_price = 200.00
WHERE LOWER(name) LIKE '%ultra%';

UPDATE rarities SET buyback_price = 400.00
WHERE LOWER(name) LIKE '%special illustration%';

UPDATE rarities SET buyback_price = 1000.00
WHERE LOWER(name) LIKE '%futuristic%';

UPDATE rarities SET buyback_price = 2000.00
WHERE LOWER(name) LIKE '%hyper%' OR LOWER(name) LIKE '%secret%' OR LOWER(name) LIKE '%gold%';
