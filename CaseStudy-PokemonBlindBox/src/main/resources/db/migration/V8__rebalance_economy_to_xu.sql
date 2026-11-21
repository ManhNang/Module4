-- ============================================================================
-- Flyway Migration: V8__rebalance_economy_to_xu.sql
-- Nhiệm vụ: Chuyển đổi đơn vị tiền tệ trong game sang Xu & cân bằng kinh tế chống lạm phát
--   - Giá Cửa hàng: 1 Pack = 450 Xu | 1 Box (20 Pack, -10%) = 8.100 Xu
--   - Giá Bán nhanh (Quick-sell): Common 5 Xu, Uncommon 12 Xu, Rare 30 Xu, EX/Illustration 120 Xu, Secret/Hyper 450 Xu
--   - Nhiệm vụ ngày: 25 - 40 Xu (Tổng 100 Xu/ngày), đổi tên "Mở 1 Pack bất kỳ"
--   - Quy đổi số dư Ví & giá Chợ cũ từ VNĐ sang Xu theo tỷ lệ 1.000 ₫ = 10 Xu (chia 100)
-- ============================================================================

-- 1. Cập nhật giá Pack (450 Xu) & Box (8.100 Xu) cho toàn bộ Bộ sưu tập
UPDATE card_sets
SET cards_per_pack = 6,
    packs_per_box = 20,
    pack_price = 450.00,
    box_price = 8100.00;

UPDATE booster_boxes
SET snapshot_box_price = 8100.00
WHERE snapshot_box_price > 20000.00;

UPDATE booster_packs
SET snapshot_pack_price = 450.00
WHERE snapshot_pack_price > 2000.00;

-- 2. Cân bằng giá Bán nhanh (Quick-sell) theo độ hiếm (15% - 30% giá Pack)
UPDATE rarities
SET buyback_price = CASE
    WHEN LOWER(name) REGEXP 'hyper|secret|gold|special illustration' THEN 450.00
    WHEN LOWER(name) REGEXP '(^|[^a-z])ex([^a-z]|$)|double rare|ultra rare|full art|illustration rare' THEN 120.00
    WHEN LOWER(name) REGEXP 'uncommon|không phổ biến' THEN 12.00
    WHEN LOWER(name) REGEXP 'rare|hiếm' OR is_holographic = TRUE THEN 30.00
    ELSE 5.00
END;

-- 3. Cập nhật Nhiệm vụ ngày hiện có sang mức thưởng Xu mới & đổi tên nhiệm vụ mở Pack
UPDATE daily_quests
SET title = 'Mở 1 Pack bất kỳ',
    reward_coins = 35.00
WHERE quest_code = 'OPEN_PACK_ONCE';

UPDATE daily_quests
SET reward_coins = 25.00
WHERE quest_code = 'HARVEST_ONCE' AND reward_coins > 100.00;

UPDATE daily_quests
SET reward_coins = 40.00
WHERE quest_code = 'EXPEDITION_ONCE' AND reward_coins > 100.00;

-- 4. Cập nhật các chuyến thám hiểm đang chạy (nếu còn lưu thưởng VNĐ cũ)
UPDATE expeditions
SET reward_coins = CASE zone_code
    WHEN 'GREEN_FOREST' THEN 20.00
    WHEN 'CRYSTAL_CAVE' THEN 65.00
    WHEN 'LEGEND_PEAK' THEN 160.00
    ELSE LEAST(reward_coins, 160.00)
END,
bonus_pack_chance = CASE zone_code
    WHEN 'GREEN_FOREST' THEN 5
    WHEN 'CRYSTAL_CAVE' THEN 12
    WHEN 'LEGEND_PEAK' THEN 25
    ELSE bonus_pack_chance
END
WHERE reward_coins > 500.00;

-- 5. Quy đổi hạn mức thu nhập Đảo hôm nay về trần 250 Xu
UPDATE sanctuary_daily_earnings
SET coins_earned = LEAST(ROUND(coins_earned / 100.0, 2), 250.00)
WHERE coins_earned > 250.00;

-- 6. Quy đổi số dư Ví & giá Chợ đang niêm yết từ VNĐ cũ sang Xu (1.000 ₫ = 10 Xu -> chia 100)
UPDATE wallets
SET balance = ROUND(balance / 100.0, 2),
    locked_balance = ROUND(locked_balance / 100.0, 2)
WHERE balance >= 50000.00 OR locked_balance >= 50000.00;

UPDATE market_listings
SET price = GREATEST(10.00, ROUND(price / 100.0, 2))
WHERE price >= 10000.00;

UPDATE auctions
SET starting_price = GREATEST(10.00, ROUND(starting_price / 100.0, 2)),
    min_bid_increment = GREATEST(5.00, ROUND(min_bid_increment / 100.0, 2)),
    current_highest_bid = GREATEST(10.00, ROUND(current_highest_bid / 100.0, 2))
WHERE starting_price >= 10000.00;
